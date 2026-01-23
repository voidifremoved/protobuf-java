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
				out.print(" * ```\n");
			}
			else
			{
				out.print(" * <pre>\n");
			}
			for (int i = 0; i < last; i++)
			{
				out.print(" * " + lines[i] + "\n");
			}
			if (kdoc)
			{
				out.print(" * ```\n");
			}
			else
			{
				out.print(" * </pre>\n");
			}
			out.print(" *\n");
		}
	}

	private static void findLocationAndWriteComment(
			PrintWriter out, FileDescriptor file, List<Integer> path, Options options, boolean kdoc)
	{
		SourceCodeInfo sourceCodeInfo = file.toProto().getSourceCodeInfo();
		for (SourceCodeInfo.Location location : sourceCodeInfo.getLocationList())
		{
			if (location.getPathList().equals(path))
			{
				writeDocCommentBodyForLocation(out, location, options, kdoc);
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
		String fieldComment = firstLineOf(field.toProto().toString());
		if (options.stripNonfunctionalCodegen)
		{
			fieldComment = field.getName();
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
		findLocationAndWriteComment(out, field.getFile(), getPath(field), options, kdoc);
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
		findLocationAndWriteComment(out, field.getFile(), getPath(field), options, kdoc);
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
		out.print("/**\n");
		findLocationAndWriteComment(out, enum_.getFile(), getPath(enum_), options, kdoc);
		if (kdoc)
		{
			out.print(" * Protobuf enum `" + escapeKdoc(enum_.getFullName()) + "`\n");
		}
		else
		{
			out.print(" * Protobuf enum {@code " + escapeJavadoc(enum_.getFullName()) + "}\n");
		}
		out.print(" */\n");
	}

	public static void writeEnumValueDocComment(
			PrintWriter out, EnumValueDescriptor value, Options options)
	{
		out.print("/**\n");
		findLocationAndWriteComment(out, value.getFile(), getPath(value), options, false);
		// Match C++ format: <code>NAME = NUMBER;</code>
		out.print(" * <code>" + escapeJavadoc(value.getName() + " = " + value.getNumber() + ";") + "</code>\n");
		out.print(" */\n");
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
