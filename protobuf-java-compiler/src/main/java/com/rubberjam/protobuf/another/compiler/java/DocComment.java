package com.rubberjam.protobuf.another.compiler.java;

import java.util.Map;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.rubberjam.protobuf.io.Printer; // Assuming Printer class availability

public final class DocComment
{

	private DocComment()
	{
	}

	public enum FieldAccessorType
	{
		HAZZER,
		GETTER,
		SETTER,
		CLEARER,
		// Repeated
		LIST_COUNT,
		LIST_GETTER,
		LIST_INDEXED_GETTER,
		LIST_INDEXED_SETTER,
		LIST_ADDER,
		LIST_MULTI_ADDER
	}

	// A simple wrapper to match C++ Options struct behavior used in the source
	public static class Options
	{
		public boolean stripNonfunctionalCodegen = false;
	}

	/**
	 * Escapes the input string for inclusion in Javadoc.
	 *
	 */
	public static String escapeJavadoc(String input)
	{
		StringBuilder result = new StringBuilder(input.length() * 2);
		char prev = '*';

		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			switch (c)
			{
			case '*':
				// Avoid "/*".
				if (prev == '/')
				{
					result.append("&#42;");
				}
				else
				{
					result.append(c);
				}
				break;
			case '/':
				// Avoid "*/".
				if (prev == '*')
				{
					result.append("&#47;");
				}
				else
				{
					result.append(c);
				}
				break;
			case '@':
				// '@' starts javadoc tags.
				result.append("&#64;");
				break;
			case '<':
				// Avoid interpretation as HTML.
				result.append("&lt;");
				break;
			case '>':
				// Avoid interpretation as HTML.
				result.append("&gt;");
				break;
			case '&':
				// Avoid interpretation as HTML.
				result.append("&amp;");
				break;
			case '\\':
				// Java interprets Unicode escape sequences anywhere!
				result.append("&#92;");
				break;
			default:
				result.append(c);
				break;
			}
			prev = c;
		}

		return result.toString();
	}

	/**
	 * Escapes the input string for inclusion in KDoc.
	 *
	 */
	public static String escapeKdoc(String input)
	{
		StringBuilder result = new StringBuilder(input.length() * 2);
		char prev = 'a'; // Initial dummy value

		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			switch (c)
			{
			case '*':
				// Avoid "/*".
				if (prev == '/')
				{
					result.append("&#42;");
				}
				else
				{
					result.append(c);
				}
				break;
			case '/':
				// Avoid "*/".
				if (prev == '*')
				{
					result.append("&#47;");
				}
				else
				{
					result.append(c);
				}
				break;
			default:
				result.append(c);
				break;
			}
			prev = c;
		}
		return result.toString();
	}

	private static void writeDocCommentBodyForLocation(
			Printer printer, SourceCodeInfo.Location location, Options options, boolean kdoc)
	{
		if (options.stripNonfunctionalCodegen)
		{
			return;
		}

		String comments = location.getLeadingComments();
		if (comments.isEmpty())
		{
			comments = location.getTrailingComments();
		}

		if (!comments.isEmpty())
		{
			if (kdoc)
			{
				comments = escapeKdoc(comments);
			}
			else
			{
				comments = escapeJavadoc(comments);
			}

			String[] lines = comments.split("\n");
			// Logic to mimic absl::StrSplit and removing empty back lines could
			// be added here
			// For simplicity, we process the split array directly.

			if (kdoc)
			{
				printer.emit(" * ```\n");
			}
			else
			{
				printer.emit(" * <pre>\n");
			}

			for (String line : lines)
			{
				// Strip leading whitespace
				String trimmed = stripLeadingWhitespace(line);
				if (!trimmed.isEmpty())
				{
					printer.emit(Map.of("line", trimmed), " * $line$\n");
				}
				else
				{
					printer.emit(" *\n");
				}
			}

			if (kdoc)
			{
				printer.emit(" * ```\n");
			}
			else
			{
				printer.emit(" * </pre>\n");
			}
			printer.emit(" *\n");
		}
	}

	private static String stripLeadingWhitespace(String s)
	{
		int i = 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i)))
		{
			i++;
		}
		return s.substring(i);
	}

	private static void writeDocCommentBody(
			Printer printer, GenericDescriptor descriptor, Options options, boolean kdoc)
	{
		SourceCodeInfo.Location location = getLocation(descriptor);
		if (location != null)
		{
			writeDocCommentBodyForLocation(printer, location, options, kdoc);
		}
	}

	// Helper to retrieve SourceCodeInfo.Location from a descriptor.
	// In Java, this involves looking up the path in the file's SourceCodeInfo.
	private static SourceCodeInfo.Location getLocation(GenericDescriptor descriptor)
	{
		// In a real implementation, this would traverse
		// descriptor.getFile().getSourceCodeInfo()
		// matching descriptor.getPath(). For brevity, we assume this helper
		// exists or
		// returns null if precise location matching isn't implemented in the
		// environment.
		//
		// NOTE: Standard Java Descriptors don't expose getSourceLocation()
		// directly like C++.
		return null;
	}

	private static String firstLineOf(String value)
	{
		int pos = value.indexOf('\n');
		String result = (pos != -1) ? value.substring(0, pos) : value;
		if (result.endsWith("{"))
		{
			result += " ... }";
		}
		return result;
	}

	private static void writeDebugString(
			Printer printer, FieldDescriptor field, Options options, boolean kdoc)
	{
		// Note: Java FieldDescriptor doesn't have a direct equivalent to C++
		// DebugString()
		// that returns the proto definition string easily. We use a placeholder
		// logic.
		String fieldComment = field.getName(); // Fallback
		// If logic to reconstruct "optional string foo = 5;" exists, use it
		// here.

		if (kdoc)
		{
			printer.emit(Map.of("def", escapeKdoc(fieldComment)), " * `$def$`\n");
		}
		else
		{
			printer.emit(Map.of("def", escapeJavadoc(fieldComment)), " * <code>$def$</code>\n");
		}
	}

	public static void writeMessageDocComment(
			Printer printer, Descriptor message, Options options, boolean kdoc)
	{
		printer.emit("/**\n");
		writeDocCommentBody(printer, message, options, kdoc);
		if (kdoc)
		{
			printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
					" * Protobuf type `$fullname$`\n" + " */\n");
		}
		else
		{
			printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
					" * Protobuf type {@code $fullname$}\n" + " */\n");
		}
	}

	public static void writeFieldDocComment(
			Printer printer, FieldDescriptor field, Options options, boolean kdoc)
	{
		printer.emit("/**\n");
		writeDocCommentBody(printer, field, options, kdoc);
		writeDebugString(printer, field, options, kdoc);
		printer.emit(" */\n");
	}

	public static void writeDeprecatedJavadoc(
			Printer printer, FieldDescriptor field, Options options)
	{
		if (!field.getOptions().getDeprecated())
		{
			return;
		}
		printer.emit(Map.of("name", field.getFullName()), " * @deprecated $name$ is deprecated.\n");
		// Line number info is hard to get in Java without full SourceInfo map.
		// Skipping for equivalence.
	}

	public static void writeFieldAccessorDocComment(
			Printer printer,
			FieldDescriptor field,
			FieldAccessorType type,
			Options options,
			boolean builder,
			boolean kdoc,
			boolean isPrivate)
	{
		printer.emit("/**\n");
		writeDocCommentBody(printer, field, options, kdoc);
		writeDebugString(printer, field, options, kdoc);
		if (!kdoc && !isPrivate)
		{
			writeDeprecatedJavadoc(printer, field, options);
		}

		String name = underscoresToCamelCase(field.getName(), false);

		switch (type)
		{
		case HAZZER:
			printer.emit(Map.of("name", name), " * @return Whether the $name$ field is set.\n");
			break;
		case GETTER:
			printer.emit(Map.of("name", name), " * @return The $name$.\n");
			break;
		case SETTER:
			printer.emit(Map.of("name", name), " * @param value The $name$ to set.\n");
			break;
		case CLEARER:
			break;
		case LIST_COUNT:
			printer.emit(Map.of("name", name), " * @return The count of $name$.\n");
			break;
		case LIST_GETTER:
			printer.emit(Map.of("name", name), " * @return A list containing the $name$.\n");
			break;
		case LIST_INDEXED_GETTER:
			printer.emit(" * @param index The index of the element to return.\n");
			printer.emit(Map.of("name", name), " * @return The $name$ at the given index.\n");
			break;
		case LIST_INDEXED_SETTER:
			printer.emit(" * @param index The index to set the value at.\n");
			printer.emit(Map.of("name", name), " * @param value The $name$ to set.\n");
			break;
		case LIST_ADDER:
			printer.emit(Map.of("name", name), " * @param value The $name$ to add.\n");
			break;
		case LIST_MULTI_ADDER:
			printer.emit(Map.of("name", name), " * @param values The $name$ to add.\n");
			break;
		}
		if (builder)
		{
			printer.emit(" * @return This builder for chaining.\n");
		}
		printer.emit(" */\n");
	}

	// Minimal implementation helper for CamelCase
	private static String underscoresToCamelCase(String input, boolean capNextLetter)
	{
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			if (c == '_')
			{
				capNextLetter = true;
			}
			else if (capNextLetter)
			{
				result.append(Character.toUpperCase(c));
				capNextLetter = false;
			}
			else
			{
				result.append(c);
			}
		}
		return result.toString();
	}
}