package com.rubberjam.protobuf.compiler.java.proto3.size;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;

import java.io.PrintWriter;

public class MessageBuilderGenerator
{
	private final Descriptor descriptor;
	private final JavaContext context;
	private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;

	public MessageBuilderGenerator(
			Descriptor descriptor,
			JavaContext context,
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
		
		StringBuilder nameBuilder = new StringBuilder(className);
		Descriptor parent = descriptor.getContainingType();
		while (parent != null)
		{
			nameBuilder.insert(0, parent.getName() + ".");
			parent = parent.getContainingType();
		}
		String fullClassName = outerClassName + "." + nameBuilder.toString();

		com.rubberjam.protobuf.compiler.java.DocComment.writeMessageDocComment(printer, descriptor, context, false, "    ");

		printer.println("    public static final class Builder extends");
		if (descriptor.isExtendable())
		{
			printer.println("        com.google.protobuf.GeneratedMessage.ExtendableBuilder<");
			printer.println("          " + fullClassName + ", Builder> implements");
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

		boolean hasMapFields = false;
		for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
		{
			if (field.isMapField())
			{
				hasMapFields = true;
				break;
			}
		}
		if (hasMapFields)
		{
			printer.println("      @SuppressWarnings({\"rawtypes\"})");
			printer.println("      protected com.google.protobuf.MapFieldReflectionAccessor internalGetMapFieldReflection(");
			printer.println("          int number) {");
			printer.println("        switch (number) {");
			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isMapField())
				{
					printer.println("          case " + field.getNumber() + ":");
					printer.println("            return internalGet" + com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(field.getName(), true) + "();");
				}
			}
			printer.println("          default:");
			printer.println("            throw new RuntimeException(");
			printer.println("                \"Invalid map field number: \" + number);");
			printer.println("        }");
			printer.println("      }");
			printer.println("      @SuppressWarnings({\"rawtypes\"})");
			printer.println("      protected com.google.protobuf.MapFieldReflectionAccessor internalGetMutableMapFieldReflection(");
			printer.println("          int number) {");
			printer.println("        switch (number) {");
			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isMapField())
				{
					printer.println("          case " + field.getNumber() + ":");
					printer.println("            return internalGetMutable" + com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(field.getName(), true) + "();");
				}
			}
			printer.println("          default:");
			printer.println("            throw new RuntimeException(");
			printer.println("                \"Invalid map field number: \" + number);");
			printer.println("        }");
			printer.println("      }");
		}
		printer.println("      @java.lang.Override");
		printer.println("      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("          internalGetFieldAccessorTable() {");
		printer.println("        return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("            .ensureFieldAccessorsInitialized(");
		printer.println("                " + fullClassName + ".class, " + fullClassName + ".Builder.class);");
		printer.println("      }");
		printer.println();

		// Check if we need to generate initialization code
		java.io.StringWriter initCodeWriter = new java.io.StringWriter();
		java.io.PrintWriter initCodePrinter = new java.io.PrintWriter(initCodeWriter);
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateFieldBuilderInitializationCode(initCodePrinter);
		}
		String initCode = initCodeWriter.toString();

		printer.println("      // Construct using " + fullClassName + ".newBuilder()");
		printer.println("      private Builder() {");
		if (!initCode.isEmpty()) {
			printer.println("        maybeForceBuilderInitialization();");
		} else {
			printer.println();
		}
		printer.println("      }");
		printer.println();

		printer.println("      private Builder(");
		printer.println("          com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("        super(parent);");
		if (!initCode.isEmpty()) {
			printer.println("        maybeForceBuilderInitialization();");
		} else {
			printer.println();
		}
		printer.println("      }");

		if (!initCode.isEmpty()) {
			printer.println("      private void maybeForceBuilderInitialization() {");
			printer.println("        if (com.google.protobuf.GeneratedMessage");
			printer.println("                .alwaysUseFieldBuilders) {");
			printer.print(initCode);
			printer.println("        }");
			printer.println("      }");
		}

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
		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (context.isSyntheticOneof(oneof)) continue;
			String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(oneof.getName(), false);
			printer.println("        " + oneofName + "Case_ = 0;");
			printer.println("        " + oneofName + "_ = null;");
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
		boolean hasRepeated = false;
		for (ImmutableFieldGenerator gen : fieldGenerators.getFieldGenerators()) {
			if (gen.getDescriptor().isRepeated()
					&& gen.getDescriptor().getJavaType() == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
					&& !gen.getDescriptor().isMapField()) {
				hasRepeated = true;
				break;
			}
		}
		if (hasRepeated) {
			printer.println("        buildPartialRepeatedFields(result);");
		}
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("        if (" + getBitFieldName(i) + " != 0) { buildPartial" + i + "(result); }");
		}
		boolean hasOneofs = false;
		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (!context.isSyntheticOneof(oneof))
			{
				hasOneofs = true;
				break;
			}
		}
		if (hasOneofs)
		{
			printer.println("        buildPartialOneofs(result);");
		}
		printer.println("        onBuilt();");
		printer.println("        return result;");
		printer.println("      }");
		printer.println();

		if (hasRepeated)
		{
			printer.println("      private void buildPartialRepeatedFields(" + fullClassName + " result) {");
			for (ImmutableFieldGenerator gen : fieldGenerators.getFieldGenerators())
			{
				if (gen.getDescriptor().isRepeated()
						&& gen.getDescriptor().getJavaType() == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
						&& !gen.getDescriptor().isMapField())
				{
					gen.generateBuildingCode(printer);
				}
			}
			printer.println("      }");
			printer.println();
		}

		int fieldIndex = 0;
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("      private void buildPartial" + i + "(" + fullClassName + " result) {");
			printer.println("        int from_" + getBitFieldName(i) + " = " + getBitFieldName(i) + ";");
			// Check if we need to_bitField0_ logic - only needed if result message has bitField0_
			// Repeated fields, maps, and oneof fields don't need bitField0_ in the message class
			boolean resultNeedsBitField = false;
			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isMapField() || field.isRepeated() || field.getContainingOneof() != null)
				{
					continue;
				}
				if (field.hasPresence())
				{
					resultNeedsBitField = true;
					break;
				}
			}
			int end = Math.min(fieldIndex + 32, totalFields);
			boolean toBitFieldDeclared = false;
			for (; fieldIndex < end; fieldIndex++)
			{
				ImmutableFieldGenerator gen = fieldGenerators.getFieldGenerators().get(fieldIndex);
				if (!(gen.getDescriptor().isRepeated()
						&& gen.getDescriptor().getJavaType() == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
						&& !gen.getDescriptor().isMapField()))
				{
					boolean fieldNeedsBitField = !gen.getDescriptor().isMapField() && !gen.getDescriptor().isRepeated()
							&& (gen.getDescriptor().getContainingOneof() == null || context.isSyntheticOneofField(gen.getDescriptor()));
					if (gen.getDescriptor().hasPresence() && fieldNeedsBitField && !toBitFieldDeclared) {
						printer.println("        int to_" + getBitFieldName(i) + " = 0;");
						toBitFieldDeclared = true;
					}
					gen.generateBuildingCode(printer);
				}
			}
			if (toBitFieldDeclared)
			{
				printer.println("        result." + getBitFieldName(i) + " |= to_" + getBitFieldName(i) + ";");
			}
			printer.println("      }");
		}
		if (totalBuilderPieces > 0)
		{
			printer.println();
		}

		if (hasOneofs)
		{
			printer.println("      private void buildPartialOneofs(" + fullClassName + " result) {");
			for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
			{
				if (!context.isSyntheticOneof(oneof))
				{
					String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(oneof.getName(), false);
					printer.println("        result." + oneofName + "Case_ = " + oneofName + "Case_;");
					printer.println("        result." + oneofName + "_ = this." + oneofName + "_;");
					for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
					{
						if (field.getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE
								|| field.getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP)
						{
							String name = com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(field.getName(), false);
							printer.println("        if (" + oneofName + "Case_ == " + field.getNumber() + " &&");
							printer.println("            " + name + "Builder_ != null) {");
							printer.println("          result." + oneofName + "_ = " + name + "Builder_.build();");
							printer.println("        }");
						}
					}
				}
			}
			printer.println("      }");
			printer.println();
		}

		if (context.hasGeneratedMethods(descriptor))
		{
			if (descriptor.isExtendable())
			{
				printer.println("      public <Type> Builder setExtension(");
				printer.println("          com.google.protobuf.GeneratedMessage.GeneratedExtension<");
				printer.println("              " + fullClassName + ", Type> extension,");
				printer.println("          Type value) {");
				printer.println("        return super.setExtension(extension, value);");
				printer.println("      }");
				printer.println("      public <Type> Builder setExtension(");
				printer.println("          com.google.protobuf.GeneratedMessage.GeneratedExtension<");
				printer.println("              " + fullClassName + ", java.util.List<Type>> extension,");
				printer.println("          int index, Type value) {");
				printer.println("        return super.setExtension(extension, index, value);");
				printer.println("      }");
				printer.println("      public <Type> Builder addExtension(");
				printer.println("          com.google.protobuf.GeneratedMessage.GeneratedExtension<");
				printer.println("              " + fullClassName + ", java.util.List<Type>> extension,");
				printer.println("          Type value) {");
				printer.println("        return super.addExtension(extension, value);");
				printer.println("      }");
				printer.println("      public <Type> Builder clearExtension(");
				printer.println("          com.google.protobuf.GeneratedMessage.GeneratedExtension<");
				printer.println("              " + fullClassName + ", Type> extension) {");
				printer.println("        return super.clearExtension(extension);");
				printer.println("      }");
			}

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
				if (fieldGenerator.getDescriptor().getContainingOneof() == null || context.isSyntheticOneofField(fieldGenerator.getDescriptor()))
				{
					fieldGenerator.generateMergingCode(printer);
				}
			}
			for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
			{
				if (context.isSyntheticOneof(oneof)) continue;
				String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(oneof.getName(), false);
				printer.println("        switch (other.get" + com.rubberjam.protobuf.compiler.java.StringUtils.toProperCase(oneofName) + "Case()) {");
				for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
				{
					printer.println("          case " + field.getName().toUpperCase() + ": {");
					fieldGenerators.get(field).generateMergingCode(printer);
					printer.println("            break;");
					printer.println("          }");
				}
				printer.println("          case " + oneofName.toUpperCase() + "_NOT_SET: {");
				printer.println("            break;");
				printer.println("          }");
				printer.println("        }");
			}
			if (descriptor.isExtendable()) {
				printer.println("        this.mergeExtensionFields(other);");
			}
			printer.println("        this.mergeUnknownFields(other.getUnknownFields());");
			printer.println("        onChanged();");
			printer.println("        return this;");
			printer.println("      }");
			printer.println();

			// isInitialized()
			printer.println("      @java.lang.Override");
			printer.println("      public final boolean isInitialized() {");
			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isRequired())
				{
					printer.println("        if (!has" + com.rubberjam.protobuf.compiler.java.StringUtils.capitalizedFieldName(field) + "()) {");
					printer.println("          return false;");
					printer.println("        }");
				}
			}

			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.getJavaType() == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
						&& com.rubberjam.protobuf.compiler.java.Helpers.hasRequiredFields(field.getMessageType()))
				{
					String name = com.rubberjam.protobuf.compiler.java.StringUtils.capitalizedFieldName(field);
					if (field.isRequired())
					{
						printer.println("        if (!get" + name + "().isInitialized()) {");
						printer.println("          return false;");
						printer.println("        }");
					}
					else if (field.isRepeated())
					{
						if (field.isMapField())
						{
							com.google.protobuf.Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");
							String valueType = context.getNameResolver().getImmutableClassName(valueField.getMessageType());
							printer.println("        for (" + valueType + " item : get" + name + "Map().values()) {");
							printer.println("          if (!item.isInitialized()) {");
							printer.println("            return false;");
							printer.println("          }");
							printer.println("        }");
						}
						else
						{
							printer.println("        for (int i = 0; i < get" + name + "Count(); i++) {");
							printer.println("          if (!get" + name + "(i).isInitialized()) {");
							printer.println("            return false;");
							printer.println("          }");
							printer.println("        }");
						}
					}
					else
					{
						printer.println("        if (has" + name + "()) {");
						printer.println("          if (!get" + name + "().isInitialized()) {");
						printer.println("            return false;");
						printer.println("          }");
						printer.println("        }");
					}
				}
			}

			if (descriptor.isExtendable()) {
				printer.println("        if (!extensionsAreInitialized()) {");
				printer.println("          return false;");
				printer.println("        }");
			}
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
				if (field.isPackable())
				{
					int packedTag = (field.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
					printer.println("              case " + packedTag + ": {");
					fieldGenerator.generateBuilderParsingCodeFromPacked(printer);
					printer.println("                break;");
					printer.println("              } // case " + packedTag);
				}
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
		}

		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (context.isSyntheticOneof(oneof)) continue;
			String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils.underscoresToCamelCase(oneof.getName(), false);
			printer.println("      private int " + oneofName + "Case_ = 0;");
			printer.println("      private java.lang.Object " + oneofName + "_;");
			printer.println("      public " + com.rubberjam.protobuf.compiler.java.StringUtils.toProperCase(oneofName) + "Case");
			printer.println("          get" + com.rubberjam.protobuf.compiler.java.StringUtils.toProperCase(oneofName) + "Case() {");
			printer.println("        return " + com.rubberjam.protobuf.compiler.java.StringUtils.toProperCase(oneofName) + "Case.forNumber(");
			printer.println("            " + oneofName + "Case_);");
			printer.println("      }");
			printer.println();
			printer.println("      public Builder clear" + com.rubberjam.protobuf.compiler.java.StringUtils.toProperCase(oneofName) + "() {");
			printer.println("        " + oneofName + "Case_ = 0;");
			printer.println("        " + oneofName + "_ = null;");
			printer.println("        onChanged();");
			printer.println("        return this;");
			printer.println("      }");
			printer.println();
		}

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
