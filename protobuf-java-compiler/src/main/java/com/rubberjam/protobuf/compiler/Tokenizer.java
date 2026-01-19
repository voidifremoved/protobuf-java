// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler;

import java.io.IOException;
import java.io.Reader;

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

		@Override
		public String toString()
		{
			return "Token{" +
					"type=" + type +
					", text='" + text + '\'' +
					", line=" + line +
					", column=" + column +
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
		nextChar();
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
		currentToken.line = line;
		currentToken.column = column;

		// Skip whitespace.
		while (Character.isWhitespace(currentChar))
		{
			nextChar();
		}

		// Skip comments.
		if (currentChar == '/')
		{
			nextChar();
			if (currentChar == '/')
			{
				// Line comment.
				while (currentChar != '\n' && !atEnd)
				{
					nextChar();
				}
				return next();
			}
			else if (currentChar == '*')
			{
				// Block comment.
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
					}
					else
					{
						nextChar();
					}
				}
				return next();
			}
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
			while (Character.isDigit(currentChar))
			{
				sb.append(currentChar);
				nextChar();
			}
			if (currentChar == '.')
			{
				sb.append(currentChar);
				nextChar();
				while (Character.isDigit(currentChar))
				{
					sb.append(currentChar);
					nextChar();
				}
				currentToken.type = TokenType.FLOAT;
			}
			else
			{
				currentToken.type = TokenType.INTEGER;
			}
			currentToken.text = sb.toString();
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
					switch (currentChar)
					{
					case 'n':
						sb.append('\n');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'r':
						sb.append('\r');
						break;
					case '\\':
						sb.append('\\');
						break;
					case '\"':
						sb.append('\"');
						break;
					case '\'':
						sb.append('\'');
						break;
					default:
						sb.append(currentChar);
						break;
					}
				}
				else
				{
					sb.append(currentChar);
				}
				nextChar();
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
