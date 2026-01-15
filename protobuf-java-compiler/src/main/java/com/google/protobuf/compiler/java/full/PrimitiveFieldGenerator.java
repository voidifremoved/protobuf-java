package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldCommon;
import com.google.protobuf.compiler.java.FieldGeneratorInfo;
import com.google.protobuf.compiler.java.JavaType;
import com.google.protobuf.compiler.java.StringUtils;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveFieldGenerator extends ImmutableFieldGenerator {
  private final FieldDescriptor descriptor;
  private final int messageBitIndex;
  private final int builderBitIndex;
  private final Context context;
  private final Map<String, String> variables;

  public PrimitiveFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    this.descriptor = descriptor;
    this.messageBitIndex = messageBitIndex;
    this.builderBitIndex = builderBitIndex;
    this.context = context;
    this.variables = new HashMap<>();
    setPrimitiveVariables(
        descriptor,
        messageBitIndex,
        builderBitIndex,
        context.getFieldGeneratorInfo(descriptor),
        variables,
        context);
  }

  private void setPrimitiveVariables(
      FieldDescriptor descriptor,
      int messageBitIndex,
      int builderBitIndex,
      FieldGeneratorInfo info,
      Map<String, String> variables,
      Context context) {
    FieldCommon.setCommonFieldVariables(descriptor, info, variables);
    JavaType javaType = StringUtils.getJavaType(descriptor);

    variables.put("type", StringUtils.getPrimitiveTypeName(javaType));
    variables.put("boxed_type", StringUtils.boxedPrimitiveTypeName(javaType));
    variables.put("field_type", variables.get("type"));
    variables.put("default", StringUtils.defaultValue(descriptor));
    variables.put("default_init",
        StringUtils.isDefaultValueJavaDefault(descriptor)
            ? ""
            : "= " + StringUtils.defaultValue(descriptor));

    // Bit handling (simplified for now)
    variables.put("is_field_present_message", "true"); // TODO: Implement bit fields
    variables.put("set_has_field_bit_builder", "");
    variables.put("clear_has_field_bit_builder", "");

    variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
  }

  @Override
  public int getMessageBitIndex() {
    return messageBitIndex;
  }

  @Override
  public int getBuilderBitIndex() {
    return builderBitIndex;
  }

  @Override
  public int getNumBitsForMessage() {
    return 0; // TODO
  }

  @Override
  public int getNumBitsForBuilder() {
    return 0; // TODO
  }

  @Override
  public void generateInterfaceMembers(PrintWriter printer) {
     printer.println("  boolean has" + variables.get("capitalized_name") + "();");
     printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
  }

  @Override
  public void generateMembers(PrintWriter printer) {
     printer.println("  private " + variables.get("field_type") + " " + variables.get("name") + "_;");

     printer.println("  @java.lang.Override");
     printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
     // TODO: Implement presence check logic
     printer.println("    return false;");
     printer.println("  }");

     printer.println("  @java.lang.Override");
     printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
     printer.println("    return " + variables.get("name") + "_;");
     printer.println("  }");
  }

  @Override
  public void generateBuilderMembers(PrintWriter printer) {
     printer.println("  private " + variables.get("field_type") + " " + variables.get("name") + "_;");

     printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
     // TODO: Builder presence check
     printer.println("    return false;");
     printer.println("  }");

     printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
     printer.println("    return " + variables.get("name") + "_;");
     printer.println("  }");

     printer.println("  public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
     printer.println("    " + variables.get("name") + "_ = value;");
     // printer.println("    onChanged();"); // onChanged not yet implemented
     printer.println("    return this;");
     printer.println("  }");

     printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
     printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
     // printer.println("    onChanged();");
     printer.println("    return this;");
     printer.println("  }");
  }

  @Override
  public void generateInitializationCode(PrintWriter printer) {
     printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
  }

  @Override
  public void generateBuilderClearCode(PrintWriter printer) {
     printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
  }

  @Override
  public void generateMergingCode(PrintWriter printer) {
     // Placeholder
  }

  @Override
  public void generateBuildingCode(PrintWriter printer) {
      printer.println("      result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
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
      return variables.get("boxed_type");
  }
}
