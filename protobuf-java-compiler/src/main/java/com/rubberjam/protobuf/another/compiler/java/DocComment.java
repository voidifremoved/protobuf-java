package com.rubberjam.protobuf.another.compiler.java;

import java.util.Map;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.io.Printer; // Assuming Printer class availability

public final class DocComment
{

	private DocComment()
	{
	}

	public enum AccessorType
	{
		HAZZER,
		GETTER,
		SETTER,
		CLEARER,
        BYTES_GETTER,
		// Repeated
		LIST_COUNT,
		LIST_GETTER,
		LIST_INDEXED_GETTER,
		LIST_INDEXED_SETTER,
		LIST_ADDER,
		LIST_MULTI_ADDER,
		// Map
		MAP_ENTRY_ADDER,
		MAP_MULTI_ADDER,
		MAP_ENTRY_REMOVER
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
		if (options.isStripNonfunctionalCodegen())
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
        String fieldComment = getFieldDefinition(field);

		if (kdoc)
		{
			printer.emit(Map.of("def", escapeKdoc(fieldComment)), " * `$def$`\n");
		}
		else
		{
			printer.emit(Map.of("def", escapeJavadoc(fieldComment)), " * <code>$def$</code>\n");
		}
	}

    private static String getFieldDefinition(FieldDescriptor field) {
        StringBuilder sb = new StringBuilder();

        if (field.isRepeated()) {
            sb.append("repeated ");
        } else if (field.isRequired()) {
            sb.append("required ");
        } else {
            // Check syntax via toProto() string
            String syntax = field.getFile().toProto().getSyntax();
            if (syntax.isEmpty() || "proto2".equals(syntax)) {
                sb.append("optional ");
            } else if (field.toProto().hasProto3Optional()) { // Use Proto3Optional if available? Or just check label?
                 // But hasProto3Optional is usually true for explicit optional in proto3.
                 // In proto2, optional is default, no proto3_optional bit.
                 // Actually, use toProto().getLabel()
                 if (field.toProto().getLabel() == com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
                     // Check if implicit proto3 optional?
                     // In proto3, if not proto3_optional, it is implicit (no label printed).
                     // If it is proto3_optional, it is "optional".
                     if ("proto3".equals(syntax)) {
                         if (field.toProto().getProto3Optional()) {
                             sb.append("optional ");
                         }
                     } else {
                         // Proto2
                         sb.append("optional ");
                     }
                 }
            } else {
                 // Fallback if no hasProto3Optional method? It should exist in recent protobuf-java.
                 // If not, we can assume proto2 optional if not repeated/required.
                 if (syntax.isEmpty() || "proto2".equals(syntax)) {
                     sb.append("optional ");
                 }
            }
        }

        if (field.getType() == FieldDescriptor.Type.MESSAGE) {
             sb.append(field.getMessageType().getFullName());
        } else if (field.getType() == FieldDescriptor.Type.ENUM) {
             sb.append(field.getEnumType().getFullName());
        } else {
             sb.append(field.getType().name().toLowerCase());
        }

        sb.append(" ").append(field.getName()).append(" = ").append(field.getNumber());

        if (shouldPrintDefault(field)) {
             sb.append(" [default = ");
             sb.append(formatDefaultValue(field));
             sb.append("]");
        }

        sb.append(";");
        return sb.toString();
    }

    private static boolean shouldPrintDefault(FieldDescriptor field) {
        if (field.isRepeated()) return false;
        if (field.getType() == FieldDescriptor.Type.MESSAGE) return false;
        if (field.getType() == FieldDescriptor.Type.GROUP) return false;

        String syntax = field.getFile().toProto().getSyntax();
        if ("proto3".equals(syntax)) {
            return false;
        }

        return field.hasDefaultValue();
    }

    private static String formatDefaultValue(FieldDescriptor field) {
        Object val = field.getDefaultValue();
        if (field.getType() == FieldDescriptor.Type.STRING) {
            return "\"" + escapeString((String)val) + "\"";
        } else if (field.getType() == FieldDescriptor.Type.BYTES) {
            return "\"" + escapeString(((com.google.protobuf.ByteString)val).toStringUtf8()) + "\""; // Approx
        } else if (field.getType() == FieldDescriptor.Type.ENUM) {
            return ((EnumValueDescriptor)val).getName();
        } else {
            return val.toString();
        }
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
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

	public static void writeMessageDocComment(
			Printer printer, EnumDescriptor message, Options options, boolean kdoc)
	{
		printer.emit("/**\n");
		writeDocCommentBody(printer, message, options, kdoc);
		if (kdoc)
		{
			printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
					" * Protobuf enum `$fullname$`\n" + " */\n");
		}
		else
		{
			printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
					" * Protobuf enum {@code $fullname$}\n" + " */\n");
		}
	}

	public static void writeMessageDocComment(
			Printer printer, ServiceDescriptor message, Options options, boolean kdoc)
	{
		printer.emit("/**\n");
		writeDocCommentBody(printer, message, options, kdoc);
		if (kdoc)
		{
			printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
					" * Protobuf service `$fullname$`\n" + " */\n");
		}
		else
		{
			printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
					" * Protobuf service {@code $fullname$}\n" + " */\n");
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
			AccessorType type,
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
        case BYTES_GETTER:
            printer.emit(Map.of("name", name), " * @return The bytes for $name$.\n");
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
		case MAP_ENTRY_ADDER:
			printer.emit(Map.of("name", name), " * @param key The key of the $name$ to add.\n");
			printer.emit(Map.of("name", name), " * @param value The value of the $name$ to add.\n");
			break;
		case MAP_MULTI_ADDER:
			printer.emit(Map.of("name", name), " * @param values The $name$ to add.\n");
			break;
		case MAP_ENTRY_REMOVER:
			printer.emit(Map.of("name", name), " * @param key The key of the $name$ to remove.\n");
			break;
		}
		if (builder)
		{
			printer.emit(" * @return This builder for chaining.\n");
		}
		printer.emit(" */\n");
	}

	public static void writeFieldAccessorDocComment(
			Printer printer,
			FieldDescriptor field,
			AccessorType type,
			Options options,
			boolean builder)
	{
		writeFieldAccessorDocComment(printer, field, type, options, builder, false, false);
	}

	public static void writeFieldAccessorDocComment(
			Printer printer,
			FieldDescriptor field,
			AccessorType type,
			Options options)
	{
		writeFieldAccessorDocComment(printer, field, type, options, false, false, false);
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

	public static void writeEnumValueDocComment(Printer printer, EnumValueDescriptor value, Context context)
	{
		// TODO implement this!
	}
}