package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.MessageGenerator;

import java.io.PrintWriter;

public class ImmutableMessageGenerator extends MessageGenerator
{
	private final Descriptor descriptor;
	private final Context context;

	public ImmutableMessageGenerator(Descriptor descriptor, Context context)
	{
		super(descriptor);
		this.descriptor = descriptor;
		this.context = context;
	}

	@Override
	public void generateStaticVariables(PrintWriter printer, int[] bytecodeEstimate)
	{
		// TODO: Implement
	}

	@Override
	public int generateStaticVariableInitializers(PrintWriter printer)
	{
		// TODO: Implement
		return 0;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		printer.println("// Lite Message Generation for " + descriptor.getName() + " is pending.");
		printer.println("public static final class " + descriptor.getName() + " extends");
		printer.println("    com.google.protobuf.GeneratedMessageLite<" + descriptor.getName() + ", " + descriptor.getName()
				+ ".Builder> implements");
		printer.println("    " + descriptor.getName() + "OrBuilder {");
		printer.println("  private " + descriptor.getName() + "() {}");

		// Minimal valid body to compile
		printer.println("  private static final " + descriptor.getName() + " DEFAULT_INSTANCE;");
		printer.println("  static {");
		printer.println("    DEFAULT_INSTANCE = new " + descriptor.getName() + "();");
		printer.println("  }");
		printer.println("  public static " + descriptor.getName() + " getDefaultInstance() {");
		printer.println("    return DEFAULT_INSTANCE;");
		printer.println("  }");

		// Builder
		printer.println("  public static final class Builder extends");
		printer.println("      com.google.protobuf.GeneratedMessageLite.Builder<");
		printer.println("        " + descriptor.getName() + ", Builder> implements");
		printer.println("      " + descriptor.getName() + "OrBuilder {");
		printer.println("    private Builder() {");
		printer.println("      super(DEFAULT_INSTANCE);");
		printer.println("    }");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  protected final java.lang.Object dynamicMethod(");
		printer.println("      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,");
		printer.println("      java.lang.Object arg0, java.lang.Object arg1) {");
		printer.println("    switch (method) {");
		printer.println("      case NEW_MUTABLE_INSTANCE: return new " + descriptor.getName() + "();");
		printer.println("      case NEW_BUILDER: return new Builder();");
		printer.println("      case BUILD_MESSAGE_INFO: return null; // TODO");
		printer.println("      case GET_DEFAULT_INSTANCE: return DEFAULT_INSTANCE;");
		printer.println("      case GET_PARSER: return null; // TODO");
		printer.println("      case GET_MEMOIZED_IS_INITIALIZED: return (byte) 1;");
		printer.println("      case SET_MEMOIZED_IS_INITIALIZED: return null;");
		printer.println("    }");
		printer.println("    throw new UnsupportedOperationException();");
		printer.println("  }");

		printer.println("}");
	}

	@Override
	public void generateInterface(PrintWriter printer)
	{
		printer.println("public interface " + descriptor.getName() + "OrBuilder extends");
		printer.println("    com.google.protobuf.MessageLiteOrBuilder {");
		printer.println("}");
	}

	@Override
	public void generateExtensionRegistrationCode(PrintWriter printer)
	{
		// TODO: Implement
	}
}
