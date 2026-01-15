package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldCommon;
import com.google.protobuf.compiler.java.FieldGeneratorInfo;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MessageFieldGenerator extends ImmutableFieldGenerator {
  private final FieldDescriptor descriptor;
  private final int messageBitIndex;
  private final int builderBitIndex;
  private final Context context;
  private final Map<String, String> variables;

  public MessageFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    this.descriptor = descriptor;
    this.messageBitIndex = messageBitIndex;
    this.builderBitIndex = builderBitIndex;
    this.context = context;
    this.variables = new HashMap<>();
    setMessageVariables(
        descriptor,
        messageBitIndex,
        builderBitIndex,
        context.getFieldGeneratorInfo(descriptor),
        variables,
        context);
  }

  private void setMessageVariables(
      FieldDescriptor descriptor,
      int messageBitIndex,
      int builderBitIndex,
      FieldGeneratorInfo info,
      Map<String, String> variables,
      Context context) {
    FieldCommon.setCommonFieldVariables(descriptor, info, variables);
    variables.put("type", context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
    variables.put("group_or_message",
        descriptor.getType() == FieldDescriptor.Type.GROUP ? "Group" : "Message");

    // Bit logic
    variables.put("is_field_present_message", "true"); // Placeholder
    variables.put("set_has_field_bit_builder", "");
    variables.put("clear_has_field_bit_builder", "");
  }

  @Override
  public int getMessageBitIndex() { return messageBitIndex; }
  @Override
  public int getBuilderBitIndex() { return builderBitIndex; }
  @Override
  public int getNumBitsForMessage() { return 0; }
  @Override
  public int getNumBitsForBuilder() { return 0; }

  @Override
  public void generateInterfaceMembers(PrintWriter printer) {
    printer.println("  boolean has" + variables.get("capitalized_name") + "();");
    printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
    printer.println("  " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name") + "OrBuilder();");
  }

  @Override
  public void generateMembers(PrintWriter printer) {
    printer.println("  private " + variables.get("type") + " " + variables.get("name") + "_;");

    printer.println("  @java.lang.Override");
    printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
    printer.println("    return " + variables.get("name") + "_ != null;");
    printer.println("  }");

    printer.println("  @java.lang.Override");
    printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
    printer.println("    return " + variables.get("name") + "_ == null ? " + variables.get("type") + ".getDefaultInstance() : " + variables.get("name") + "_;");
    printer.println("  }");

    printer.println("  @java.lang.Override");
    printer.println("  public " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name") + "OrBuilder() {");
    printer.println("    return " + variables.get("name") + "_ == null ? " + variables.get("type") + ".getDefaultInstance() : " + variables.get("name") + "_;");
    printer.println("  }");
  }

  @Override
  public void generateBuilderMembers(PrintWriter printer) {
    printer.println("  private " + variables.get("type") + " " + variables.get("name") + "_;");

    // Nested builder logic (SingleFieldBuilder) is complex, creating simplified version first
    printer.println("  private com.google.protobuf.SingleFieldBuilder<" + variables.get("type") + ", " + variables.get("type") + ".Builder, " + variables.get("type") + "OrBuilder> " + variables.get("name") + "Builder_;");

    printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
    printer.println("    return " + variables.get("name") + "Builder_ != null || " + variables.get("name") + "_ != null;");
    printer.println("  }");

    printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
    printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
    printer.println("      return " + variables.get("name") + "_ == null ? " + variables.get("type") + ".getDefaultInstance() : " + variables.get("name") + "_;");
    printer.println("    } else {");
    printer.println("      return " + variables.get("name") + "Builder_.getMessage();");
    printer.println("    }");
    printer.println("  }");

    printer.println("  public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
    printer.println("    if (value == null) { throw new NullPointerException(); }");
    printer.println("    " + variables.get("name") + "_ = value;");
    printer.println("    " + variables.get("name") + "Builder_ = null;");
    printer.println("    return this;");
    printer.println("  }");

    // merge, clear, builder getters...
    printer.println("  public Builder merge" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
    printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
    printer.println("      if (" + variables.get("name") + "_ != null) {");
    printer.println("        " + variables.get("name") + "_ = " + variables.get("type") + ".newBuilder(" + variables.get("name") + "_).mergeFrom(value).buildPartial();");
    printer.println("      } else {");
    printer.println("        " + variables.get("name") + "_ = value;");
    printer.println("      }");
    printer.println("    } else {");
    printer.println("      " + variables.get("name") + "Builder_.mergeFrom(value);");
    printer.println("    }");
    printer.println("    return this;");
    printer.println("  }");

    printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
    printer.println("    " + variables.get("name") + "_ = null;");
    printer.println("    " + variables.get("name") + "Builder_ = null;");
    printer.println("    return this;");
    printer.println("  }");
  }

  @Override
  public void generateInitializationCode(PrintWriter printer) {
    // No-op for null initialization
  }

  @Override
  public void generateBuilderClearCode(PrintWriter printer) {
    printer.println("    " + variables.get("name") + "_ = null;");
    printer.println("    " + variables.get("name") + "Builder_ = null;");
  }

  @Override
  public void generateMergingCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public void generateBuildingCode(PrintWriter printer) {
      printer.println("      if (" + variables.get("name") + "Builder_ == null) {");
      printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
      printer.println("      } else {");
      printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "Builder_.build();");
      printer.println("      }");
  }

  @Override
  public void generateBuilderParsingCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public void generateSerializedSizeCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public void generateFieldBuilderInitializationCode(PrintWriter printer) {
      // no-op
  }

  @Override
  public void generateEqualsCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public void generateHashCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public void generateSerializationCode(PrintWriter printer) {
      // Placeholder
  }

  @Override
  public String getBoxedType() {
      return variables.get("type");
  }
}
