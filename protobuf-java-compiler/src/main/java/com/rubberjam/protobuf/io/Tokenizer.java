// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.io;

import java.io.IOException;
import java.io.Reader;

import com.rubberjam.protobuf.compiler.ErrorCollector;

/**
 * Tokenizer for .proto files.
 */
public class Tokenizer
{

	/**
	 * The type of a token.
	 */
	public enum TokenType
	{
		START,
		END,
		IDENTIFIER,
		INTEGER,
		FLOAT,
		STRING,
		SYMBOL,
		WHITESPACE,
		NEWLINE
	}

	/**
	 * Represents a token.
	 */
	public static class Token
	{
		public TokenType type;
		public String text;
		public int line;
		public int column;
		public int endColumn;
		public String leadingComments;
		public String trailingComments;
		public java.util.List<String> leadingDetachedComments;

		
		
		public Token()
		{
			super();
		}



		public Token(TokenType type, String text, int line, int column, int endColumn)
		{
			super();
			this.type = type;
			this.text = text;
			this.line = line;
			this.column = column;
			this.endColumn = endColumn;
		}



		@Override
		public String toString()
		{
			return "Token{" +
					"type=" + type +
					", text='" + text + '\'' +
					", line=" + line +
					", column=" + column +
					", leadingComments='" + leadingComments + '\'' +
					", trailingComments='" + trailingComments + '\'' +
					'}';
		}
	}

	private final Reader input;
	private final ErrorCollector errorCollector;

	private char currentChar;
	private int line;
	private int column;
	private boolean atEnd = false;

	private Token currentToken = new Token();
	private Token previousToken = new Token();

	/**
	 * Constructs a new Tokenizer.
	 *
	 * @param input
	 *            The input stream to read from.
	 * @param errorCollector
	 *            The error collector to report errors to.
	 */
	public Tokenizer(Reader input, ErrorCollector errorCollector)
	{
		this.input = input;
		this.errorCollector = errorCollector;
		this.line = 0;
		this.column = 0;
		this.currentToken.type = TokenType.START;
		if (input != null)
		{
			nextChar();
		}
	}

	/**
	 * The current token.
	 */
	public Token current()
	{
		return currentToken;
	}

	/**
	 * The previous token.
	 */
	public Token previous()
	{
		return previousToken;
	}

	/**
	 * Advances to the next token.
	 *
	 * @return false if the end of the input is reached.
	 */
	public boolean next()
	{
		previousToken = currentToken;
		currentToken = new Token();

		// Phase 1: Check for trailing comments on the same line as previous token
		if (previousToken.type != TokenType.START)
		{
			StringBuilder trailingComments = new StringBuilder();
			while (true)
			{
				// Skip horizontal whitespace only
				while (Character.isWhitespace(currentChar) && currentChar != '\n')
				{
					nextChar();
				}

				if (currentChar == '\n')
				{
					// For block openers like '{' or closers like '}', we don't want to attach comments on the next line
					// as trailing comments. They should be leading comments for the next item.
					if (previousToken.text != null && (previousToken.text.equals("{") || previousToken.text.equals("}")))
					{
						break;
					}

					// Newline found. Check if the next line starts with a comment (and no blank line)
					nextChar(); // consume newline

					// Look ahead for comments on the next line
					boolean foundCommentOnNextLine = false;

					// Skip horizontal whitespace on the new line
					while (Character.isWhitespace(currentChar) && currentChar != '\n')
					{
						nextChar();
					}

					if (currentChar == '\n')
					{
						// Blank line found (two consecutive newlines with only whitespace between).
						// Stop trailing comment search.
						break;
					}

					// Check for comment start on the new line
					if (currentChar == '/')
					{
						// Try to read next char to verify it is a comment
						nextChar();
						if (currentChar == '/' || currentChar == '*')
						{
							// It is a comment. Parse it.
							// We already consumed first '/'. currentChar is second '/' or '*'
							String comment = parseCommentBody();
							trailingComments.append(comment);
							foundCommentOnNextLine = true;
						}
						else
						{
							// Not a comment. It's a symbol '/' starting the next line.
							// This is the start of the next token.
							// We must return this '/' as the next token.

							currentToken.text = "/";
							currentToken.type = TokenType.SYMBOL;
							currentToken.line = line;
							currentToken.column = column - 1;

							if (trailingComments.length() > 0)
							{
								previousToken.trailingComments = trailingComments.toString();
							}
							return true;
						}
					}

					if (!foundCommentOnNextLine)
					{
						// Next line does not start with a comment. It starts with a token.
						// Stop trailing comment search.
						break;
					}
					// If we found a comment, loop continues to look for more comments
				}
				else if (atEnd)
				{
					break;
				}
				else if (currentChar == '/')
				{
					// Try to read next char to verify it is a comment
					// We have to consume '/' temporarily.
					nextChar();
					if (currentChar == '/' || currentChar == '*')
					{
						// It is a comment. Parse it.
						// We already consumed first '/'. currentChar is second '/' or '*'
						String comment = parseCommentBody();
						trailingComments.append(comment);
					}
					else
					{
						// Not a comment. It's a symbol '/'.
						// We consumed '/', so previous char was '/'.
						// Since we can't unread easily, we must handle this state.
						// We set currentToken to '/' and return.
						currentToken.text = "/";
						currentToken.type = TokenType.SYMBOL;
						currentToken.line = line;
						currentToken.column = column - 1; // Approx

						// If we found trailing comments before this slash, attach them
						if (trailingComments.length() > 0)
						{
							previousToken.trailingComments = trailingComments.toString();
						}
						return true;
					}
				}
				else
				{
					// Not whitespace, newline, or comment - start of next token
					break;
				}
			}

			if (trailingComments.length() > 0)
			{
				previousToken.trailingComments = trailingComments.toString();
			}
		}

		// Phase 2: Leading comments for current token
		StringBuilder leadingComments = new StringBuilder();
		while (true)
		{
			// Skip any whitespace (including newlines)
			while (Character.isWhitespace(currentChar))
			{
				nextChar();
			}

			if (atEnd)
			{
				break;
			}

			if (currentChar == '/')
			{
				nextChar();
				if (currentChar == '/' || currentChar == '*')
				{
					String comment = parseCommentBody();
					leadingComments.append(comment);
				}
				else
				{
					// Not a comment. Return '/' symbol.
					currentToken.text = "/";
					currentToken.type = TokenType.SYMBOL;
					currentToken.line = line;
					currentToken.column = column - 1;

					if (leadingComments.length() > 0)
					{
						currentToken.leadingComments = leadingComments.toString();
					}
					return true;
				}
			}
			else
			{
				break;
			}
		}

		currentToken.line = line;
		currentToken.column = column;

		if (leadingComments.length() > 0)
		{
			currentToken.leadingComments = leadingComments.toString();
		}

		if (atEnd)
		{
			currentToken.type = TokenType.END;
			return false;
		}

		if (Character.isLetter(currentChar) || currentChar == '_')
		{
			StringBuilder sb = new StringBuilder();
			while (Character.isLetterOrDigit(currentChar) || currentChar == '_')
			{
				sb.append(currentChar);
				nextChar();
			}
			currentToken.text = sb.toString();
			currentToken.type = TokenType.IDENTIFIER;
		}
		else if (Character.isDigit(currentChar))
		{
			StringBuilder sb = new StringBuilder();
			if (currentChar == '0') {
				sb.append(currentChar);
				nextChar();
				if (currentChar == 'x' || currentChar == 'X') {
					sb.append(currentChar);
					nextChar();
					while (isHexDigit(currentChar)) {
						sb.append(currentChar);
						nextChar();
					}
					currentToken.type = TokenType.INTEGER;
					currentToken.text = sb.toString();
					return true;
				}
			}

			while (Character.isDigit(currentChar))
			{
				sb.append(currentChar);
				nextChar();
			}

			boolean isFloat = false;
			if (currentChar == '.')
			{
				isFloat = true;
				sb.append(currentChar);
				nextChar();
				while (Character.isDigit(currentChar))
				{
					sb.append(currentChar);
					nextChar();
				}
			}

			if (currentChar == 'e' || currentChar == 'E') {
				isFloat = true;
				sb.append(currentChar);
				nextChar();
				if (currentChar == '+' || currentChar == '-') {
					sb.append(currentChar);
					nextChar();
				}
				while (Character.isDigit(currentChar)) {
					sb.append(currentChar);
					nextChar();
				}
			}

			currentToken.type = isFloat ? TokenType.FLOAT : TokenType.INTEGER;
			currentToken.text = sb.toString();
		}
		else if (currentChar == '.')
		{
			// Check for float starting with .
			nextChar();
			if (Character.isDigit(currentChar))
			{
				StringBuilder sb = new StringBuilder(".");
				while (Character.isDigit(currentChar))
				{
					sb.append(currentChar);
					nextChar();
				}
				if (currentChar == 'e' || currentChar == 'E') {
					sb.append(currentChar);
					nextChar();
					if (currentChar == '+' || currentChar == '-') {
						sb.append(currentChar);
						nextChar();
					}
					while (Character.isDigit(currentChar)) {
						sb.append(currentChar);
						nextChar();
					}
				}
				currentToken.type = TokenType.FLOAT;
				currentToken.text = sb.toString();
			}
			else
			{
				currentToken.text = ".";
				currentToken.type = TokenType.SYMBOL;
			}
		}
		else if (currentChar == '\"' || currentChar == '\'')
		{
			char delimiter = currentChar;
			nextChar();
			StringBuilder sb = new StringBuilder();
			while (currentChar != delimiter && !atEnd)
			{
				if (currentChar == '\\')
				{
					nextChar();
					if (atEnd) {
						errorCollector.recordError(line, column, "Unexpected end of string.");
						break;
					}
					switch (currentChar)
					{
					case 'a': sb.append((char) 0x07); nextChar(); break;
					case 'b': sb.append('\b'); nextChar(); break;
					case 'f': sb.append('\f'); nextChar(); break;
					case 'n': sb.append('\n'); nextChar(); break;
					case 'r': sb.append('\r'); nextChar(); break;
					case 't': sb.append('\t'); nextChar(); break;
					case 'v': sb.append((char) 0x0b); nextChar(); break;
					case '\\': sb.append('\\'); nextChar(); break;
					case '\'': sb.append('\''); nextChar(); break;
					case '\"': sb.append('\"'); nextChar(); break;
					case '?': sb.append('?'); nextChar(); break;

					case '0': case '1': case '2': case '3':
					case '4': case '5': case '6': case '7':
					{
						int code = 0;
						for (int i = 0; i < 3; ++i) {
							if (currentChar >= '0' && currentChar <= '7') {
								code = (code * 8) + (currentChar - '0');
								nextChar();
							} else {
								break;
							}
						}
						sb.append((char) code);
						break;
					}

					case 'x': case 'X':
					{
						nextChar();
						int code = 0;
						if (isHexDigit(currentChar)) {
							code = Character.digit(currentChar, 16);
							nextChar();
							if (isHexDigit(currentChar)) {
								code = (code * 16) + Character.digit(currentChar, 16);
								nextChar();
							}
						} else {
							errorCollector.recordError(line, column, "Invalid hex escape.");
						}
						sb.append((char) code);
						break;
					}

					case 'u':
					{
						nextChar();
						int code = 0;
						for (int i = 0; i < 4; i++) {
							if (isHexDigit(currentChar)) {
								code = (code * 16) + Character.digit(currentChar, 16);
								nextChar();
							} else {
								errorCollector.recordError(line, column, "Invalid unicode escape.");
								break;
							}
						}
						sb.append((char) code);
						break;
					}

					case 'U':
					{
						nextChar();
						int code = 0;
						for (int i = 0; i < 8; i++) {
							if (isHexDigit(currentChar)) {
								code = (code * 16) + Character.digit(currentChar, 16);
								nextChar();
							} else {
								errorCollector.recordError(line, column, "Invalid unicode escape.");
								break;
							}
						}
						sb.appendCodePoint(code);
						break;
					}

					default:
						// Invalid escape sequence, but usually we just print it as literal backslash + char
						// in robust parsers, or error out. C++ parser records error.
						// C++ tokenizer.cc: RecordError("Invalid escape sequence."); text->push_back('\\'); text->push_back(current_char_);
						// So we append both.
						errorCollector.recordError(line, column, "Invalid escape sequence.");
						sb.append('\\');
						sb.append(currentChar);
						nextChar();
						break;
					}
				}
				else
				{
					sb.append(currentChar);
					nextChar();
				}
			}
			if (currentChar == delimiter)
			{
				nextChar();
			}
			else
			{
				errorCollector.recordError(line, column, "Unterminated string literal.");
			}
			currentToken.text = sb.toString();
			currentToken.type = TokenType.STRING;
		}
		else
		{
			currentToken.text = String.valueOf(currentChar);
			currentToken.type = TokenType.SYMBOL;
			nextChar();
		}

		return true;
	}

	private String parseCommentBody()
	{
		StringBuilder comment = new StringBuilder();
		if (currentChar == '/')
		{
			// Line comment.
			nextChar(); // Skip second '/'
			while (currentChar != '\n' && !atEnd)
			{
				comment.append(currentChar);
				nextChar();
			}
			// Don't consume newline here, let loop handle it

			String c = comment.toString();
			if (c.startsWith(" "))
			{
				c = c.substring(1);
			}
			return c + "\n";
		}
		else if (currentChar == '*')
		{
			// Block comment.
			nextChar(); // Skip '*'
			StringBuilder rawComment = new StringBuilder();
			while (!atEnd)
			{
				if (currentChar == '*')
				{
					nextChar();
					if (currentChar == '/')
					{
						nextChar();
						break;
					}
					rawComment.append('*');
				}
				else
				{
					rawComment.append(currentChar);
					nextChar();
				}
			}

			// Clean up block comment
			String[] lines = rawComment.toString().split("\n");
			StringBuilder cleaned = new StringBuilder();
			for (String l : lines)
			{
				String trimmed = l.trim();
				if (trimmed.startsWith("*"))
				{
					trimmed = trimmed.substring(1);
				}
				if (trimmed.startsWith(" "))
				{
					trimmed = trimmed.substring(1);
				}
				cleaned.append(trimmed).append('\n');
			}
			return cleaned.toString();
		}
		return "";
	}

	private boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	private void nextChar()
	{
		try
		{
			int c = input.read();
			if (c == -1)
			{
				atEnd = true;
				currentChar = '\0';
			}
			else
			{
				currentChar = (char) c;
				if (currentChar == '\n')
				{
					line++;
					column = 0;
				}
				else
				{
					column++;
				}
			}
		}
		catch (IOException e)
		{
			errorCollector.recordError(line, column, "Error reading from input: " + e.getMessage());
			atEnd = true;
			currentChar = '\0';
		}
	}
}
