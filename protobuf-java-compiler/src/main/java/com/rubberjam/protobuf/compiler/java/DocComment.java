// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

/** A utility class for generating documentation comments. */
public final class DocComment
{

	private DocComment()
	{
	}

	public static String escapeJavadoc(String input)
	{
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		char prev = '*';
		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			switch (c)
			{
			case '*':
				if (prev == '/')
				{
					out.print("&#42;");
				}
				else
				{
					out.print(c);
				}
				break;
			case '/':
				if (prev == '*')
				{
					out.print("&#47;");
				}
				else
				{
					out.print(c);
				}
				break;
			case '@':
				out.print("&#64;");
				break;
			case '<':
				out.print("&lt;");
				break;
			case '>':
				out.print("&gt;");
				break;
			case '&':
				out.print("&amp;");
				break;
			case '\\':
				out.print("&#92;");
				break;
			default:
				out.print(c);
				break;
			}
			prev = c;
		}
		return writer.toString();
	}

	private static String escapeKdoc(String input)
	{
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		char prev = 'a';
		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			switch (c)
			{
			case '*':
				if (prev == '/')
				{
					out.print("&#42;");
				}
				else
				{
					out.print(c);
				}
				break;
			case '/':
				if (prev == '*')
				{
					out.print("&#47;");
				}
				else
				{
					out.print(c);
				}
				break;
			default:
				out.print(c);
				break;
			}
			prev = c;
		}
		return writer.toString();
	}

	private static void writeDocCommentBodyForLocation(
			PrintWriter out, SourceCodeInfo.Location location, Options options, boolean kdoc)
	{
		writeDocCommentBodyForLocation(out, location, options, kdoc, "");
	}

	private static void writeDocCommentBodyForLocation(
			PrintWriter out, SourceCodeInfo.Location location, Options options, boolean kdoc, String indentPrefix)
	{
		if (options.stripNonfunctionalCodegen)
		{
			return;
		}
		String comments = !location.getLeadingComments().isEmpty()
				? location.getLeadingComments()
				: location.getTrailingComments();
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
			String[] lines = comments.split("\n", -1);
			int last = lines.length;
			while (last > 0 && lines[last - 1].isEmpty())
			{
				last--;
			}
			if (kdoc)
			{
				out.print(indentPrefix + " * ```\n");
			}
			else
			{
				out.print(indentPrefix + " * <pre>\n");
			}
			for (int i = 0; i < last; i++)
			{
				out.print(indentPrefix + " * " + lines[i] + "\n");
			}
			if (kdoc)
			{
				out.print(indentPrefix + " * ```\n");
			}
			else
			{
				out.print(indentPrefix + " * </pre>\n");
			}
			out.print(indentPrefix + " *\n");
		}
	}

	private static void findLocationAndWriteComment(
			PrintWriter out, FileDescriptor file, List<Integer> path, Context context, boolean kdoc)
	{
		findLocationAndWriteComment(out, file, path, context, kdoc, "");
	}

	private static void findLocationAndWriteComment(
			PrintWriter out, FileDescriptor file, List<Integer> path, Context context, boolean kdoc, String indentPrefix)
	{
		SourceCodeInfo.Location location = getLocation(file, path, context);
		if (location != null)
		{
			writeDocCommentBodyForLocation(out, location, context.getOptions(), kdoc, indentPrefix);
		}
	}

	private static void writeDeprecatedJavadoc(
			PrintWriter out, FieldDescriptor field, Context context)
	{
		out.print(" * @deprecated " + field.getFullName() + " is deprecated.\n");
		SourceCodeInfo.Location location = getLocation(field.getFile(), getPath(field), context);
		if (location != null && location.getSpanCount() > 0)
		{
			out.print(" *     See " + field.getFile().getName() + ";l=" + location.getSpan(0) + "\n");
		}
	}

	private static SourceCodeInfo.Location getLocation(
			FileDescriptor file, List<Integer> path, Context context)
	{
		SourceCodeInfo sourceCodeInfo = (context.getSourceProto() != null)
				? context.getSourceProto().getSourceCodeInfo()
				: file.toProto().getSourceCodeInfo();
		for (SourceCodeInfo.Location location : sourceCodeInfo.getLocationList())
		{
			if (location.getPathList().equals(path))
			{
				return location;
			}
		}
		return null;
	}

	private static List<Integer> getPath(Descriptor descriptor)
	{
		List<Integer> path;
		if (descriptor.getContainingType() == null)
		{
			path = new ArrayList<>();
			path.add(FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER);
		}
		else
		{
			path = getPath(descriptor.getContainingType());
			path.add(DescriptorProto.NESTED_TYPE_FIELD_NUMBER);
		}
		path.add(descriptor.getIndex());
		return path;
	}

	private static List<Integer> getPath(FieldDescriptor descriptor)
	{
		List<Integer> path = getPath(descriptor.getContainingType());
		path.add(DescriptorProto.FIELD_FIELD_NUMBER);
		path.add(descriptor.getIndex());
		return path;
	}

	private static List<Integer> getPath(EnumDescriptor descriptor)
	{
		List<Integer> path;
		if (descriptor.getContainingType() == null)
		{
			path = new ArrayList<>();
			path.add(FileDescriptorProto.ENUM_TYPE_FIELD_NUMBER);
		}
		else
		{
			path = getPath(descriptor.getContainingType());
			path.add(DescriptorProto.ENUM_TYPE_FIELD_NUMBER);
		}
		path.add(descriptor.getIndex());
		return path;
	}

	private static List<Integer> getPath(EnumValueDescriptor descriptor)
	{
		List<Integer> path = getPath(descriptor.getType());
		path.add(EnumDescriptorProto.VALUE_FIELD_NUMBER);
		path.add(descriptor.getIndex());
		return path;
	}

	private static List<Integer> getPath(ServiceDescriptor descriptor)
	{
		List<Integer> path = new ArrayList<>();
		path.add(FileDescriptorProto.SERVICE_FIELD_NUMBER);
		path.add(descriptor.getIndex());
		return path;
	}

	private static List<Integer> getPath(MethodDescriptor descriptor)
	{
		List<Integer> path = getPath(descriptor.getService());
		path.add(ServiceDescriptorProto.METHOD_FIELD_NUMBER);
		path.add(descriptor.getIndex());
		return path;
	}

	private static String firstLineOf(String value)
	{
		int pos = value.indexOf('\n');
		if (pos != -1)
		{
			value = value.substring(0, pos);
		}
		if (!value.isEmpty() && value.charAt(value.length() - 1) == '{')
		{
			value += " ... }";
		}
		return value;
	}

	private static void writeDebugString(
			PrintWriter out, FieldDescriptor field, Context context, boolean kdoc, String indentPrefix)
	{
		String fieldComment;
		if (context.getOptions().stripNonfunctionalCodegen)
		{
			fieldComment = field.getName();
		}
		else
		{
			// Build field declaration format: "optional string field1 = 1;"
			// Match C++ DebugString() format
			StringBuilder sb = new StringBuilder();

			if (field.isExtension())
			{
				sb.append("extend .");
				sb.append(field.getContainingType().getFullName());
				sb.append(" { ... }");
			}
			else if (field.isMapField())
			{
				Descriptor messageType = field.getMessageType();
				FieldDescriptor key = messageType.findFieldByName("key");
				FieldDescriptor value = messageType.findFieldByName("value");
				sb.append("map<");
				appendFieldType(sb, key);
				sb.append(", ");
				appendFieldType(sb, value);
				sb.append(">");
			}
			else
			{
				// Add label (optional/required/repeated) for proto2 or
				// explicitly optional proto3 fields
				boolean isProto3 = !field.getFile().toProto().getSyntax().equals("proto2");
				FieldDescriptorProto.Label label = field.toProto().getLabel();
				// Oneof fields do not have labels (except synthetic oneofs for
				// proto3 optional)
				boolean isSyntheticOneof = field.toProto().hasProto3Optional() && field.toProto().getProto3Optional();
				boolean isRealOneof = field.getContainingOneof() != null && !isSyntheticOneof;

				if (!isRealOneof && (!isProto3 || field.isOptional()))
				{
					switch (label)
					{
					case LABEL_REQUIRED:
						sb.append("required ");
						break;
					case LABEL_OPTIONAL:
						if (!isProto3 || isSyntheticOneof)
						{
							sb.append("optional ");
						}
						break;
					case LABEL_REPEATED:
						sb.append("repeated ");
						break;
					}
				}
				else if (field.isRepeated())
				{
					sb.append("repeated ");
				}

				if (field.getType() == FieldDescriptor.Type.GROUP)
				{
					sb.append("group ");
					sb.append(field.getMessageType().getName());
					sb.append(" = ");
					sb.append(field.getNumber());
					sb.append(" { ... }");
				}
				else
				{
					appendFieldType(sb, field);
				}
			}

			if (!field.isExtension() && field.getType() != FieldDescriptor.Type.GROUP)
			{
				sb.append(" ");
				sb.append(field.getName());
				sb.append(" = ");
				sb.append(field.getNumber());

				java.util.List<String> fieldOptions = new java.util.ArrayList<>();
				if (field.hasDefaultValue())
				{
					fieldOptions.add("default = " + getDefaultValueString(field, context));
				}
				if (field.toProto().getOptions().hasPacked())
				{
					fieldOptions.add("packed = " + field.toProto().getOptions().getPacked());
				}
				if (field.toProto().getOptions().getDeprecated())
				{
					fieldOptions.add("deprecated = true");
				}

				if (!fieldOptions.isEmpty())
				{
					sb.append(" [");
					sb.append(String.join(", ", fieldOptions));
					sb.append("]");
				}

				sb.append(";");
			}

			fieldComment = sb.toString();
		}

		if (kdoc)
		{
			out.print(indentPrefix + " * `" + escapeKdoc(fieldComment) + "`\n");
		}
		else
		{
			out.print(indentPrefix + " * <code>" + escapeJavadoc(fieldComment) + "</code>\n");
		}
	}

	private static void appendFieldType(StringBuilder sb, FieldDescriptor field)
	{
		FieldDescriptor.Type type = field.getType();
		if (type == FieldDescriptor.Type.MESSAGE || type == FieldDescriptor.Type.GROUP)
		{
			sb.append(".").append(field.getMessageType().getFullName());
		}
		else if (type == FieldDescriptor.Type.ENUM)
		{
			sb.append(".").append(field.getEnumType().getFullName());
		}
		else
		{
			// Primitive type
			switch (type)
			{
			case DOUBLE:
				sb.append("double");
				break;
			case FLOAT:
				sb.append("float");
				break;
			case INT64:
				sb.append("int64");
				break;
			case UINT64:
				sb.append("uint64");
				break;
			case INT32:
				sb.append("int32");
				break;
			case FIXED64:
				sb.append("fixed64");
				break;
			case FIXED32:
				sb.append("fixed32");
				break;
			case BOOL:
				sb.append("bool");
				break;
			case STRING:
				sb.append("string");
				break;
			case GROUP:
				sb.append("group");
				break;
			case MESSAGE:
				sb.append("message");
				break;
			case BYTES:
				sb.append("bytes");
				break;
			case UINT32:
				sb.append("uint32");
				break;
			case ENUM:
				sb.append("enum");
				break;
			case SFIXED32:
				sb.append("sfixed32");
				break;
			case SFIXED64:
				sb.append("sfixed64");
				break;
			case SINT32:
				sb.append("sint32");
				break;
			case SINT64:
				sb.append("sint64");
				break;
			}
		}
	}

	protected static Object getDefaultValueString(FieldDescriptor field, Context context)
	{
		Object defaultValue = field.getDefaultValue();
		if (defaultValue instanceof String)
		{
			return "\"" + defaultValue + "\"";
		}
		if (field.getType() == FieldDescriptor.Type.FLOAT || field.getType() == FieldDescriptor.Type.DOUBLE)
		{
			// Use the string representation from the proto definition to
			// preserve formatting
			// and avoid precision issues with float/double conversion.
			FieldDescriptorProto proto = getFieldDescriptorProto(field, context);
			if (proto != null && proto.hasDefaultValue())
			{
				return proto.getDefaultValue();
			}
		}
		if (field.getType() == Type.UINT32)
		{
			return Integer.toUnsignedString((Integer) defaultValue);
		}
		if (field.getType() == Type.UINT64)
		{
			return Long.toUnsignedString((Long) defaultValue);
		}
		return defaultValue;
	}

	private static FieldDescriptorProto getFieldDescriptorProto(FieldDescriptor field, Context context)
	{
		FileDescriptorProto fileProto = context.getSourceProto();
		if (fileProto == null)
		{
			return field.toProto();
		}

		if (!field.getFile().getName().equals(fileProto.getName()))
		{
			// Field is imported, cannot get original source proto.
			return field.toProto();
		}

		Descriptor message = field.getContainingType();
		DescriptorProto messageProto = null;

		java.util.Deque<Integer> indices = new java.util.ArrayDeque<>();
		Descriptor current = message;
		while (current != null)
		{
			indices.push(current.getIndex());
			current = current.getContainingType();
		}

		if (indices.isEmpty())
		{
			// Should not happen for a field in a message
			return field.toProto();
		}

		// Root message
		int rootIndex = indices.pop();
		if (rootIndex >= fileProto.getMessageTypeCount()) return field.toProto();
		messageProto = fileProto.getMessageType(rootIndex);

		while (!indices.isEmpty())
		{
			int index = indices.pop();
			if (index >= messageProto.getNestedTypeCount()) return field.toProto();
			messageProto = messageProto.getNestedType(index);
		}

		if (field.getIndex() >= messageProto.getFieldCount()) return field.toProto();
		return messageProto.getField(field.getIndex());
	}

	public static void writeMessageDocComment(
			PrintWriter out, Descriptor message, Context context, boolean kdoc)
	{
		writeMessageDocComment(out, message, context, kdoc, "");
	}

	public static void writeMessageDocComment(
			PrintWriter out, Descriptor message, Context context, boolean kdoc, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, message.getFile(), getPath(message), context, kdoc, indentPrefix);
		if (kdoc)
		{
			out.print(indentPrefix + " * Protobuf type `" + escapeKdoc(message.getFullName()) + "`\n");
		}
		else
		{
			out.print(indentPrefix + " * Protobuf type {@code " + escapeJavadoc(message.getFullName()) + "}\n");
		}
		out.print(indentPrefix + " */\n");
	}

	public static void writeFieldDocComment(
			PrintWriter out, FieldDescriptor field, Context context, boolean kdoc)
	{
		writeFieldDocComment(out, field, context, kdoc, "");
	}

	public static void writeFieldDocComment(
			PrintWriter out, FieldDescriptor field, Context context, boolean kdoc, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, field.getFile(), getPath(field), context, kdoc, indentPrefix);
		writeDebugString(out, field, context, kdoc, indentPrefix);
		out.print(indentPrefix + " */\n");
	}

	public static void writeFieldAccessorDocComment(
			PrintWriter out,
			FieldDescriptor field,
			FieldAccessorType type,
			Context context,
			boolean builder,
			boolean kdoc,
			boolean isPrivate)
	{
		String camelcaseName = StringUtils.javadocFieldName(field);
		if (field.getType() == FieldDescriptor.Type.GROUP)
		{
			// Groups in Javadoc documentation text use lower-case names
			camelcaseName = camelcaseName.toLowerCase();
		}
		out.print("/**\n");
		// Use empty indent prefix since Helpers.writeDocComment already handles
		// indentation
		findLocationAndWriteComment(out, field.getFile(), getPath(field), context, kdoc, "");
		writeDebugString(out, field, context, kdoc, "");
		if (!kdoc && !isPrivate && field.getOptions().getDeprecated())
		{
			writeDeprecatedJavadoc(out, field, context);
		}
		switch (type)
		{
		case HAZZER:
			out.print(" * @return Whether the " + camelcaseName + " field is set.\n");
			break;
		case GETTER:
			out.print(" * @return The " + camelcaseName + ".\n");
			break;
		case VALUE_GETTER:
			out.print(" * @return The enum numeric value on the wire for " + camelcaseName + ".\n");
			break;
		case SETTER:
			out.print(" * @param value The " + camelcaseName + " to set.\n");
			break;
		case VALUE_SETTER:
			out.print(" * @param value The enum numeric value on the wire for " + camelcaseName + " to set.\n");
			break;
		case CLEARER:
			break;
		case LIST_COUNT:
			out.print(" * @return The count of " + camelcaseName + ".\n");
			break;
		case LIST_GETTER:
			out.print(" * @return A list containing the " + camelcaseName + ".\n");
			break;
		case LIST_INDEXED_GETTER:
			out.print(" * @param index The index of the element to return.\n");
			out.print(" * @return The " + camelcaseName + " at the given index.\n");
			break;
		case LIST_INDEXED_SETTER:
			out.print(" * @param index The index to set the value at.\n");
			out.print(" * @param value The " + camelcaseName + " to set.\n");
			break;
		case LIST_ADDER:
			out.print(" * @param value The " + camelcaseName + " to add.\n");
			break;
		case LIST_MULTI_ADDER:
			out.print(" * @param values The " + camelcaseName + " to add.\n");
			break;
		case LIST_VALUE_GETTER:
			out.print(" * @return A list containing the enum numeric values on the wire for " + camelcaseName + ".\n");
			break;
		case LIST_INDEXED_VALUE_GETTER:
			out.print(" * @param index The index of the value to return.\n");
			out.print(" * @return The enum numeric value on the wire of " + camelcaseName + " at the given index.\n");
			break;
		case LIST_INDEXED_VALUE_SETTER:
			out.print(" * @param index The index to set the value at.\n");
			out.print(" * @param value The enum numeric value on the wire for " + camelcaseName + " to set.\n");
			break;
		case LIST_VALUE_ADDER:
			out.print(" * @param value The enum numeric value on the wire for " + camelcaseName + " to add.\n");
			break;
		case LIST_VALUE_MULTI_ADDER:
			out.print(" * @param values The enum numeric values on the wire for " + camelcaseName + " to add.\n");
			break;
		}
		if (builder)
		{
			out.print(" * @return This builder for chaining.\n");
		}
		out.print(" */\n");
	}

	public static void writeFieldStringBytesAccessorDocComment(
			PrintWriter out,
			FieldDescriptor field,
			FieldAccessorType type,
			Context context,
			boolean builder,
			boolean kdoc,
			boolean isPrivate)
	{
		String camelcaseName = StringUtils.javadocFieldName(field);
		if (field.getType() == FieldDescriptor.Type.GROUP)
		{
			// Groups in Javadoc documentation text use lower-case names
			camelcaseName = camelcaseName.toLowerCase();
		}
		out.print("/**\n");
		// Use empty indent prefix since Helpers.writeDocComment already handles
		// indentation
		findLocationAndWriteComment(out, field.getFile(), getPath(field), context, kdoc, "");
		writeDebugString(out, field, context, kdoc, "");
		if (!kdoc && !isPrivate && field.getOptions().getDeprecated())
		{
			writeDeprecatedJavadoc(out, field, context);
		}
		switch (type)
		{
		case GETTER:
			out.print(" * @return The bytes for " + camelcaseName + ".\n");
			break;
		case SETTER:
			out.print(" * @param value The bytes for " + camelcaseName + " to set.\n");
			break;
		case LIST_GETTER:
			out.print(" * @return A list containing the bytes for " + camelcaseName + ".\n");
			break;
		case LIST_INDEXED_GETTER:
			out.print(" * @param index The index of the value to return.\n");
			out.print(" * @return The bytes of the " + camelcaseName + " at the given index.\n");
			break;
		case LIST_INDEXED_SETTER:
			out.print(" * @param index The index to set the value at.\n");
			out.print(" * @param value The bytes of the " + camelcaseName + " to set.\n");
			break;
		case LIST_ADDER:
			out.print(" * @param value The bytes of the " + camelcaseName + " to add.\n");
			break;
		case LIST_MULTI_ADDER:
			out.print(" * @param values The bytes of the " + camelcaseName + " to add.\n");
			break;
		default:
			break;
		}
		if (builder)
		{
			out.print(" * @return This builder for chaining.\n");
		}
		out.print(" */\n");
	}

	public static void writeEnumDocComment(
			PrintWriter out, EnumDescriptor enum_, Context context, boolean kdoc)
	{
		writeEnumDocComment(out, enum_, context, kdoc, "");
	}

	public static void writeEnumDocComment(
			PrintWriter out, EnumDescriptor enum_, Context context, boolean kdoc, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, enum_.getFile(), getPath(enum_), context, kdoc, indentPrefix);
		if (kdoc)
		{
			out.print(indentPrefix + " * Protobuf enum `" + escapeKdoc(enum_.getFullName()) + "`\n");
		}
		else
		{
			out.print(indentPrefix + " * Protobuf enum {@code " + escapeJavadoc(enum_.getFullName()) + "}\n");
		}
		out.print(indentPrefix + " */\n");
	}

	public static void writeEnumValueDocComment(
			PrintWriter out, EnumValueDescriptor value, Context context)
	{
		writeEnumValueDocComment(out, value, context, "");
	}

	public static void writeEnumValueDocComment(
			PrintWriter out, EnumValueDescriptor value, Context context, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, value.getFile(), getPath(value), context, false, indentPrefix);
		// Match C++ format: <code>NAME = NUMBER;</code>
		StringBuilder sb = new StringBuilder();
		sb.append(value.getName());
		sb.append(" = ");
		sb.append(value.getNumber());
		if (value.getOptions().getDeprecated())
		{
			sb.append(" [deprecated = true]");
		}
		sb.append(";");
		out.print(indentPrefix + " * <code>" + escapeJavadoc(sb.toString()) + "</code>\n");
		out.print(indentPrefix + " */\n");
	}

	public static void writeServiceDocComment(
			PrintWriter out, ServiceDescriptor service, Context context)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, service.getFile(), getPath(service), context, false);
		out.print(" * Protobuf service {@code " + escapeJavadoc(service.getFullName()) + "}\n");
		out.print(" */\n");
	}

	public static void writeMethodDocComment(
			PrintWriter out, MethodDescriptor method, Context context)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, method.getFile(), getPath(method), context, false);
		out.print(" * <code>" + escapeJavadoc(firstLineOf(method.toProto().toString())) + "</code>\n");
		out.print(" */\n");
	}
}
