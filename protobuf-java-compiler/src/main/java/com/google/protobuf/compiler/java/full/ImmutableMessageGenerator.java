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
    this.fieldGenerators = MakeFieldGens.makeImmutableFieldGenerators(descriptor, context);
  }

  @Override
  public void generateStaticVariables(PrintWriter printer, int[] bytecodeEstimate) {
      // Stub
  }

  @Override
  public int generateStaticVariableInitializers(PrintWriter printer) {
      // Stub
      return 0;
  }

  @Override
  public void generate(PrintWriter printer) {
      String className = descriptor.getName();
      printer.println("public static final class " + className + " extends");
      printer.println("    com.google.protobuf.GeneratedMessage implements");
      printer.println("    " + className + "OrBuilder {");
      printer.println("private static final long serialVersionUID = 0L;");

      // Fields
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
          fieldGenerator.generateMembers(printer);
      }

      printer.println("  private " + className + "() {");
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
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

      // Builder Class
      printer.println("  public static final class Builder extends");
      printer.println("      com.google.protobuf.GeneratedMessage.Builder<Builder> implements");
      printer.println("      " + className + "OrBuilder {");

      printer.println("    public static final com.google.protobuf.Descriptors.Descriptor");
      printer.println("        getDescriptor() {");
      printer.println("      return " + context.getNameResolver().getFileClassName(descriptor.getFile(), true) + ".internal_" + descriptor.getName() + "_descriptor;");
      printer.println("    }");

      printer.println("    @java.lang.Override");
      printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
      printer.println("        internalGetFieldAccessorTable() {");
      printer.println("      return " + context.getNameResolver().getFileClassName(descriptor.getFile(), true) + ".internal_" + descriptor.getName() + "_fieldAccessorTable");
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
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
          fieldGenerator.generateBuilderClearCode(printer);
      }
      printer.println("      return this;");
      printer.println("    }");

      printer.println("    @java.lang.Override");
      printer.println("    public com.google.protobuf.Descriptors.Descriptor");
      printer.println("        getDescriptorForType() {");
      printer.println("      return " + context.getNameResolver().getFileClassName(descriptor.getFile(), true) + ".internal_" + descriptor.getName() + "_descriptor;");
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
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
          fieldGenerator.generateBuildingCode(printer);
      }
      printer.println("    }");

      // Fields for builder
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
          fieldGenerator.generateBuilderMembers(printer);
      }

      printer.println("  }"); // End Builder

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
  public void generateInterface(PrintWriter printer) {
      String className = descriptor.getName();
      printer.println("public interface " + className + "OrBuilder extends");
      printer.println("    com.google.protobuf.MessageOrBuilder {");
      for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators()) {
          fieldGenerator.generateInterfaceMembers(printer);
      }
      printer.println("}");
  }

  @Override
  public void generateExtensionRegistrationCode(PrintWriter printer) {
      printer.println("// TODO: generateExtensionRegistrationCode");
  }
}
