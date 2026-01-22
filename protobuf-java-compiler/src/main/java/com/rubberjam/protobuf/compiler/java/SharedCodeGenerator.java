package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.GeneratorContext;

import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

public class SharedCodeGenerator
{
	private final FileDescriptor file;
	private final Options options;
	private final ClassNameResolver nameResolver;

	public SharedCodeGenerator(FileDescriptor file, Options options)
	{
		this.file = file;
		this.options = options;
		this.nameResolver = new ClassNameResolver();
	}

	public void generateDescriptors(PrintWriter printer)
	{
		FileDescriptorProto fileProto = file.toProto();
		com.google.protobuf.ByteString byteString = fileProto.toByteString();

		printer.println("    java.lang.String[] descriptorData = {");

		printer.print("      \"");
		int bytesPerLine = 40;
		for (int i = 0; i < byteString.size(); i += bytesPerLine)
		{
			int end = Math.min(i + bytesPerLine, byteString.size());
			com.google.protobuf.ByteString chunk = byteString.substring(i, end);
			String escaped = escapeBytesForJava(chunk);

			if (i > 0)
			{
				printer.println("\" +");
				printer.print("      \"");
			}
			printer.print(escaped);
		}
		printer.println("\"");
		printer.println("    };");

		printer.println("    descriptor = com.google.protobuf.Descriptors.FileDescriptor");
		printer.println("      .internalBuildGeneratedFileFrom(descriptorData,");
		printer.println("        new com.google.protobuf.Descriptors.FileDescriptor[] {");

		for (FileDescriptor dependency : file.getDependencies())
		{
			String dependencyName = nameResolver.getImmutableClassName(dependency);
			printer.println("          " + dependencyName + ".getDescriptor(),");
		}

		printer.println("        });");
	}

	private String escapeBytesForJava(com.google.protobuf.ByteString input)
	{
		StringBuilder builder = new StringBuilder(input.size() * 4);
		for (int i = 0; i < input.size(); i++)
		{
			byte b = input.byteAt(i);
			switch (b)
			{
			case '\b':
				builder.append("\\b");
				break;
			case '\t':
				builder.append("\\t");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\f':
				builder.append("\\f");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\"':
				builder.append("\\\"");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			default:
				if (b >= 0x20 && b < 0x7f)
				{
					builder.append((char) b);
				}
				else
				{
					builder.append(String.format("\\%03o", b & 0xFF));
				}
				break;
			}
		}
		return builder.toString();
	}
}
