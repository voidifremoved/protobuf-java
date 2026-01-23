// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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
			PrintWriter out, FileDescriptor file, List<Integer> path, Options options, boolean kdoc)
	{
		findLocationAndWriteComment(out, file, path, options, kdoc, "");
	}

	private static void findLocationAndWriteComment(
			PrintWriter out, FileDescriptor file, List<Integer> path, Options options, boolean kdoc, String indentPrefix)
	{
		SourceCodeInfo sourceCodeInfo = file.toProto().getSourceCodeInfo();
		for (SourceCodeInfo.Location location : sourceCodeInfo.getLocationList())
		{
			if (location.getPathList().equals(path))
			{
				writeDocCommentBodyForLocation(out, location, options, kdoc, indentPrefix);
				return;
			}
		}
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
			PrintWriter out, FieldDescriptor field, Options options, boolean kdoc)
	{
		String fieldComment;
		if (options.stripNonfunctionalCodegen)
		{
			fieldComment = field.getName();
		}
		else
		{
			// Build field declaration format: "optional string field1 = 1;"
			// Match C++ DebugString() format
			StringBuilder sb = new StringBuilder();
			
			// Add label (optional/required/repeated) for proto2 or explicitly optional proto3 fields
			boolean isProto3 = !field.getFile().toProto().getSyntax().equals("proto2");
			FieldDescriptorProto.Label label = field.toProto().getLabel();
			if (!isProto3 || field.isOptional())
			{
				switch (label)
				{
					case LABEL_REQUIRED:
						sb.append("required ");
						break;
					case LABEL_OPTIONAL:
						sb.append("optional ");
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
			
			// Add type name
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
					case DOUBLE: sb.append("double"); break;
					case FLOAT: sb.append("float"); break;
					case INT64: sb.append("int64"); break;
					case UINT64: sb.append("uint64"); break;
					case INT32: sb.append("int32"); break;
					case FIXED64: sb.append("fixed64"); break;
					case FIXED32: sb.append("fixed32"); break;
					case BOOL: sb.append("bool"); break;
					case STRING: sb.append("string"); break;
					case GROUP: sb.append("group"); break;
					case MESSAGE: sb.append("message"); break;
					case BYTES: sb.append("bytes"); break;
					case UINT32: sb.append("uint32"); break;
					case ENUM: sb.append("enum"); break;
					case SFIXED32: sb.append("sfixed32"); break;
					case SFIXED64: sb.append("sfixed64"); break;
					case SINT32: sb.append("sint32"); break;
					case SINT64: sb.append("sint64"); break;
				}
			}
			
			sb.append(" ");
			sb.append(field.getName());
			sb.append(" = ");
			sb.append(field.getNumber());
			
			if (field.hasDefaultValue())
			{
				sb.append(" [default = ");
				sb.append(field.getDefaultValue());
				sb.append("]");
			}
			
			sb.append(";");
			
			fieldComment = sb.toString();
		}
		
		if (kdoc)
		{
			out.print(" * `" + escapeKdoc(fieldComment) + "`\n");
		}
		else
		{
			out.print(" * <code>" + escapeJavadoc(fieldComment) + "</code>\n");
		}
	}

	public static void writeMessageDocComment(
			PrintWriter out, Descriptor message, Options options, boolean kdoc)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, message.getFile(), getPath(message), options, kdoc);
		if (kdoc)
		{
			out.print(" * Protobuf type `" + escapeKdoc(message.getFullName()) + "`\n");
		}
		else
		{
			out.print(" * Protobuf type {@code " + escapeJavadoc(message.getFullName()) + "}\n");
		}
		out.print(" */\n");
	}

	public static void writeFieldDocComment(
			PrintWriter out, FieldDescriptor field, Options options, boolean kdoc)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, field.getFile(), getPath(field), options, kdoc);
		writeDebugString(out, field, options, kdoc);
		out.print(" */\n");
	}

	public static void writeFieldAccessorDocComment(
			PrintWriter out,
			FieldDescriptor field,
			FieldAccessorType type,
			Options options,
			boolean builder,
			boolean kdoc,
			boolean isPrivate)
	{
		String camelcaseName = StringUtils.camelCaseFieldName(field);
		out.print("/**\n");
		// Use empty indent prefix since Helpers.writeDocComment already handles indentation
		findLocationAndWriteComment(out, field.getFile(), getPath(field), options, kdoc, "");
		writeDebugString(out, field, options, kdoc);
		if (!kdoc && !isPrivate && field.getOptions().getDeprecated())
		{
			out.print(" * @deprecated\n");
		}
		switch (type)
		{
		case HAZZER:
			out.print(" * @return Whether the " + camelcaseName + " field is set.\n");
			break;
		case GETTER:
			out.print(" * @return The " + camelcaseName + ".\n");
			break;
		case SETTER:
			out.print(" * @param value The " + camelcaseName + " to set.\n");
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
			Options options,
			boolean builder,
			boolean kdoc,
			boolean isPrivate)
	{
		String camelcaseName = StringUtils.camelCaseFieldName(field);
		out.print("/**\n");
		// Use empty indent prefix since Helpers.writeDocComment already handles indentation
		findLocationAndWriteComment(out, field.getFile(), getPath(field), options, kdoc, "");
		writeDebugString(out, field, options, kdoc);
		if (!kdoc && !isPrivate && field.getOptions().getDeprecated())
		{
			out.print(" * @deprecated\n");
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
			PrintWriter out, EnumDescriptor enum_, Options options, boolean kdoc)
	{
		writeEnumDocComment(out, enum_, options, kdoc, "");
	}

	public static void writeEnumDocComment(
			PrintWriter out, EnumDescriptor enum_, Options options, boolean kdoc, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, enum_.getFile(), getPath(enum_), options, kdoc, indentPrefix);
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
			PrintWriter out, EnumValueDescriptor value, Options options)
	{
		writeEnumValueDocComment(out, value, options, "");
	}

	public static void writeEnumValueDocComment(
			PrintWriter out, EnumValueDescriptor value, Options options, String indentPrefix)
	{
		out.print(indentPrefix + "/**\n");
		findLocationAndWriteComment(out, value.getFile(), getPath(value), options, false, indentPrefix);
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
			PrintWriter out, ServiceDescriptor service, Options options)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, service.getFile(), getPath(service), options, false);
		out.print(" * Protobuf service {@code " + escapeJavadoc(service.getFullName()) + "}\n");
		out.print(" */\n");
	}

	public static void writeMethodDocComment(
			PrintWriter out, MethodDescriptor method, Options options)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, method.getFile(), getPath(method), options, false);
		out.print(" * <code>" + escapeJavadoc(firstLineOf(method.toProto().toString())) + "</code>\n");
		out.print(" */\n");
	}
}
