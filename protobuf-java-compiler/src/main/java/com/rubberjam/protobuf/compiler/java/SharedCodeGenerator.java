package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.Descriptors.*;

import java.io.PrintWriter;

public class SharedCodeGenerator
{
	private final FileDescriptor file;
	private final FileDescriptorProto sourceProto;
	private final Options options;
	private final ClassNameResolver nameResolver;

	public SharedCodeGenerator(FileDescriptor file, FileDescriptorProto sourceProto, Options options)
	{
		this.file = file;
		this.sourceProto = sourceProto;
		this.options = options;
		this.nameResolver = new ClassNameResolver();
	}

	public void generateDescriptors(PrintWriter printer)
	{
		// Use file.toProto() and clear source code info to match expected output format
		// This ensures we use the resolved descriptor format rather than sourceProto which may have different encoding
		FileDescriptorProto fileProto = file.toProto();
		FileDescriptorProto.Builder fileProtoBuilder = fileProto.toBuilder();
		fileProtoBuilder.clearSourceCodeInfo();
		// Clear syntax field to match expected output format (proto2 is default)
		if ("proto2".equals(fileProtoBuilder.getSyntax()))
		{
			fileProtoBuilder.clearSyntax();
		}

		normalizeDescriptor(fileProtoBuilder);

		// Build the cleaned proto
		fileProto = fileProtoBuilder.build();
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
			case '\t':
				builder.append("\\t");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\'':
				builder.append("\\\'");
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

	private void normalizeDescriptor(FileDescriptorProto.Builder fileBuilder)
	{
		// Fix top-level extensions
		for (int i = 0; i < file.getExtensions().size(); i++)
		{
			fixField(fileBuilder.getExtensionBuilder(i), file.getExtensions().get(i));
		}

		// Fix messages
		for (int i = 0; i < file.getMessageTypes().size(); i++)
		{
			fixMessage(fileBuilder.getMessageTypeBuilder(i), file.getMessageTypes().get(i));
		}

		// Fix services
		for (int i = 0; i < file.getServices().size(); i++)
		{
			fixService(fileBuilder.getServiceBuilder(i), file.getServices().get(i));
		}

		// Clean options
		if (fileBuilder.hasOptions())
		{
			// No-op: Don't strip options. Protobuf preserves explicit options in the descriptor.
		}
	}

	private void fixMessage(DescriptorProto.Builder messageBuilder, Descriptor messageDescriptor)
	{
		for (int i = 0; i < messageDescriptor.getFields().size(); i++)
		{
			fixField(messageBuilder.getFieldBuilder(i), messageDescriptor.getFields().get(i));
		}
		for (int i = 0; i < messageDescriptor.getExtensions().size(); i++)
		{
			fixField(messageBuilder.getExtensionBuilder(i), messageDescriptor.getExtensions().get(i));
		}
		for (int i = 0; i < messageDescriptor.getNestedTypes().size(); i++)
		{
			fixMessage(messageBuilder.getNestedTypeBuilder(i), messageDescriptor.getNestedTypes().get(i));
		}
	}

	private void fixField(FieldDescriptorProto.Builder fieldBuilder, FieldDescriptor fieldDescriptor)
	{
		// Fix type name
		if (fieldDescriptor.getType() == FieldDescriptor.Type.MESSAGE ||
				fieldDescriptor.getType() == FieldDescriptor.Type.GROUP)
		{
			fieldBuilder.setTypeName("." + fieldDescriptor.getMessageType().getFullName());
			fieldBuilder.setType(fieldDescriptor.getType().toProto());
		}
		else if (fieldDescriptor.getType() == FieldDescriptor.Type.ENUM)
		{
			fieldBuilder.setTypeName("." + fieldDescriptor.getEnumType().getFullName());
			fieldBuilder.setType(fieldDescriptor.getType().toProto());
		}

		// Fix extendee
		if (fieldDescriptor.isExtension())
		{
			fieldBuilder.setExtendee("." + fieldDescriptor.getContainingType().getFullName());
		}
	}

	private void fixService(ServiceDescriptorProto.Builder serviceBuilder, ServiceDescriptor serviceDescriptor)
	{
		for (int i = 0; i < serviceDescriptor.getMethods().size(); i++)
		{
			fixMethod(serviceBuilder.getMethodBuilder(i), serviceDescriptor.getMethods().get(i));
		}
	}

	private void fixMethod(MethodDescriptorProto.Builder methodBuilder, MethodDescriptor methodDescriptor)
	{
		methodBuilder.setInputType("." + methodDescriptor.getInputType().getFullName());
		methodBuilder.setOutputType("." + methodDescriptor.getOutputType().getFullName());

		// Check for uninterpreted deprecated option and convert to standard field
		java.util.List<UninterpretedOption> uninterpretedOptions = methodDescriptor.getOptions().getUninterpretedOptionList();
		for (UninterpretedOption option : uninterpretedOptions)
		{
			if (option.getNameCount() == 1 &&
				option.getName(0).getNamePart().equals("deprecated") &&
				option.getIdentifierValue().equals("true"))
			{
				methodBuilder.getOptionsBuilder().setDeprecated(true);
				// Clear all uninterpreted options since we found the one we want
				// (Assuming only one or we want to clean up)
				methodBuilder.getOptionsBuilder().clearUninterpretedOption();
				break;
			}
		}
	}
}
