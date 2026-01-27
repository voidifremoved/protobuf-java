package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;
import com.rubberjam.protobuf.compiler.java.MessageGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;

public class ImmutableMessageGenerator extends MessageGenerator
{
	private final Context context;
	private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;
	private final MessageBuilderGenerator messageBuilderGenerator;

	public ImmutableMessageGenerator(Descriptor descriptor, Context context)
	{
		super(descriptor);
		this.context = context;
		this.fieldGenerators = MakeFieldGens.makeImmutableFieldGenerators(descriptor, context);
		this.messageBuilderGenerator = new MessageBuilderGenerator(descriptor, context, fieldGenerators);
	}

	@Override
	public void generateStaticVariables(PrintWriter printer, int[] bytecodeEstimate)
	{
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		printer.println("  private static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("    internal_" + identifier + "_descriptor;");
		printer.println("  private static final ");
		printer.println("    com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("      internal_" + identifier + "_fieldAccessorTable;");

		for (Descriptor nestedType : descriptor.getNestedTypes())
		{
			new ImmutableMessageGenerator(nestedType, context).generateStaticVariables(printer, bytecodeEstimate);
		}
	}

	@Override
	public int generateStaticVariableInitializers(PrintWriter printer)
	{
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		if (descriptor.getContainingType() == null)
		{
			printer.println("    internal_" + identifier + "_descriptor =");
			printer.println("      getDescriptor().getMessageType(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentIdentifier = getUniqueFileScopeIdentifier(descriptor.getContainingType());
			printer.println("    internal_" + identifier + "_descriptor =");
			printer.println("      internal_" + parentIdentifier + "_descriptor.getNestedType(" + descriptor.getIndex() + ");");
		}
		printer.println("    internal_" + identifier + "_fieldAccessorTable = new");
		printer.println("      com.google.protobuf.GeneratedMessage.FieldAccessorTable(");
		printer.println("        internal_" + identifier + "_descriptor,");
		printer.print("        new java.lang.String[] {");
		for (int i = 0; i < descriptor.getFields().size(); i++)
		{
			String fieldName;
			if (descriptor.getFields().get(i).getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP)
			{
				fieldName = StringUtils.underscoresToCamelCase(
						descriptor.getFields().get(i).getMessageType().getName(), true);
			}
			else
			{
				fieldName = StringUtils.capitalizedFieldName(descriptor.getFields().get(i));
			}
			printer.print(" \"" + fieldName + "\",");
		}
		// Add Oneof field names for oneofs
		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (!context.isSyntheticOneof(oneof))
			{
				printer.print(" \"" + StringUtils.underscoresToCamelCase(oneof.getName(), true) + "\",");
			}
		}
		printer.println(" });");
		

		for (Descriptor nestedType : descriptor.getNestedTypes())
		{
			new ImmutableMessageGenerator(nestedType, context).generateStaticVariableInitializers(printer);
		}
		return 0;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		String className = descriptor.getName();
		String packageName = context.getNameResolver().getFileJavaPackage(descriptor.getFile());
		String fileClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
		String outerClassName = packageName.isEmpty() ? fileClassName : packageName + "." + fileClassName;
		String fullClassName = context.getNameResolver().getImmutableClassName(descriptor);

		// Workaround for nested types if ClassNameResolver fails to resolve nesting
		if (!descriptor.getFile().getOptions().getJavaMultipleFiles())
		{
			String p = descriptor.getFile().getPackage();
			String n = descriptor.getFullName();
			String rel = n;
			if (!p.isEmpty() && n.startsWith(p + "."))
			{
				rel = n.substring(p.length() + 1);
			}
			fullClassName = outerClassName + "." + rel;
		}
		
		// Match C++ WriteMessageDocComment behavior - use DocComment utility with 2-space indent
		com.rubberjam.protobuf.compiler.java.DocComment.writeMessageDocComment(printer, descriptor, context, false, "  ");
		
		printer.println("  public static final class " + className + " extends");
		if (descriptor.isExtendable())
		{
			printer.println("      com.google.protobuf.GeneratedMessage.ExtendableMessage<");
			printer.println("        " + className + "> implements");
		}
		else
		{
			printer.println("      com.google.protobuf.GeneratedMessage implements");
		}
		printer.println("      // @@protoc_insertion_point(message_implements:" + descriptor.getFullName() + ")");
		printer.println("      " + className + "OrBuilder {");
		printer.println("  private static final long serialVersionUID = 0L;");
		printer.println("    static {");
		printer.println("      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(");
		printer.println("        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,");
		printer.println("        /* major= */ 4,");
		printer.println("        /* minor= */ 33,");
		printer.println("        /* patch= */ 4,");
		printer.println("        /* suffix= */ \"\",");
		printer.println("        \"" + className + "\");");
		printer.println("    }");
		printer.println("    // Use " + className + ".newBuilder() to construct.");
		if (descriptor.isExtendable())
		{
			printer.println("    private " + className + "(com.google.protobuf.GeneratedMessage.ExtendableBuilder<" + context.getNameResolver().getImmutableClassName(descriptor) + ", ?> builder) {");
		}
		else
		{
			printer.println("    private " + className + "(com.google.protobuf.GeneratedMessage.Builder<?> builder) {");
		}
		printer.println("      super(builder);");
		printer.println("    }");
		printer.println("    private " + className + "() {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInitializationCode(printer);
		}
		printer.println("    }");
		printer.println();

		printer.println("    public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptor() {");
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("    }");
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
			printer.println("    @SuppressWarnings({\"rawtypes\"})");
			printer.println("    @java.lang.Override");
			printer.println("    protected com.google.protobuf.MapFieldReflectionAccessor internalGetMapFieldReflection(");
			printer.println("        int number) {");
			printer.println("      switch (number) {");
			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isMapField())
				{
					printer.println("        case " + field.getNumber() + ":");
					printer.println("          return internalGet" + StringUtils.capitalizedFieldName(field) + "();");
				}
			}
			printer.println("        default:");
			printer.println("          throw new RuntimeException(");
			printer.println("              \"Invalid map field number: \" + number);");
			printer.println("      }");
			printer.println("    }");
		}

		printer.println("    @java.lang.Override");
		printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("        internalGetFieldAccessorTable() {");
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("          .ensureFieldAccessorsInitialized(");
		printer.println("              " + fullClassName + ".class, " + fullClassName + ".Builder.class);");
		printer.println("    }");
		printer.println();

		for (com.google.protobuf.Descriptors.EnumDescriptor enumDescriptor : descriptor.getEnumTypes())
		{
			new ImmutableEnumGenerator(enumDescriptor, context).generate(printer);
		}

		for (com.google.protobuf.Descriptors.Descriptor nestedDescriptor : descriptor.getNestedTypes())
		{
			// Skip map entry types - they are synthetic types created for map fields
			// and should not be generated as separate nested classes
			if (!isMapEntryType(nestedDescriptor))
			{
				if (printer instanceof com.rubberjam.protobuf.compiler.java.IndentPrinter)
				{
					((com.rubberjam.protobuf.compiler.java.IndentPrinter) printer).indent();
				}
				ImmutableMessageGenerator messageGenerator = new ImmutableMessageGenerator(nestedDescriptor, context);
				messageGenerator.generateInterface(printer);
				messageGenerator.generate(printer);
				if (printer instanceof com.rubberjam.protobuf.compiler.java.IndentPrinter)
				{
					((com.rubberjam.protobuf.compiler.java.IndentPrinter) printer).outdent();
				}
			}
		}

		// bitField0_ for tracking field presence - only needed if there are optional fields
		// Oneof fields don't need bitField0_ in the message class (they use oneofCase_ instead)
		boolean needsBitField = false;
		for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
		{
			// Maps, repeated fields, and oneof fields don't need bitField0_ in the message class
			if (field.isMapField() || field.isRepeated() || context.isRealOneof(field))
			{
				continue;
			}
			// Optional fields (proto2) or proto3 optional fields need bitField0_
			if (field.hasPresence())
			{
				needsBitField = true;
				break;
			}
		}
		if (needsBitField)
		{
			printer.println("    private int bitField0_;");
		}

		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (!context.isSyntheticOneof(oneof))
			{
				String camelCaseName = StringUtils.underscoresToCamelCase(oneof.getName(), false);
				printer.println("    private int " + camelCaseName + "Case_ = 0;");
				printer.println("    @SuppressWarnings(\"serial\")");
				printer.println("    private java.lang.Object " + camelCaseName + "_;");

				String enumName = StringUtils.underscoresToCamelCase(oneof.getName(), true);
				printer.println("    public enum " + enumName + "Case");
				printer.println("        implements com.google.protobuf.Internal.EnumLite,");
				printer.println("            com.google.protobuf.AbstractMessage.InternalOneOfEnum {");
				for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
				{
					printer.println("      " + field.getName().toUpperCase() + "(" + field.getNumber() + "),");
				}

				printer.println("      " + enumName.toUpperCase() + "_NOT_SET(0);");
				printer.println("      private final int value;");
				printer.println("      private " + enumName + "Case(int value) {");
				printer.println("        this.value = value;");
				printer.println("      }");
				printer.println("      /**");
				printer.println("       * @param value The number of the enum to look for.");
				printer.println("       * @return The enum associated with the given number.");
				printer.println("       * @deprecated Use {@link #forNumber(int)} instead.");
				printer.println("       */");
				printer.println("      @java.lang.Deprecated");
				printer.println("      public static " + enumName + "Case valueOf(int value) {");
				printer.println("        return forNumber(value);");
				printer.println("      }");
				printer.println();
				printer.println("      public static " + enumName + "Case forNumber(int value) {");
				printer.println("        switch (value) {");
				for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
				{
					printer.println("          case " + field.getNumber() + ": return " + field.getName().toUpperCase() + ";");
				}
				printer.println("          case 0: return " + enumName.toUpperCase() + "_NOT_SET;");
				printer.println("          default: return null;");
				printer.println("        }");
				printer.println("      }");
				printer.println("      public int getNumber() {");
				printer.println("        return this.value;");
				printer.println("      }");
				printer.println("    };");
				printer.println();
				printer.println("    public " + enumName + "Case");
				printer.println("    get" + enumName + "Case() {");
				printer.println("      return " + enumName + "Case.forNumber(");
				printer.println("          " + camelCaseName + "Case_);");
				printer.println("    }");
				printer.println();
			}
		}

		// Fields (each with its FIELD_NUMBER constant before it)
		java.util.List<ImmutableFieldGenerator> generators = fieldGenerators.getFieldGenerators();
		for (int i = 0; i < generators.size(); i++)
		{
			ImmutableFieldGenerator fieldGenerator = generators.get(i);
			com.google.protobuf.Descriptors.FieldDescriptor field = fieldGenerator.getDescriptor();
			
			printer.println("    public static final int " + StringUtils.fieldConstantName(field) + " = " + field.getNumber() + ";");
			fieldGenerator.generateMembers(printer);
			if (i < generators.size() - 1)
			{
				printer.println();
			}
		}

		boolean hasRealOneofs = false;
		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (!context.isSyntheticOneof(oneof))
			{
				hasRealOneofs = true;
				break;
			}
		}
		if (!generators.isEmpty() || (!hasRealOneofs && needsBitField))
		{
			printer.println();
		}

		if (context.hasGeneratedMethods(descriptor))
		{
			// isInitialized()
			printer.println("    private byte memoizedIsInitialized = -1;");
			printer.println("    @java.lang.Override");
			printer.println("    public final boolean isInitialized() {");
			printer.println("      byte isInitialized = memoizedIsInitialized;");
			printer.println("      if (isInitialized == 1) return true;");
			printer.println("      if (isInitialized == 0) return false;");
			printer.println();

			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.isRequired())
				{
					printer.println("      if (!has" + StringUtils.capitalizedFieldName(field) + "()) {");
					printer.println("        memoizedIsInitialized = 0;");
					printer.println("        return false;");
					printer.println("      }");
				}
			}

			for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
			{
				if (field.getJavaType() == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
						&& com.rubberjam.protobuf.compiler.java.Helpers.hasRequiredFields(field.getMessageType()))
				{
					String name = StringUtils.capitalizedFieldName(field);
					if (field.isRequired())
					{
						printer.println("      if (!get" + name + "().isInitialized()) {");
						printer.println("        memoizedIsInitialized = 0;");
						printer.println("        return false;");
						printer.println("      }");
					}
					else if (field.isRepeated())
					{
						if (field.isMapField())
						{
							com.google.protobuf.Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");
							String valueType = context.getNameResolver().getImmutableClassName(valueField.getMessageType());
							printer.println("      for (" + valueType + " item : get" + name + "Map().values()) {");
							printer.println("        if (!item.isInitialized()) {");
							printer.println("          memoizedIsInitialized = 0;");
							printer.println("          return false;");
							printer.println("        }");
							printer.println("      }");
						}
						else
						{
							printer.println("      for (int i = 0; i < get" + name + "Count(); i++) {");
							printer.println("        if (!get" + name + "(i).isInitialized()) {");
							printer.println("          memoizedIsInitialized = 0;");
							printer.println("          return false;");
							printer.println("        }");
							printer.println("      }");
						}
					}
					else
					{
						printer.println("      if (has" + name + "()) {");
						printer.println("        if (!get" + name + "().isInitialized()) {");
						printer.println("          memoizedIsInitialized = 0;");
						printer.println("          return false;");
						printer.println("        }");
						printer.println("      }");
					}
				}
			}

			if (descriptor.isExtendable())
			{
				printer.println("      if (!extensionsAreInitialized()) {");
				printer.println("        memoizedIsInitialized = 0;");
				printer.println("        return false;");
				printer.println("      }");
			}
			printer.println("      memoizedIsInitialized = 1;");
			printer.println("      return true;");
			printer.println("    }");
			printer.println();

			// writeTo()
			printer.println("    @java.lang.Override");
			printer.println("    public void writeTo(com.google.protobuf.CodedOutputStream output)");
			printer.println("                        throws java.io.IOException {");
			if (descriptor.isExtendable())
			{
				printer.println("      com.google.protobuf.GeneratedMessage");
				printer.println("        .ExtendableMessage.ExtensionSerializer");
				printer.println("          extensionWriter = newExtensionSerializer();");
			}
			for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getSortedFieldGenerators())
			{
				fieldGenerator.generateWriteToCode(printer);
			}
			if (descriptor.isExtendable())
			{
				int limit = 536870912;
				if (!descriptor.toProto().getExtensionRangeList().isEmpty())
				{
					limit = descriptor.toProto().getExtensionRangeList().stream()
							.mapToInt(com.google.protobuf.DescriptorProtos.DescriptorProto.ExtensionRange::getEnd)
							.max().orElse(536870912);
				}
				printer.println("      extensionWriter.writeUntil(" + limit + ", output);");
			}
			printer.println("      getUnknownFields().writeTo(output);");
			printer.println("    }");
			printer.println();

			// getSerializedSize()
			printer.println("    @java.lang.Override");
			printer.println("    public int getSerializedSize() {");
			printer.println("      int size = memoizedSize;");
			printer.println("      if (size != -1) return size;");
			printer.println();
			printer.println("      size = 0;");
			for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getSortedFieldGenerators())
			{
				fieldGenerator.generateSerializedSizeCode(printer);
			}
			if (descriptor.isExtendable())
			{
				printer.println("      size += extensionsSerializedSize();");
			}
			printer.println("      size += getUnknownFields().getSerializedSize();");
			printer.println("      memoizedSize = size;");
			printer.println("      return size;");
			printer.println("    }");
			printer.println();

			// equals()
			printer.println("    @java.lang.Override");
			printer.println("    public boolean equals(final java.lang.Object obj) {");
			printer.println("      if (obj == this) {");
			printer.println("       return true;");
			printer.println("      }");
			// fullClassName is already defined in internalGetFieldAccessorTable block
			printer.println("      if (!(obj instanceof " + fullClassName + ")) {");
			printer.println("        return super.equals(obj);");
			printer.println("      }");
			printer.println("      " + fullClassName + " other = (" + fullClassName + ") obj;");
			printer.println();
			for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
			{
				if (!context.isRealOneof(fieldGenerator.getDescriptor()))
				{
					fieldGenerator.generateEqualsCode(printer);
				}
			}
			for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
			{
				if (!context.isSyntheticOneof(oneof))
				{
					String camelCaseName = StringUtils.underscoresToCamelCase(oneof.getName(), true);
					printer.println("      if (!get" + camelCaseName + "Case().equals(other.get" + camelCaseName + "Case())) return false;");
					printer.println("      switch (" + StringUtils.underscoresToCamelCase(oneof.getName(), false) + "Case_) {");
					for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
					{
						printer.println("        case " + field.getNumber() + ":");
						fieldGenerators.get(field).generateOneofEqualsCode(printer);
						printer.println("          break;");
					}
					printer.println("        case 0:");
					printer.println("        default:");
					printer.println("      }");
				}
			}
			printer.println("      if (!getUnknownFields().equals(other.getUnknownFields())) return false;");
			if (descriptor.isExtendable())
			{
				printer.println("      if (!getExtensionFields().equals(other.getExtensionFields()))");
				printer.println("        return false;");
			}
			printer.println("      return true;");
			printer.println("    }");
			printer.println();

			// hashCode()
			printer.println("    @java.lang.Override");
			printer.println("    public int hashCode() {");
			printer.println("      if (memoizedHashCode != 0) {");
			printer.println("        return memoizedHashCode;");
			printer.println("      }");
			printer.println("      int hash = 41;");
			printer.println("      hash = (19 * hash) + getDescriptor().hashCode();");
			for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
			{
				if (!context.isRealOneof(fieldGenerator.getDescriptor()))
				{
					fieldGenerator.generateHashCode(printer);
				}
			}
			for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
			{
				if (!context.isSyntheticOneof(oneof))
				{
					printer.println("      switch (" + StringUtils.underscoresToCamelCase(oneof.getName(), false) + "Case_) {");
					for (com.google.protobuf.Descriptors.FieldDescriptor field : oneof.getFields())
					{
						printer.println("        case " + field.getNumber() + ":");
						fieldGenerators.get(field).generateOneofHashCode(printer);
						printer.println("          break;");
					}
					printer.println("        case 0:");
					printer.println("        default:");
					printer.println("      }");
				}
			}
			if (descriptor.isExtendable())
			{
				printer.println("      hash = hashFields(hash, getExtensionFields());");
			}
			printer.println("      hash = (29 * hash) + getUnknownFields().hashCode();");
			printer.println("      memoizedHashCode = hash;");
			printer.println("      return hash;");
			printer.println("    }");
			printer.println();
		}

		// parseFrom() methods
		// fullClassName is already defined in internalGetFieldAccessorTable block
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        java.nio.ByteBuffer data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        java.nio.ByteBuffer data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        com.google.protobuf.ByteString data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        com.google.protobuf.ByteString data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(byte[] data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        byte[] data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(java.io.InputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        java.io.InputStream input,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input, extensionRegistry);");
		printer.println("    }");
		printer.println();
		printer.println("    public static " + fullClassName + " parseDelimitedFrom(java.io.InputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseDelimitedWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println();
		printer.println("    public static " + fullClassName + " parseDelimitedFrom(");
		printer.println("        java.io.InputStream input,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        com.google.protobuf.CodedInputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println("    public static " + fullClassName + " parseFrom(");
		printer.println("        com.google.protobuf.CodedInputStream input,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input, extensionRegistry);");
		printer.println("    }");
		printer.println();

		// Builder methods
		printer.println("    @java.lang.Override");
		printer.println("    public Builder newBuilderForType() { return newBuilder(); }");

		printer.println("    public static Builder newBuilder() {");
		printer.println("      return DEFAULT_INSTANCE.toBuilder();");
		printer.println("    }");
		

		// fullClassName is already defined in generate method
		printer.println("    public static Builder newBuilder(" + fullClassName + " prototype) {");
		printer.println("      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);");
		printer.println("    }");


		printer.println("    @java.lang.Override");
		printer.println("    public Builder toBuilder() {");
		printer.println("      return this == DEFAULT_INSTANCE");
		printer.println("          ? new Builder() : new Builder().mergeFrom(this);");
		printer.println("    }");
		printer.println();
		printer.println("    @java.lang.Override");
		printer.println("    protected Builder newBuilderForType(");
		printer.println("        com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("      Builder builder = new Builder(parent);");
		printer.println("      return builder;");
		printer.println("    }");


	
		


		messageBuilderGenerator.generate(printer);
		printer.println();

		// Default instance
		printer.println("    // @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")");
		printer.println("    private static final " + fullClassName + " DEFAULT_INSTANCE;");
		printer.println("    static {");
		printer.println("      DEFAULT_INSTANCE = new " + fullClassName + "();");
		printer.println("    }");
		printer.println();

		printer.println("    public static " + fullClassName + " getDefaultInstance() {");
		printer.println("      return DEFAULT_INSTANCE;");
		printer.println("    }");
		printer.println();

		// PARSER field
		printer.println("    private static final com.google.protobuf.Parser<" + className + ">");
		printer.println("        PARSER = new com.google.protobuf.AbstractParser<" + className + ">() {");
		printer.println("      @java.lang.Override");
		printer.println("      public " + className + " parsePartialFrom(");
		printer.println("          com.google.protobuf.CodedInputStream input,");
		printer.println("          com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("          throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("        Builder builder = newBuilder();");
		printer.println("        try {");
		printer.println("          builder.mergeFrom(input, extensionRegistry);");
		printer.println("        } catch (com.google.protobuf.InvalidProtocolBufferException e) {");
		printer.println("          throw e.setUnfinishedMessage(builder.buildPartial());");
		printer.println("        } catch (com.google.protobuf.UninitializedMessageException e) {");
		printer.println("          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());");
		printer.println("        } catch (java.io.IOException e) {");
		printer.println("          throw new com.google.protobuf.InvalidProtocolBufferException(e)");
		printer.println("              .setUnfinishedMessage(builder.buildPartial());");
		printer.println("        }");
		printer.println("        return builder.buildPartial();");
		printer.println("      }");
		printer.println("    };");
		printer.println();
		printer.println("    public static com.google.protobuf.Parser<" + className + "> parser() {");
		printer.println("      return PARSER;");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    public com.google.protobuf.Parser<" + className + "> getParserForType() {");
		printer.println("      return PARSER;");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    public " + fullClassName + " getDefaultInstanceForType() {");
		printer.println("      return DEFAULT_INSTANCE;");
		printer.println("    }");
		printer.println();

		for (com.google.protobuf.Descriptors.FieldDescriptor extension : descriptor.getExtensions())
		{
			new ImmutableExtensionGenerator(extension, context).generate(printer);
		}

		printer.println("  }");
		printer.println();
	}

	@Override
	public void generateInterface(PrintWriter printer)
	{
		String className = descriptor.getName();
		printer.println("  public interface " + className + "OrBuilder extends");
		printer.println("      // @@protoc_insertion_point(interface_extends:" + descriptor.getFullName() + ")");
		if (descriptor.isExtendable())
		{
			printer.println("      com.google.protobuf.GeneratedMessage.");
			printer.println("          ExtendableMessageOrBuilder<" + className + "> {");
		}
		else
		{
			printer.println("      com.google.protobuf.MessageOrBuilder {");
		}
		if (!fieldGenerators.getFieldGenerators().isEmpty())
		{
			printer.println();
		}
		boolean first = true;
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			if (!first)
			{
				printer.println();
			}
			first = false;
			fieldGenerator.generateInterfaceMembers(printer);
		}

		for (com.google.protobuf.Descriptors.OneofDescriptor oneof : descriptor.getOneofs())
		{
			if (!context.isSyntheticOneof(oneof))
			{
				printer.println();
				String camelCaseName = StringUtils.underscoresToCamelCase(oneof.getName(), true);
				printer.println("    " + context.getNameResolver().getImmutableClassName(descriptor) + "." + camelCaseName + "Case get" + camelCaseName + "Case();");
			}
		}
		printer.println("  }");
	}

	@Override
	public void generateExtensionRegistrationCode(PrintWriter printer)
	{
		for (com.google.protobuf.Descriptors.FieldDescriptor extension : descriptor.getExtensions())
		{
			ImmutableExtensionGenerator extensionGenerator = new ImmutableExtensionGenerator(extension, context);
			extensionGenerator.generateRegistrationCode(printer);
		}

		for (com.google.protobuf.Descriptors.Descriptor nestedType : descriptor.getNestedTypes())
		{
			// Skip map entry types
			if (!isMapEntryType(nestedType))
			{
				new ImmutableMessageGenerator(nestedType, context).generateExtensionRegistrationCode(printer);
			}
		}
	}

	private String getUniqueFileScopeIdentifier(Descriptor descriptor)
	{
		return "static_" + descriptor.getFullName().replace('.', '_');
	}

	/**
	 * Checks if a nested type is a map entry type (synthetic type created for map fields).
	 * Map entry types should not be generated as separate nested classes.
	 */
	private boolean isMapEntryType(com.google.protobuf.Descriptors.Descriptor nestedType)
	{
		if (nestedType.getFields().size() != 2)
		{
			return false;
		}
		com.google.protobuf.Descriptors.FieldDescriptor field1 = nestedType.getFields().get(0);
		com.google.protobuf.Descriptors.FieldDescriptor field2 = nestedType.getFields().get(1);
		if (!((field1.getName().equals("key") && field2.getName().equals("value")) ||
			(field1.getName().equals("value") && field2.getName().equals("key"))))
		{
			return false;
		}
		// Check if any map field in the parent uses this nested type
		// Use full name comparison since descriptor objects may not be == equal
		String nestedTypeFullName = nestedType.getFullName();
		for (com.google.protobuf.Descriptors.FieldDescriptor field : descriptor.getFields())
		{
			if (field.isMapField() && field.getMessageType().getFullName().equals(nestedTypeFullName))
			{
				return true;
			}
		}
		return false;
	}
}
