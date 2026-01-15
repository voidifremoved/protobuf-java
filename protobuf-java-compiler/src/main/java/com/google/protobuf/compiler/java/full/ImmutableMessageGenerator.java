package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldGeneratorMap;
import com.google.protobuf.compiler.java.MessageGenerator;
import java.io.PrintWriter;

public class ImmutableMessageGenerator extends MessageGenerator {
  private final Context context;
  private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;

  public ImmutableMessageGenerator(Descriptor descriptor, Context context) {
    super(descriptor);
    this.context = context;
    this.fieldGenerators = new FieldGeneratorMap<>(descriptor);
    // TODO: Initialize field generators using MakeFieldGens (need to implement that)
  }

  @Override
  public void generateStaticVariables(PrintWriter printer, int[] bytecodeEstimate) {
      printer.println("// TODO: generateStaticVariables");
  }

  @Override
  public int generateStaticVariableInitializers(PrintWriter printer) {
      printer.println("// TODO: generateStaticVariableInitializers");
      return 0;
  }

  @Override
  public void generate(PrintWriter printer) {
      String className = descriptor.getName();
      printer.println("public static final class " + className + " extends");
      printer.println("    com.google.protobuf.GeneratedMessage implements");
      printer.println("    " + className + "OrBuilder {");
      printer.println("private static final long serialVersionUID = 0L;");

      printer.println("  private " + className + "() {");
      printer.println("  }");

      printer.println("  @java.lang.Override");
      printer.println("  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {");
      printer.println("    return this.unknownFields;");
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
  public void generateInterface(PrintWriter printer) {
      printer.println("// TODO: generateInterface");
  }

  @Override
  public void generateExtensionRegistrationCode(PrintWriter printer) {
      printer.println("// TODO: generateExtensionRegistrationCode");
  }
}
