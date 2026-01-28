package com.rubberjam.protobuf.compiler.java.proto3.lite;

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

		// Builder Class
		printer.println("  public static final class Builder extends");
		printer.println("      com.google.protobuf.GeneratedMessageLite.Builder<");
		printer.println("        " + className + ", Builder> implements");
		printer.println("      " + className + "OrBuilder {");

		printer.println("    private Builder() {");
		printer.println("      super(DEFAULT_INSTANCE);");
		printer.println("    }");

		// Stub generation for fields
		// for (ImmutableFieldGenerator fieldGenerator :
		// fieldGenerators.getFieldGenerators()) {
		// fieldGenerator.generateBuilderMembers(printer);
		// }

		printer.println("  }"); // End Builder
	}
}
