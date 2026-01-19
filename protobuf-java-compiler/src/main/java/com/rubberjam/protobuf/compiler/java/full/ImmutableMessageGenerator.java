package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;
import com.rubberjam.protobuf.compiler.java.MessageGenerator;

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
		// Stub
	}

	@Override
	public int generateStaticVariableInitializers(PrintWriter printer)
	{
		// Stub
		return 0;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		String className = descriptor.getName();
		printer.println("public static final class " + className + " extends");
		printer.println("    com.google.protobuf.GeneratedMessage implements");
		printer.println("    " + className + "OrBuilder {");
		printer.println("private static final long serialVersionUID = 0L;");

		// Fields
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateMembers(printer);
		}

		printer.println("  private " + className + "() {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInitializationCode(printer);
		}
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {");
		printer.println("    return this.unknownFields;");
		printer.println("  }");

		// Builder
		printer.println("  public static Builder newBuilder() {");
		printer.println("    return DEFAULT_INSTANCE.toBuilder();");
		printer.println("  }");

		printer.println("  public static Builder newBuilder(" + className + " prototype) {");
		printer.println("    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public Builder toBuilder() {");
		printer.println("    return this == DEFAULT_INSTANCE");
		printer.println("        ? new Builder() : new Builder().mergeFrom(this);");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("    Builder builder = new Builder(parent);");
		printer.println("    return builder;");
		printer.println("  }");

		messageBuilderGenerator.generate(printer);

		// Constructor taking builder
		printer.println("  private " + className + "(Builder builder) {");
		printer.println("    super(builder);");
		printer.println("  }");

		// Static block
		printer.println("  static {");
		printer.println("  }");

		// Default instance
		printer.println("  private static final " + className + " DEFAULT_INSTANCE;");
		printer.println("  static {");
		printer.println("    DEFAULT_INSTANCE = new " + className + "();");
		printer.println("  }");

		printer.println("  public static " + className + " getDefaultInstance() {");
		printer.println("    return DEFAULT_INSTANCE;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public " + className + " getDefaultInstanceForType() {");
		printer.println("    return DEFAULT_INSTANCE;");
		printer.println("  }");

		printer.println("}");
	}

	@Override
	public void generateInterface(PrintWriter printer)
	{
		String className = descriptor.getName();
		printer.println("public interface " + className + "OrBuilder extends");
		printer.println("    com.google.protobuf.MessageOrBuilder {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInterfaceMembers(printer);
		}
		printer.println("}");
	}

	@Override
	public void generateExtensionRegistrationCode(PrintWriter printer)
	{
		printer.println("// TODO: generateExtensionRegistrationCode");
	}
}
