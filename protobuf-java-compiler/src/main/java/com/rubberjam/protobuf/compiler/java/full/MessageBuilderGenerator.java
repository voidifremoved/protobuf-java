package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;

import java.io.PrintWriter;

public class MessageBuilderGenerator
{
	private final Descriptor descriptor;
	private final Context context;
	private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;

	public MessageBuilderGenerator(
			Descriptor descriptor,
			Context context,
			FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators)
	{
		this.descriptor = descriptor;
		this.context = context;
		this.fieldGenerators = fieldGenerators;
	}

	public void generate(PrintWriter printer)
	{
		String className = descriptor.getName();
		int totalBuilderBits = 0;
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			totalBuilderBits += fieldGenerator.getNumBitsForBuilder();
		}
		int totalBuilderInts = (totalBuilderBits + 31) / 32;
		int totalFields = fieldGenerators.getFieldGenerators().size();
		int totalBuilderPieces = (totalFields + 31) / 32;

		// Builder Class
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		String packageName = context.getNameResolver().getFileJavaPackage(descriptor.getFile());
		String fileClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
		String outerClassName = packageName.isEmpty() ? fileClassName : packageName + "." + fileClassName;
		String fullClassName = outerClassName + "." + className;
		
		com.rubberjam.protobuf.compiler.java.DocComment.writeMessageDocComment(printer, descriptor, context.getOptions(), false, "    ");

		printer.println("    public static final class Builder extends");
		if (descriptor.isExtendable())
		{
			printer.println("        com.google.protobuf.GeneratedMessage.ExtendableBuilder<" + className + ", Builder> implements");
		}
		else
		{
			printer.println("        com.google.protobuf.GeneratedMessage.Builder<Builder> implements");
		}

		printer.println("        // @@protoc_insertion_point(builder_implements:" + descriptor.getFullName() + ")");
		printer.println("        " + fullClassName + "OrBuilder {");

		printer.println("      public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("          getDescriptor() {");
		printer.println("        return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("      }");
		printer.println();

		printer.println("      @java.lang.Override");
		printer.println("      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("          internalGetFieldAccessorTable() {");
		printer.println("        return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("            .ensureFieldAccessorsInitialized(");
		printer.println("                " + fullClassName + ".class, " + fullClassName + ".Builder.class);");
		printer.println("      }");
		printer.println();

		printer.println("      // Construct using " + fullClassName + ".newBuilder()");
		printer.println("      private Builder() {");
		printer.println();
		printer.println("      }");
		printer.println();

		printer.println("      private Builder(");
		printer.println("          com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("        super(parent);");
		printer.println();
		printer.println("      }");

		printer.println("      @java.lang.Override");
		printer.println("      public Builder clear() {");
		printer.println("        super.clear();");
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("        " + getBitFieldName(i) + " = 0;");
		}
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderClearCode(printer);
		}
		printer.println("        return this;");
		printer.println("      }");
		printer.println();

		printer.println("      @java.lang.Override");
		printer.println("      public com.google.protobuf.Descriptors.Descriptor");
		printer.println("          getDescriptorForType() {");
		printer.println("        return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("      }");
		printer.println();

		printer.println("      @java.lang.Override");
		printer.println("      public " + fullClassName + " getDefaultInstanceForType() {");
		printer.println("        return " + fullClassName + ".getDefaultInstance();");
		printer.println("      }");
		printer.println();

		printer.println("      @java.lang.Override");
		printer.println("      public " + fullClassName + " build() {");
		printer.println("        " + fullClassName + " result = buildPartial();");
		printer.println("        if (!result.isInitialized()) {");
		printer.println("          throw newUninitializedMessageException(result);");
		printer.println("        }");
		printer.println("        return result;");
		printer.println("      }");
		printer.println();

		printer.println("      @java.lang.Override");
		printer.println("      public " + fullClassName + " buildPartial() {");
		printer.println("        " + fullClassName + " result = new " + fullClassName + "(this);");
		boolean hasRepeatedFields = false;
		for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
		{
			if (field.isRepeated())
			{
				hasRepeatedFields = true;
				break;
			}
		}
		if (hasRepeatedFields)
		{
			printer.println("        buildPartialRepeatedFields(result);");
		}
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("        if (" + getBitFieldName(i) + " != 0) { buildPartial" + i + "(result); }");
		}
		printer.println("        onBuilt();");
		printer.println("        return result;");
		printer.println("      }");
		printer.println();

		if (hasRepeatedFields)
		{
			printer.println("      private void buildPartialRepeatedFields(" + fullClassName + " result) {");
			// TODO: repeated fields building
			printer.println("      }");
			printer.println();
		}

		int fieldIndex = 0;
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("      private void buildPartial" + i + "(" + fullClassName + " result) {");
			printer.println("        int from_" + getBitFieldName(i) + " = " + getBitFieldName(i) + ";");
			printer.println("        int to_" + getBitFieldName(i) + " = 0;");
			int end = Math.min(fieldIndex + 32, totalFields);
			for (; fieldIndex < end; fieldIndex++)
			{
				fieldGenerators.getFieldGenerators().get(fieldIndex).generateBuildingCode(printer);
			}
			printer.println("        result." + getBitFieldName(i) + " |= to_" + getBitFieldName(i) + ";");
			printer.println("      }");
		}
		printer.println();

		// mergeFrom(Message other)
		printer.println("      @java.lang.Override");
		printer.println("      public Builder mergeFrom(com.google.protobuf.Message other) {");
		printer.println("        if (other instanceof " + fullClassName + ") {");
		printer.println("          return mergeFrom((" + fullClassName + ")other);");
		printer.println("        } else {");
		printer.println("          super.mergeFrom(other);");
		printer.println("          return this;");
		printer.println("        }");
		printer.println("      }");
		printer.println();

		// mergeFrom(ClassName other)
		printer.println("      public Builder mergeFrom(" + fullClassName + " other) {");
		printer.println("        if (other == " + fullClassName + ".getDefaultInstance()) return this;");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			if (fieldGenerator.getDescriptor().getContainingOneof() == null)
			{
				fieldGenerator.generateMergingCode(printer);
			}
		}
		printer.println("        this.mergeUnknownFields(other.getUnknownFields());");
		printer.println("        onChanged();");
		printer.println("        return this;");
		printer.println("      }");
		printer.println();

		// isInitialized()
		printer.println("      @java.lang.Override");
		printer.println("      public final boolean isInitialized() {");
		printer.println("        return true;");
		printer.println("      }");
		printer.println();

		// mergeFrom(CodedInputStream, ExtensionRegistryLite)
		printer.println("      @java.lang.Override");
		printer.println("      public Builder mergeFrom(");
		printer.println("          com.google.protobuf.CodedInputStream input,");
		printer.println("          com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("          throws java.io.IOException {");
		printer.println("        if (extensionRegistry == null) {");
		printer.println("          throw new java.lang.NullPointerException();");
		printer.println("        }");
		printer.println("        try {");
		printer.println("          boolean done = false;");
		printer.println("          while (!done) {");
		printer.println("            int tag = input.readTag();");
		printer.println("            switch (tag) {");
		printer.println("              case 0:");
		printer.println("                done = true;");
		printer.println("                break;");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getSortedFieldGenerators())
		{
			com.google.protobuf.Descriptors.FieldDescriptor field = fieldGenerator.getDescriptor();
			int tag = com.rubberjam.protobuf.compiler.java.Helpers.getTag(field);
			printer.println("              case " + tag + ": {");
			fieldGenerator.generateBuilderParsingCode(printer);
			printer.println("                break;");
			printer.println("              } // case " + tag);
		}
		printer.println("              default: {");
		printer.println("                if (!super.parseUnknownField(input, extensionRegistry, tag)) {");
		printer.println("                  done = true; // was an endgroup tag");
		printer.println("                }");
		printer.println("                break;");
		printer.println("              } // default:");
		printer.println("            } // switch (tag)");
		printer.println("          } // while (!done)");
		printer.println("        } catch (com.google.protobuf.InvalidProtocolBufferException e) {");
		printer.println("          throw e.unwrapIOException();");
		printer.println("        } finally {");
		printer.println("          onChanged();");
		printer.println("        } // finally");
		printer.println("        return this;");
		printer.println("      }");

		// bitField0_ for builder
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("      private int " + getBitFieldName(i) + ";");
		}
		printer.println();

		// Fields for builder
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderMembers(printer);
			printer.println();
		}

		printer.println("      // @@protoc_insertion_point(builder_scope:" + descriptor.getFullName() + ")");
		printer.println("    }"); // End Builder
	}

	private static String getBitFieldName(int index)
	{
		return "bitField" + index + "_";
	}

	private String getUniqueFileScopeIdentifier(Descriptor descriptor)
	{
		return "static_" + descriptor.getFullName().replace('.', '_');
	}
}
