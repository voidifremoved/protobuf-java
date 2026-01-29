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
		// Default MUST be null to prevent swallowing punctuation for standard
		// variables
		String consumeAfter = null;

		public PrinterValue(String text)
		{
			this.text = text;
		}

		public PrinterValue(BooleanSupplier callback)
		{
			this.callback = callback;
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
				// Prevent double-wrapping if the value is already a
				// PrinterValue
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
		withVars(vars);
	}

	public AutoCloseable withIndent()
	{
		currentIndent += options.spacesPerIndent;
		return () -> currentIndent -= options.spacesPerIndent;
	}

	// --- Emission API ---

	public void emit(String formatStr)
	{
		emit(Collections.emptyMap(), formatStr);
	}

	public void emit(Map<String, Object> vars, String formatStr)
	{
		try (AutoCloseable scope = withVars(vars))
		{
			Format fmt = tokenizeFormat(formatStr);
			int baseIndent = currentIndent;
			Map<String, Integer> activeStarts = new HashMap<>();

			// Raw strings start with a newline in the template. If we aren't
			// currently at the start of a line, we need to emit that newline.
			// However, if we only wrote indentation so far, we are effectively
			// at the start of the line, so we don't need a newline.
			boolean isIndentedOnly = (bytesWritten - lastNewlineBytes) == baseIndent;
			if (fmt.isRawString && !atStartOfLine && !isIndentedOnly)
			{
				writeRaw("\n");
			}

			boolean previousLineWasPure = false;
			for (int i = 0; i < fmt.lines.size(); i++)
			{
				Line line = fmt.lines.get(i);
				boolean isPure = isPureMarkers(line);
				// Emit newline for every line after the first.
				if (i > 0 && !previousLineWasPure)
				{
					writeRaw("\n");
				}
				currentIndent = baseIndent + line.indent;

				if (atStartOfLine && !isPure && !line.chunks.isEmpty())
				{
					writeRaw(" ".repeat(currentIndent));
				}

				for (int chunkIdx = 0; chunkIdx < line.chunks.size(); chunkIdx++)
				{
					Chunk chunk = line.chunks.get(chunkIdx);
					if (!chunk.isVar)
					{
						writeRaw(chunk.text);
					}
					else
					{
						handleVariableWithAnnotations(chunk, activeStarts, line, chunkIdx);
					}
				}
				previousLineWasPure = isPure;
			}
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

	private void handleVariableWithAnnotations(Chunk chunk, Map<String, Integer> activeStarts, Line line, int chunkIdx)
	{
		String varName = chunk.text;
		if (varName.isEmpty())
		{
			writeRaw(String.valueOf(options.variableDelimiter));
			return;
		}

		if (varName.startsWith("_start$"))
		{
			activeStarts.put(varName.substring(7), bytesWritten);
		}
		else if (varName.startsWith("_end$"))
		{
			String actualVar = varName.substring(5);
			Integer start = activeStarts.remove(actualVar);
			if (start != null)
			{
				AnnotationRecord record = lookupAnnotation(actualVar);
				if (record != null && options.annotationCollector != null)
				{
					options.annotationCollector.addAnnotation(start, bytesWritten, record.filePath, record.path, record.semantic);
				}
			}
		}
		else
		{
			processStandardVariable(varName, line, chunkIdx);
		}
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

		// Raw String Detection Logic: STRICT C++ BEHAVIOR
		// Only strip indentation if the string starts with an explicit newline.
		// This avoids confusing " Indent" (standard) with "\n Indent" (raw).
		if (options.stripRawStringIndentation && formatString.startsWith("\n") && formatString.length() > 1)
		{
			String[] lines = formatString.split("\n", -1);
			boolean foundContent = false;
			int potentialIndent = 0;

			for (int i = 1; i < lines.length; i++)
			{
				String l = lines[i];
				if (!l.trim().isEmpty() && !l.trim().startsWith(options.ignoredCommentStart))
				{
					int indentCount = 0;
					while (indentCount < l.length() && l.charAt(indentCount) == ' ')
					{
						indentCount++;
					}
					potentialIndent = indentCount;
					foundContent = true;
					break;
				}
			}

			// If content was found (even if indent is 0), we treat it as a raw
			// string
			// because it started with \n.
			if (foundContent)
			{
				rawStringIndent = potentialIndent;
				format.isRawString = true;
				processing = formatString.substring(1);
			}
		}

		String[] lines = processing.split("\n", -1);
		for (int i = 0; i < lines.length; i++)
		{
			String lineText = lines[i];
			int leading = 0;

			// Strip indentation logic
			boolean shouldStrip = (i > 0) || format.isRawString;

			if (shouldStrip)
			{
				while (leading < lineText.length() && lineText.charAt(leading) == ' ')
				{
					leading++;
				}
				lineText = lineText.substring(leading);
			}

			// Ignored Comment Logic:
			// If this is a Raw String and the stripped line starts with ignored
			// comment, skip it.
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
				// Glom logic for _start and _end markers
				if (!line.chunks.isEmpty() && !isVar)
				{
					Chunk lastChunk = line.chunks.get(line.chunks.size() - 1);
					if (lastChunk.isVar && (lastChunk.text.equals("_start") || lastChunk.text.equals("_end")))
					{
						String newText = lastChunk.text + options.variableDelimiter + p;
						line.chunks.set(line.chunks.size() - 1, new Chunk(newText, true));
						continue; // Keep isVar state as is (don't toggle)
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