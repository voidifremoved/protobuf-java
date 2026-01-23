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
		printer.println("  public static final class Builder extends");
		if (descriptor.isExtendable())
		{
			printer.println("      com.google.protobuf.GeneratedMessage.ExtendableBuilder<" + context.getNameResolver().getImmutableClassName(descriptor) + ", Builder> implements");
		}
		else
		{
			printer.println("      com.google.protobuf.GeneratedMessage.Builder<Builder> implements");
		}
		printer.println("      " + className + "OrBuilder {");

		printer.println("    public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptor() {");
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		String packageName = context.getNameResolver().getFileJavaPackage(descriptor.getFile());
		String fileClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
		String outerClassName = packageName.isEmpty() ? fileClassName : packageName + "." + fileClassName;
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("        internalGetFieldAccessorTable() {");
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("          .ensureFieldAccessorsInitialized(");
		printer.println("              " + className + ".class, " + className + ".Builder.class);");
		printer.println("    }");

		printer.println("    private Builder() {");
		printer.println("    }");

		printer.println("    private Builder(com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("      super(parent);");
		printer.println("    }");

		for (int i = 0; i < totalBuilderInts; i++)
		{
			printer.println("    private int " + getBitFieldName(i) + ";");
		}

		printer.println("    @java.lang.Override");
		printer.println("    public Builder clear() {");
		printer.println("      super.clear();");
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("      " + getBitFieldName(i) + " = 0;");
		}
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderClearCode(printer);
		}
		printer.println("      return this;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptorForType() {");
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public " + className + " getDefaultInstanceForType() {");
		printer.println("      return " + className + ".getDefaultInstance();");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public " + className + " build() {");
		printer.println("      " + className + " result = buildPartial();");
		printer.println("      if (!result.isInitialized()) {");
		printer.println("        throw newUninitializedMessageException(result);");
		printer.println("      }");
		printer.println("      return result;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public " + className + " buildPartial() {");
		printer.println("      " + className + " result = new " + className + "(this);");
		printer.println("      buildPartialRepeatedFields(result);");
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("      if (" + getBitFieldName(i) + " != 0) { buildPartial" + i + "(result); }");
		}
		printer.println("      onBuilt();");
		printer.println("      return result;");
		printer.println("    }");

		printer.println("    private void buildPartialRepeatedFields(" + className + " result) {");
		// TODO: repeated fields building
		printer.println("    }");

		int fieldIndex = 0;
		for (int i = 0; i < totalBuilderPieces; i++)
		{
			printer.println("    private void buildPartial" + i + "(" + className + " result) {");
			printer.println("      int from_" + getBitFieldName(i) + " = " + getBitFieldName(i) + ";");
			int end = Math.min(fieldIndex + 32, totalFields);
			for (; fieldIndex < end; fieldIndex++)
			{
				fieldGenerators.getFieldGenerators().get(fieldIndex).generateBuildingCode(printer);
			}
			printer.println("    }");
		}

		// Fields for builder
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderMembers(printer);
		}

		printer.println("  }"); // End Builder
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
