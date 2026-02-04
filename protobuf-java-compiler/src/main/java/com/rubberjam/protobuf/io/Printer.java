package com.rubberjam.protobuf.io;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class Printer
{

	// --- Core Enums and Interfaces ---
	public enum Semantic
	{
		NONE(0),
		SET(1),
		ALIAS(2);

		private final int value;

		Semantic(int v)
		{
			this.value = v;
		}

		public int getValue()
		{
			return value;
		}
	}

	public interface AnnotationCollector
	{
		void addAnnotation(int begin, int end, String file, List<Integer> path, Semantic semantic);
	}

	// --- Internal Logic Structures ---
	private static class Chunk
	{
		final String text;
		final boolean isVar;

		Chunk(String text, boolean isVar)
		{
			this.text = text;
			this.isVar = isVar;
		}
	}

	private static class Line
	{
		final List<Chunk> chunks = new ArrayList<>();
		int indent;

		boolean isPureMarker()
		{
			if (chunks.isEmpty()) return false;
			for (Chunk c : chunks)
			{
				// A line is "pure marker" if it only contains _start or _end
				// variables
				// and no literal text or other variables.
				if (!c.isVar) return false;
				if (!c.text.startsWith("_start") && !c.text.startsWith("_end")) return false;
			}
			return true;
		}
	}

	private static class Format
	{
		final List<Line> lines = new ArrayList<>();
		boolean isRawString = false;
	}

	public static class PrinterValue
	{
		String text;
		BooleanSupplier callback;
		String consumeAfter;

		public PrinterValue(String text)
		{
			this.text = text;
			this.consumeAfter = null; // Strings don't consume punctuation by
										// default
		}

		public PrinterValue(BooleanSupplier callback)
		{
			this.callback = callback;
			this.consumeAfter = ";,"; // Callbacks (blocks) usually consume
										// punctuation
		}
	}

	public static class AnnotationRecord
	{
		String filePath;
		List<Integer> path;
		Semantic semantic;

		public AnnotationRecord(String f, List<Integer> p, Semantic s)
		{
			this.filePath = f;
			this.path = p;
			this.semantic = s;
		}
	}

	public static class Options
	{
		public char variableDelimiter = '$';
		public int spacesPerIndent = 2;
		public String commentStart = "//";
		public String ignoredCommentStart = "//~";
		public boolean stripRawStringIndentation = true;
		public AnnotationCollector annotationCollector = null;
	}

	// Helper class for annotation record tracking
	private static class AnnotationRecordEntry
	{
		String varName;
		int position;

		AnnotationRecordEntry(String varName, int position)
		{
			this.varName = varName;
			this.position = position;
		}
	}

	// --- State Management ---
	private final Options options;
	private final StringBuilder buffer = new StringBuilder();
	private final Deque<Map<String, PrinterValue>> varStack = new ArrayDeque<>();
	private final Deque<Map<String, AnnotationRecord>> annotationStack = new ArrayDeque<>();
	private final Map<String, int[]> substitutions = new HashMap<>();

	private int currentIndent = 0;
	private int bytesWritten = 0;
	private int lastNewlineBytes = 0;
	private boolean atStartOfLine = true;
	private boolean pendingIndent = true;
	private boolean skipNextNewline = false;

	public Printer(Options options)
	{
		this.options = options;
		varStack.push(new HashMap<>());
		annotationStack.push(new HashMap<>());
	}

	// --- Scoping API ---

	public AutoCloseable withVars(Map<String, Object> vars)
	{
		Map<String, PrinterValue> varFrame = new HashMap<>();
		Map<String, AnnotationRecord> annotFrame = new HashMap<>();

		vars.forEach((k, v) ->
		{
			if (v instanceof AnnotationRecord)
			{
				annotFrame.put(k, (AnnotationRecord) v);
			}
			else if (v instanceof PrinterValue)
			{
				varFrame.put(k, (PrinterValue) v);
			}
			else if (v instanceof BooleanSupplier)
			{
				varFrame.put(k, new PrinterValue((BooleanSupplier) v));
			}
			else
			{
				varFrame.put(k, new PrinterValue(String.valueOf(v)));
			}
		});

		varStack.push(varFrame);
		annotationStack.push(annotFrame);
		return () ->
		{
			varStack.pop();
			annotationStack.pop();
		};
	}

	public void pushVars(Map<String, Object> vars)
	{
		Map<String, PrinterValue> varFrame = new HashMap<>();
		Map<String, AnnotationRecord> annotFrame = new HashMap<>();

		vars.forEach((k, v) ->
		{
			if (v instanceof AnnotationRecord)
			{
				annotFrame.put(k, (AnnotationRecord) v);
			}
			else if (v instanceof PrinterValue)
			{
				varFrame.put(k, (PrinterValue) v);
			}
			else if (v instanceof BooleanSupplier)
			{
				varFrame.put(k, new PrinterValue((BooleanSupplier) v));
			}
			else
			{
				varFrame.put(k, new PrinterValue(String.valueOf(v)));
			}
		});

		varStack.push(varFrame);
		annotationStack.push(annotFrame);
	}

	public AutoCloseable withIndent()
	{
		indent();
		return this::outdent;
	}

	public void indent()
	{
		if (!atStartOfLine)
		{
			int lineLength = bytesWritten - lastNewlineBytes;
			if (lineLength > 0 && lineLength == currentIndent)
			{
				boolean allSpaces = true;
				for (int i = 0; i < lineLength; i++)
				{
					if (buffer.charAt(buffer.length() - 1 - i) != ' ')
					{
						allSpaces = false;
						break;
					}
				}

				if (allSpaces)
				{
					String extension = " ".repeat(options.spacesPerIndent);
					buffer.append(extension);
					bytesWritten += options.spacesPerIndent;
				}
			}
		}
		currentIndent += options.spacesPerIndent;
	}

	public void outdent()
	{
		currentIndent -= options.spacesPerIndent;
		if (!atStartOfLine)
		{
			int lineLength = bytesWritten - lastNewlineBytes;
			if (lineLength > 0 && lineLength > currentIndent)
			{
				// Check if the line contains only spaces
				boolean allSpaces = true;
				for (int i = 0; i < lineLength; i++)
				{
					if (buffer.charAt(buffer.length() - 1 - i) != ' ')
					{
						allSpaces = false;
						break;
					}
				}

				if (allSpaces)
				{
					int toRemove = lineLength - currentIndent;
					buffer.setLength(buffer.length() - toRemove);
					bytesWritten -= toRemove;
					if (currentIndent == 0)
					{
						atStartOfLine = true;
					}
				}
			}
		}
	}

	// --- Emission API ---

	public void emit(String formatStr)
	{
		emit(Collections.emptyMap(), formatStr);
	}

	public void print(String formatStr)
	{
		emit(Collections.emptyMap(), formatStr);
	}

	public void print(Map<String, Object> vars, String formatStr)
	{
		emit(vars, formatStr);
	}

	public void annotate(String startVar, String endVar, com.google.protobuf.Descriptors.FieldDescriptor descriptor) {
		// Stub implementation for now to satisfy compilation.
	}

	public void emit(Map<String, Object> vars, String formatStr)
	{
		try (AutoCloseable scope = withVars(vars))
		{
			Format fmt = tokenizeFormat(formatStr);
			int baseIndent = currentIndent;
			Deque<AnnotationRecordEntry> annotRecords = new ArrayDeque<>();

			// Raw strings start with a newline in the template. If we aren't
			// currently at the start of a line, we need to emit that newline.
			// However, if we only wrote indentation so far, we are effectively
			// at the start of the line, so we don't need a newline.
			boolean isIndentedOnly = (bytesWritten - lastNewlineBytes) == baseIndent;
			if (fmt.isRawString && !atStartOfLine && !isIndentedOnly)
			{
				writeRaw("\n");
				pendingIndent = true;
			}

			boolean previousLineWasPure = false;
			for (int i = 0; i < fmt.lines.size(); i++)
			{
				Line line = fmt.lines.get(i);
				boolean isPure = isPureMarkers(line);
				// Emit newline for every line after the first.
				if (i > 0)
				{
					if (!previousLineWasPure)
					{
						if (!skipNextNewline)
						{
							writeRaw("\n");
							pendingIndent = true;
						}
					}
					skipNextNewline = false;
				}

				currentIndent = baseIndent + line.indent;

				for (int chunkIdx = 0; chunkIdx < line.chunks.size(); chunkIdx++)
				{
					Chunk chunk = line.chunks.get(chunkIdx);

					if (pendingIndent)
					{
						writeIndent();
						pendingIndent = false;
					}

					if (!chunk.isVar)
					{
						writeRaw(chunk.text);
					}
					else
					{
						chunkIdx = handleVariableWithAnnotations(chunk, annotRecords, line, chunkIdx);
					}
				}
				previousLineWasPure = isPure;
			}
			currentIndent = baseIndent;
		}
		catch (NoSuchElementException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Emits text directly to output without indentation or variable
	 * substitution. Corresponds to C++ PrintRaw.
	 */
	public void emitRaw(String data)
	{
		writeRaw(data);
	}

	private int handleVariableWithAnnotations(Chunk chunk, Deque<AnnotationRecordEntry> annotRecords, Line line, int chunkIdx)
	{
		String varName = chunk.text;
		if (varName.isEmpty())
		{
			writeRaw(String.valueOf(options.variableDelimiter));
			return chunkIdx;
		}

		if (varName.startsWith("_start$"))
		{
			String actualVar = varName.substring(7);
			int effectiveStart = bytesWritten;
			if (pendingIndent)
			{
				effectiveStart += currentIndent;
			}
			annotRecords.push(new AnnotationRecordEntry(actualVar, effectiveStart));

			// Skip all whitespace immediately after a _start.
			int nextIdx = chunkIdx + 1;
			if (nextIdx < line.chunks.size())
			{
				Chunk nextChunk = line.chunks.get(nextIdx);
				if (!nextChunk.isVar)
				{
					String text = nextChunk.text;
					int spaces = 0;
					while (spaces < text.length() && text.charAt(spaces) == ' ')
					{
						spaces++;
					}
					writeRaw(text.substring(spaces));
					return nextIdx; // Skip to the chunk after next
				}
			}
		}
		else if (varName.startsWith("_end$"))
		{
			// If a line consisted *only* of an _end, this will likely result in
			// a blank line if we do not zap the newline after it, so we do that
			// here.
			if (line.chunks.size() == 1)
			{
				skipNextNewline = true;
			}

			String actualVar = varName.substring(5);

			if (annotRecords.isEmpty())
			{
				throw new IllegalStateException("_end without matching _start: " + actualVar);
			}

			AnnotationRecordEntry recordEntry = annotRecords.pop();

			if (!recordEntry.varName.equals(actualVar))
			{
				throw new IllegalStateException(
						"_start and _end variables must match, but got " + recordEntry.varName + " and " + actualVar + ", respectively");
			}

			AnnotationRecord record = lookupAnnotation(actualVar);
			if (record != null && options.annotationCollector != null)
			{
				options.annotationCollector.addAnnotation(recordEntry.position, bytesWritten, record.filePath, record.path, record.semantic);
			}
		}
		else
		{
			processStandardVariable(varName, line, chunkIdx);
		}

		return chunkIdx;
	}

	private void processStandardVariable(String varName, Line line, int chunkIdx)
	{
		PrinterValue sub = lookupVar(varName);
		int start = bytesWritten;
		boolean shouldConsume = false;
		if (sub.callback != null)
		{
			shouldConsume = sub.callback.getAsBoolean();
		}
		else
		{
			writeRaw(sub.text);
		}

		String consume = sub.consumeAfter;
		if (consume == null && shouldConsume)
		{
			consume = ";";
		}

		if (consume != null && chunkIdx + 1 < line.chunks.size())
		{
			Chunk next = line.chunks.get(chunkIdx + 1);
			if (!next.isVar && !next.text.isEmpty() && consume.indexOf(next.text.charAt(0)) != -1)
			{
				line.chunks.set(chunkIdx + 1, new Chunk(next.text.substring(1), false));
			}
		}

		AnnotationRecord record = lookupAnnotation(varName);
		if (record != null && options.annotationCollector != null)
		{
			options.annotationCollector.addAnnotation(start, bytesWritten, record.filePath, record.path, record.semantic);
		}
		substitutions.put(varName, new int[] { start, bytesWritten });
	}

	private Format tokenizeFormat(String formatString)
	{
		Format format = new Format();
		String processing = formatString;
		int rawStringIndent = 0;

		// Raw String Detection Logic:
		// Only strip indentation if the string starts with an explicit newline.
		if (options.stripRawStringIndentation && formatString.startsWith("\n") && formatString.length() > 1)
		{
			String original = formatString;
			String firstPPDirective = null;

			// Skip past newlines and preprocessor directives to find real code
			while (processing.startsWith("\n"))
			{
				processing = processing.substring(1);

				// clang-format will think a # at the beginning of the line in a raw
				// string is a preprocessor directive and put it at the start of the line,
				// which throws off indent calculation. Skip past those to find code that
				// is indented more realistically.
				if (processing.startsWith("#"))
				{
					// Remember the first preprocessor directive in case we need to reset
					if (firstPPDirective == null)
					{
						firstPPDirective = processing;
					}

					int nextNewline = processing.indexOf('\n');
					if (nextNewline != -1)
					{
						processing = processing.substring(nextNewline);
						continue;
					}
				}

				rawStringIndent = 0;
				format.isRawString = true;
				while (processing.startsWith(" "))
				{
					rawStringIndent++;
					processing = processing.substring(1);
				}
			}

			// Reset if we skipped through some #... lines, so that we don't drop them.
			if (firstPPDirective != null)
			{
				processing = firstPPDirective;
			}

			// If we consume the entire string, this probably wasn't a raw string and
			// was probably something like a couple of explicit newlines.
			if (processing.isEmpty())
			{
				processing = original;
				format.isRawString = false;
				rawStringIndent = 0;
			}

			// If we're not at start of line and processing starts with #,
			// this means we have a preprocessor directive and should not have eaten the newline
			if (!atStartOfLine && processing.startsWith("#"))
			{
				processing = original;
			}
			else if (format.isRawString)
			{
				// We successfully detected a raw string, so skip the initial newline
				processing = formatString.substring(1);
			}
		}

		String[] lines = processing.split("\n", -1);
		for (int i = 0; i < lines.length; i++)
		{
			String lineText = lines[i];
			int leading = 0;

			boolean shouldStrip = (i > 0) || format.isRawString;

			if (shouldStrip)
			{
				while (leading < lineText.length() && lineText.charAt(leading) == ' ')
				{
					leading++;
				}
				lineText = lineText.substring(leading);
			}

			if (format.isRawString && lineText.startsWith(options.ignoredCommentStart))
			{
				continue;
			}

			Line line = new Line();
			line.indent = Math.max(0, leading - rawStringIndent);

			String regex = Pattern.quote(String.valueOf(options.variableDelimiter));
			String[] parts = lineText.split(regex, -1);
			boolean isVar = false;

			for (String p : parts)
			{
				if (!line.chunks.isEmpty() && !isVar)
				{
					Chunk lastChunk = line.chunks.get(line.chunks.size() - 1);
					if (lastChunk.isVar && (lastChunk.text.equals("_start") || lastChunk.text.equals("_end")))
					{
						String newText = lastChunk.text + options.variableDelimiter + p;
						line.chunks.set(line.chunks.size() - 1, new Chunk(newText, true));
						continue;
					}
				}

				if (isVar || !p.isEmpty())
				{
					line.chunks.add(new Chunk(p, isVar));
				}
				isVar = !isVar;
			}
			format.lines.add(line);
		}
		return format;
	}

	private void writeIndent()
	{
		if (currentIndent > 0)
		{
			String spaces = " ".repeat(currentIndent);
			buffer.append(spaces);
			bytesWritten += spaces.length();
		}
		atStartOfLine = false;
	}

	private void writeRaw(String data)
	{
		for (char c : data.toCharArray())
		{
			buffer.append(c);
			bytesWritten++;
			if (c == '\n')
			{
				atStartOfLine = true;
				lastNewlineBytes = bytesWritten;
			}
			else
			{
				atStartOfLine = false;
			}
		}
	}

	private boolean isPureMarkers(Line line)
	{
		boolean hasMarkers = false;
		for (Chunk chunk : line.chunks)
		{
			if (chunk.isVar)
			{
				if (!chunk.text.startsWith("_start") && !chunk.text.startsWith("_end"))
				{
					return false;
				}
				hasMarkers = true;
			}
			else
			{
				if (!chunk.text.isBlank())
				{
					return false;
				}
			}
		}
		return hasMarkers;
	}

	private PrinterValue lookupVar(String var)
	{
		for (Map<String, PrinterValue> frame : varStack)
		{
			if (frame.containsKey(var))
			{
				return frame.get(var);
			}
		}
		throw new NoSuchElementException("Undefined variable: " + var);
	}

	private AnnotationRecord lookupAnnotation(String var)
	{
		for (Map<String, AnnotationRecord> frame : annotationStack)
		{
			if (frame.containsKey(var))
			{
				return frame.get(var);
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return buffer.toString();
	}
}