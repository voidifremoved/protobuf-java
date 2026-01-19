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

		// Builder Class
		printer.println("  public static final class Builder extends");
		printer.println("      com.google.protobuf.GeneratedMessage.Builder<Builder> implements");
		printer.println("      " + className + "OrBuilder {");

		printer.println("    public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptor() {");
		printer.println(
				"      return "
						+ context.getNameResolver().getFileClassName(descriptor.getFile(), true)
						+ ".internal_"
						+ descriptor.getName()
						+ "_descriptor;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("        internalGetFieldAccessorTable() {");
		printer.println(
				"      return "
						+ context.getNameResolver().getFileClassName(descriptor.getFile(), true)
						+ ".internal_"
						+ descriptor.getName()
						+ "_fieldAccessorTable");
		printer.println("          .ensureFieldAccessorsInitialized(");
		printer.println("              " + className + ".class, " + className + ".Builder.class);");
		printer.println("    }");

		printer.println("    private Builder() {");
		printer.println("    }");

		printer.println("    private Builder(com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("      super(parent);");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public Builder clear() {");
		printer.println("      super.clear();");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderClearCode(printer);
		}
		printer.println("      return this;");
		printer.println("    }");

		printer.println("    @java.lang.Override");
		printer.println("    public com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptorForType() {");
		printer.println(
				"      return "
						+ context.getNameResolver().getFileClassName(descriptor.getFile(), true)
						+ ".internal_"
						+ descriptor.getName()
						+ "_descriptor;");
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
		printer.println("      if (bitField0_ != 0) { buildPartial0(result); }");
		printer.println("      onBuilt();");
		printer.println("      return result;");
		printer.println("    }");

		printer.println("    private void buildPartialRepeatedFields(" + className + " result) {");
		// TODO: repeated fields building
		printer.println("    }");

		printer.println("    private void buildPartial0(" + className + " result) {");
		printer.println("      int from_bitField0_ = bitField0_;");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuildingCode(printer);
		}
		printer.println("    }");

		// Fields for builder
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateBuilderMembers(printer);
		}

		printer.println("  }"); // End Builder
	}
}
