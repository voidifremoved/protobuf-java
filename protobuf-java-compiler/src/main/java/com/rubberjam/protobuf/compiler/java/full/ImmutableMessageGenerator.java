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
		printer.println("      internal_" + identifier + "_descriptor;");
		printer.println("  private static final com.google.protobuf.GeneratedMessage.FieldAccessorTable");
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
			printer.println("        getDescriptor().getMessageTypes().get(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentIdentifier = getUniqueFileScopeIdentifier(descriptor.getContainingType());
			printer.println("    internal_" + identifier + "_descriptor =");
			printer.println("        internal_" + parentIdentifier + "_descriptor.getNestedTypes().get(" + descriptor.getIndex() + ");");
		}
		printer.println("    internal_" + identifier + "_fieldAccessorTable =");
		printer.println("        new com.google.protobuf.GeneratedMessage.FieldAccessorTable(");
		printer.print("            internal_" + identifier + "_descriptor,");
		printer.print("            new java.lang.String[] {");
		for (int i = 0; i < descriptor.getFields().size(); i++)
		{
			if (i > 0)
			{
				printer.print(", ");
			}
			printer.print("\"" + StringUtils.capitalizedFieldName(descriptor.getFields().get(i)) + "\"");
		}
		printer.println("});");

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
		
		// Match C++ WriteMessageDocComment behavior - use DocComment utility
		com.rubberjam.protobuf.compiler.java.DocComment.writeMessageDocComment(printer, descriptor, context.getOptions(), false);
		
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

		printer.println("    @java.lang.Override");
		printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("        internalGetFieldAccessorTable() {");
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("          .ensureFieldAccessorsInitialized(");
		String fullClassName = context.getNameResolver().getImmutableClassName(descriptor);
		printer.println("              " + fullClassName + ".class, " + fullClassName + ".Builder.class);");
		printer.println("    }");
		printer.println();

		for (com.google.protobuf.Descriptors.EnumDescriptor enumDescriptor : descriptor.getEnumTypes())
		{
			new ImmutableEnumGenerator(enumDescriptor, context).generate(printer);
		}

		for (com.google.protobuf.Descriptors.Descriptor nestedDescriptor : descriptor.getNestedTypes())
		{
			ImmutableMessageGenerator messageGenerator = new ImmutableMessageGenerator(nestedDescriptor, context);
			messageGenerator.generateInterface(printer);
			messageGenerator.generate(printer);
		}

		// bitField0_ for tracking field presence
		printer.println("    private int bitField0_;");

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
		printer.println();

		// isInitialized()
		printer.println("    private byte memoizedIsInitialized = -1;");
		printer.println("    @java.lang.Override");
		printer.println("    public final boolean isInitialized() {");
		printer.println("      byte isInitialized = memoizedIsInitialized;");
		printer.println("      if (isInitialized == 1) return true;");
		printer.println("      if (isInitialized == 0) return false;");
		printer.println();
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
			fieldGenerator.generateEqualsCode(printer);
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
			fieldGenerator.generateHashCode(printer);
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

		// parseFrom() methods
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        java.nio.ByteBuffer data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        java.nio.ByteBuffer data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        com.google.protobuf.ByteString data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        com.google.protobuf.ByteString data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(byte[] data)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        byte[] data,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws com.google.protobuf.InvalidProtocolBufferException {");
		printer.println("      return PARSER.parseFrom(data, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(java.io.InputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        java.io.InputStream input,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input, extensionRegistry);");
		printer.println("    }");
		printer.println();
		printer.println("    public static " + outerClassName + "." + className + " parseDelimitedFrom(java.io.InputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseDelimitedWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println();
		printer.println("    public static " + outerClassName + "." + className + " parseDelimitedFrom(");
		printer.println("        java.io.InputStream input,");
		printer.println("        com.google.protobuf.ExtensionRegistryLite extensionRegistry)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
		printer.println("        com.google.protobuf.CodedInputStream input)");
		printer.println("        throws java.io.IOException {");
		printer.println("      return com.google.protobuf.GeneratedMessage");
		printer.println("          .parseWithIOException(PARSER, input);");
		printer.println("    }");
		printer.println("    public static " + outerClassName + "." + className + " parseFrom(");
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
		

		printer.println("    public static Builder newBuilder(" + outerClassName + "." + className + " prototype) {");
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
		printer.println("         com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("      Builder builder = new Builder(parent);");
		printer.println("      return builder;");
		printer.println("    }");


	
		


		messageBuilderGenerator.generate(printer);
		printer.println();

		// Default instance
		printer.println("    // @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")");
		printer.println("    private static final " + outerClassName + "." + className + " DEFAULT_INSTANCE;");
		printer.println("    static {");
		printer.println("      DEFAULT_INSTANCE = new " + outerClassName + "." + className + "();");
		printer.println("    }");
		printer.println();

		printer.println("    public static " + outerClassName + "." + className + " getDefaultInstance() {");
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
		printer.println("    public " + outerClassName + "." +className + " getDefaultInstanceForType() {");
		printer.println("      return DEFAULT_INSTANCE;");
		printer.println("    }");

		printer.println("  }");
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
		printer.println();
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
			new ImmutableMessageGenerator(nestedType, context).generateExtensionRegistrationCode(printer);
		}
	}

	private String getUniqueFileScopeIdentifier(Descriptor descriptor)
	{
		return "static_" + descriptor.getFullName().replace('.', '_');
	}
}
