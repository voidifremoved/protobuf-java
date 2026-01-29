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
			Deque<AnnotationRecordEntry> annotRecords = new ArrayDeque<>();
			boolean localSkipNextNewline = false;

			for (int i = 0; i < fmt.lines.size(); i++)
			{
				Line line = fmt.lines.get(i);

				// We only print a newline for lines that follow the first; a loop iteration
				// can also hint that we should not emit another newline through the
				// `skip_next_newline` variable.
				//
				// We also assume that double newlines are undesirable, so we
				// do not emit a newline if we are at the beginning of a line, *unless* the
				// previous format line is actually empty. This behavior is specific to
				// raw strings.
				if (i > 0)
				{
					boolean prevWasEmpty = fmt.lines.get(i - 1).chunks.isEmpty();
					boolean shouldSkipNewline =
							localSkipNextNewline ||
							(fmt.isRawString && (atStartOfLine && !prevWasEmpty));
					if (!shouldSkipNewline)
					{
						writeRaw("\n");
						atStartOfLine = true;
					}
				}
				localSkipNextNewline = false;

				currentIndent = baseIndent + line.indent;

				// Process chunks only if the line has content
				if (!line.chunks.isEmpty())
				{
					for (int chunkIdx = 0; chunkIdx < line.chunks.size(); chunkIdx++)
					{
						Chunk chunk = line.chunks.get(chunkIdx);
						if (!chunk.isVar)
						{
							writeRaw(chunk.text);
						}
						else
						{
							int[] result = handleVariableWithAnnotations(chunk, annotRecords, line, chunkIdx);
							chunkIdx = result[0];
							if (result[1] == 1)
							{
								// If this line consisted only of an _end, skip the next newline
								localSkipNextNewline = true;
							}
						}
					}
				}
			}

			// For multiline raw strings, we always make sure to end on a newline.
			if (fmt.isRawString && !atStartOfLine)
			{
				writeRaw("\n");
				atStartOfLine = true;
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

	/**
	 * Emits text directly to output without indentation or variable
	 * substitution. Corresponds to C++ PrintRaw.
	 * Note: This does NOT update atStartOfLine - WriteRaw in C++ doesn't update it either.
	 * It's only updated in PrintImpl when explicitly writing newlines.
	 */
	public void emitRaw(String data)
	{
		writeRawWithoutUpdatingLineState(data);
	}
	
	private void writeRawWithoutUpdatingLineState(String data)
	{
		if (data.isEmpty())
		{
			return;
		}

		// If we're at start of line and the first character is not a newline, indent
		if (atStartOfLine && data.charAt(0) != '\n')
		{
			for (int i = 0; i < currentIndent; i++)
			{
				buffer.append(' ');
				bytesWritten++;
			}
		}

		// Write all the data
		// PrintRaw in C++ doesn't update at_start_of_line_ - it's only updated in PrintImpl
		buffer.append(data);
		bytesWritten += data.length();
		
		// Set atStartOfLine to false after writing (we've written something, so we're not at start of line anymore)
		// But don't set it to true even if data ends with \n - raw output doesn't track line state for formatted output
		atStartOfLine = false;
	}

	// Returns [chunkIdx, shouldSkipNextNewline] where shouldSkipNextNewline is 1 if we should skip next newline
	private int[] handleVariableWithAnnotations(Chunk chunk, Deque<AnnotationRecordEntry> annotRecords, Line line, int chunkIdx)
	{
		String varName = chunk.text;
		if (varName.isEmpty())
		{
			// `$$` is an escape for just `$`.
			writeRaw(String.valueOf(options.variableDelimiter));
			return new int[] { chunkIdx, 0 };
		}

		// Strip leading/trailing whitespace from variable name (for regular vars only)
		String prefix = "";
		String suffix = "";
		
		int leadingWhitespace = 0;
		while (leadingWhitespace < varName.length() && Character.isWhitespace(varName.charAt(leadingWhitespace)))
		{
			leadingWhitespace++;
		}
		if (leadingWhitespace > 0)
		{
			prefix = varName.substring(0, leadingWhitespace);
			varName = varName.substring(leadingWhitespace);
		}
		
		int trailingWhitespace = 0;
		while (trailingWhitespace < varName.length() && Character.isWhitespace(varName.charAt(varName.length() - 1 - trailingWhitespace)))
		{
			trailingWhitespace++;
		}
		if (trailingWhitespace > 0)
		{
			suffix = varName.substring(varName.length() - trailingWhitespace);
			varName = varName.substring(0, varName.length() - trailingWhitespace);
		}

		if (varName.isEmpty())
		{
			throw new IllegalArgumentException("unexpected empty variable");
		}

		boolean isStart = varName.startsWith("_start$");
		boolean isEnd = varName.startsWith("_end$");
		
		if (isStart)
		{
			String actualVar = varName.substring(7);
			// Indent if at start of line before recording position
			if (atStartOfLine)
			{
				for (int i = 0; i < currentIndent; i++)
				{
					buffer.append(' ');
					bytesWritten++;
				}
				atStartOfLine = false;
			}
			annotRecords.push(new AnnotationRecordEntry(actualVar, bytesWritten));

			// Skip all whitespace immediately after a _start.
			chunkIdx++;
			if (chunkIdx < line.chunks.size())
			{
				Chunk nextChunk = line.chunks.get(chunkIdx);
				if (!nextChunk.isVar)
				{
					String text = nextChunk.text;
					// Consume leading spaces
					while (text.startsWith(" "))
					{
						text = text.substring(1);
					}
					writeRaw(text);
					return new int[] { chunkIdx, 0 }; // Skip to the chunk after next
				}
			}
			return new int[] { chunkIdx - 1, 0 }; // Return current chunk index if no next chunk
		}
		else if (isEnd)
		{
			// If a line consisted *only* of an _end, this will likely result in
			// a blank line if we do not zap the newline after it, so we do that
			// here.
			boolean shouldSkip = (line.chunks.size() == 1);

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
			if (record == null)
			{
				throw new IllegalStateException("undefined annotation variable: \"" + actualVar + "\"");
			}

			if (options.annotationCollector != null)
			{
				options.annotationCollector.addAnnotation(recordEntry.position, bytesWritten, record.filePath, record.path, record.semantic);
			}
			
			return new int[] { chunkIdx, shouldSkip ? 1 : 0 };
		}
		else
		{
			// For regular variables, use prefix/suffix handling
			processStandardVariable(varName, prefix, suffix, line, chunkIdx);
			return new int[] { chunkIdx, 0 };
		}
	}

	private void processStandardVariable(String varName, String prefix, String suffix, Line line, int chunkIdx)
	{
		PrinterValue sub = lookupVar(varName);
		int rangeStart = bytesWritten;
		int rangeEnd = bytesWritten;

		if (sub.callback != null)
		{
			// Substitution that resolves to callback cannot contain whitespace
			if (!prefix.isEmpty() || !suffix.isEmpty())
			{
				throw new IllegalArgumentException("substitution that resolves to callback cannot contain whitespace");
			}
			rangeStart = bytesWritten;
			if (!sub.callback.getAsBoolean())
			{
				throw new IllegalStateException("recursive call encountered while evaluating \"" + varName + "\"");
			}
			rangeEnd = bytesWritten;
		}
		else
		{
			// By returning here in case of empty we also skip possible spaces inside
			// the $...$, i.e. "void$ dllexpor$ f();" -> "void f();" in the empty case.
			if (!sub.text.isEmpty())
			{
				// If `sub` is empty, we do not print the spaces around it.
				writeRaw(prefix);
				writeRaw(sub.text);
				rangeEnd = bytesWritten;
				rangeStart = rangeEnd - sub.text.length();
				writeRaw(suffix);
			}
		}

		// Handle consume_after
		if (sub.consumeAfter != null && chunkIdx + 1 < line.chunks.size())
		{
			Chunk next = line.chunks.get(chunkIdx + 1);
			if (!next.isVar && !next.text.isEmpty())
			{
				char firstChar = next.text.charAt(0);
				if (sub.consumeAfter.indexOf(firstChar) != -1)
				{
					line.chunks.set(chunkIdx + 1, new Chunk(next.text.substring(1), false));
				}
			}
		}

		AnnotationRecord record = lookupAnnotation(varName);
		if (record != null && options.annotationCollector != null)
		{
			options.annotationCollector.addAnnotation(rangeStart, rangeEnd, record.filePath, record.path, record.semantic);
		}
		substitutions.put(varName, new int[] { rangeStart, rangeEnd });
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
				// Keep format.isRawString and rawStringIndent as calculated
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
				format.isRawString = false;
				rawStringIndent = 0;
			}
		}

		// We now split the remaining format string into lines and discard:
		//   1. A trailing Printer-discarded comment, if this is a raw string.
		//   2. All leading spaces to compute that line's indent.
		//      We do not do this for the first line, so that Emit("  ") works correctly.
		//   3. Set the indent for that line to max(0, line_indent - raw_string_indent).
		//   4. Trailing empty lines, if we know this is a raw string.
		boolean isFirst = true;
		String[] lines = processing.split("\n", -1);
		for (int i = 0; i < lines.length; i++)
		{
			String lineText = lines[i];
			
			if (format.isRawString)
			{
				int commentIndex = lineText.indexOf(options.ignoredCommentStart);
				if (commentIndex != -1)
				{
					String beforeComment = lineText.substring(0, commentIndex);
					// Strip leading whitespace to check if line becomes empty
					String stripped = beforeComment.replaceAll("^\\s+", "");
					if (stripped.isEmpty())
					{
						// If the first line is part of an ignored comment, consider that a first line as well.
						isFirst = false;
						continue;
					}
					lineText = beforeComment;
				}
			}

			int lineIndent = 0;
			if (!isFirst)
			{
				while (lineIndent < lineText.length() && lineText.charAt(lineIndent) == ' ')
				{
					lineIndent++;
				}
				lineText = lineText.substring(lineIndent);
			}
			isFirst = false;

			Line line = new Line();
			line.indent = lineIndent > rawStringIndent ? lineIndent - rawStringIndent : 0;

			// Split line into chunks along variable delimiters
			String regex = Pattern.quote(String.valueOf(options.variableDelimiter));
			String[] parts = lineText.split(regex, -1);
			boolean isVar = false;
			int totalLen = 0;

			for (String p : parts)
			{
				// The special _start and _end variables should actually glom the next
				// chunk into themselves, so as to be of the form _start$foo and _end$foo.
				if (!line.chunks.isEmpty() && !isVar)
				{
					Chunk prev = line.chunks.get(line.chunks.size() - 1);
					if (prev.isVar && (prev.text.equals("_start") || prev.text.equals("_end")))
					{
						// The +1 below is to account for the $ in between them.
						String newText = prev.text + options.variableDelimiter + p;
						line.chunks.set(line.chunks.size() - 1, new Chunk(newText, true));
						
						// Account for the foo$ part of $_start$foo$.
						totalLen += p.length() + 1;
						continue;
					}
				}

				if (isVar || !p.isEmpty())
				{
					line.chunks.add(new Chunk(p, isVar));
				}

				totalLen += p.length();
				if (isVar)
				{
					// This accounts for the $s around a variable.
					totalLen += 2;
				}

				isVar = !isVar;
			}

			// To ensure there are no unclosed $...$, we check that the computed length
			// above equals the actual length of the string. If it's off, that means
			// that there are missing or extra $ characters.
			if (totalLen != lineText.length())
			{
				if (line.chunks.isEmpty())
				{
					throw new IllegalArgumentException("wrong number of variable delimiters");
				}
				Chunk lastChunk = line.chunks.get(line.chunks.size() - 1);
				throw new IllegalArgumentException("unclosed variable name: `" + lastChunk.text + "`");
			}

			// Trim any empty, non-variable chunks.
			while (!line.chunks.isEmpty())
			{
				Chunk last = line.chunks.get(line.chunks.size() - 1);
				if (last.isVar || !last.text.isEmpty())
				{
					break;
				}
				line.chunks.remove(line.chunks.size() - 1);
			}

			// Add the line even if it's empty (for non-raw strings, empty lines are kept)
			format.lines.add(line);
		}

		// Discard any trailing newlines (i.e., lines which contain no chunks.)
		if (format.isRawString)
		{
			while (!format.lines.isEmpty() && format.lines.get(format.lines.size() - 1).chunks.isEmpty())
			{
				format.lines.remove(format.lines.size() - 1);
			}
		}

		return format;
	}

	private void writeRaw(String data)
	{
		if (data.isEmpty())
		{
			return;
		}

		// If we're at start of line and the first character is not a newline, indent
		if (atStartOfLine && data.charAt(0) != '\n')
		{
			for (int i = 0; i < currentIndent; i++)
			{
				buffer.append(' ');
				bytesWritten++;
			}
			atStartOfLine = false;
		}

		// Write all the data character by character to track newlines
		// This is used by emit() to track state, unlike writeRawWithoutUpdatingLineState
		for (int i = 0; i < data.length(); i++)
		{
			char c = data.charAt(i);
			buffer.append(c);
			bytesWritten++;
			atStartOfLine = (c == '\n');
		}
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