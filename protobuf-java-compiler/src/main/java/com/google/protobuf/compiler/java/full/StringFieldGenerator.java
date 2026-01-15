package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldCommon;
import com.google.protobuf.compiler.java.FieldGeneratorInfo;
import com.google.protobuf.compiler.java.StringUtils;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class StringFieldGenerator extends ImmutableFieldGenerator {
  private final FieldDescriptor descriptor;
  private final int messageBitIndex;
  private final int builderBitIndex;
  private final Context context;
  private final Map<String, String> variables;

  public StringFieldGenerator(
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

    variables.put("default", StringUtils.defaultValue(descriptor));
    variables.put("default_init", "= " + StringUtils.defaultValue(descriptor));

    // Simplified logic for now
    variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
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
     printer.println("  java.lang.String get" + variables.get("capitalized_name") + "();");
     printer.println("  com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes();");
  }

  @Override
  public void generateMembers(PrintWriter printer) {
     printer.println("  private volatile java.lang.Object " + variables.get("name") + "_ = " + variables.get("default") + ";");

     printer.println("  @java.lang.Override");
     printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "() {");
     printer.println("    java.lang.Object ref = " + variables.get("name") + "_;");
     printer.println("    if (ref instanceof java.lang.String) {");
     printer.println("      return (java.lang.String) ref;");
     printer.println("    } else {");
     printer.println("      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;");
     printer.println("      java.lang.String s = bs.toStringUtf8();");
     printer.println("      " + variables.get("name") + "_ = s;");
     printer.println("      return s;");
     printer.println("    }");
     printer.println("  }");

     printer.println("  @java.lang.Override");
     printer.println("  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes() {");
     printer.println("    java.lang.Object ref = " + variables.get("name") + "_;");
     printer.println("    if (ref instanceof java.lang.String) {");
     printer.println("      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);");
     printer.println("      " + variables.get("name") + "_ = b;");
     printer.println("      return b;");
     printer.println("    } else {");
     printer.println("      return (com.google.protobuf.ByteString) ref;");
     printer.println("    }");
     printer.println("  }");
  }

  @Override
  public void generateBuilderMembers(PrintWriter printer) {
     printer.println("  private java.lang.Object " + variables.get("name") + "_ = " + variables.get("default") + ";");

     printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "() {");
     printer.println("    java.lang.Object ref = " + variables.get("name") + "_;");
     printer.println("    if (!(ref instanceof java.lang.String)) {");
     printer.println("      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;");
     printer.println("      java.lang.String s = bs.toStringUtf8();");
     printer.println("      " + variables.get("name") + "_ = s;");
     printer.println("      return s;");
     printer.println("    } else {");
     printer.println("      return (java.lang.String) ref;");
     printer.println("    }");
     printer.println("  }");

     printer.println("  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes() {");
     printer.println("    java.lang.Object ref = " + variables.get("name") + "_;");
     printer.println("    if (ref instanceof String) {");
     printer.println("      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((java.lang.String) ref);");
     printer.println("      " + variables.get("name") + "_ = b;");
     printer.println("      return b;");
     printer.println("    } else {");
     printer.println("      return (com.google.protobuf.ByteString) ref;");
     printer.println("    }");
     printer.println("  }");

     printer.println("  public Builder set" + variables.get("capitalized_name") + "(java.lang.String value) {");
     printer.println("    if (value == null) { throw new NullPointerException(); }");
     printer.println("    " + variables.get("name") + "_ = value;");
     // printer.println("    onChanged();");
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
      return "java.lang.String";
  }

  public static class RepeatedStringFieldGenerator extends ImmutableFieldGenerator {
    private final FieldDescriptor descriptor;
    private final int messageBitIndex;
    private final int builderBitIndex;
    private final Context context;
    private final Map<String, String> variables;

    public RepeatedStringFieldGenerator(
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
      variables.put("empty_list", "com.google.protobuf.LazyStringArrayList.emptyList()");
      variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
    }

    @Override
    public int getMessageBitIndex() { return 0; }
    @Override
    public int getBuilderBitIndex() { return 0; }
    @Override
    public int getNumBitsForMessage() { return 0; }
    @Override
    public int getNumBitsForBuilder() { return 0; }

    @Override
    public void generateInterfaceMembers(PrintWriter printer) {
      printer.println("  java.util.List<java.lang.String> get" + variables.get("capitalized_name") + "List();");
      printer.println("  int get" + variables.get("capitalized_name") + "Count();");
      printer.println("  java.lang.String get" + variables.get("capitalized_name") + "(int index);");
      printer.println("  com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes(int index);");
    }

    @Override
    public void generateMembers(PrintWriter printer) {
      printer.println("  private com.google.protobuf.LazyStringArrayList " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");

      printer.println("  @java.lang.Override");
      printer.println("  public com.google.protobuf.ProtocolStringList get" + variables.get("capitalized_name") + "List() {");
      printer.println("    return " + variables.get("name") + "_;");
      printer.println("  }");

      printer.println("  @java.lang.Override");
      printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
      printer.println("    return " + variables.get("name") + "_.size();");
      printer.println("  }");

      printer.println("  @java.lang.Override");
      printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "(int index) {");
      printer.println("    return " + variables.get("name") + "_.get(index);");
      printer.println("  }");

      printer.println("  @java.lang.Override");
      printer.println("  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes(int index) {");
      printer.println("    return " + variables.get("name") + "_.getByteString(index);");
      printer.println("  }");
    }

    @Override
    public void generateBuilderMembers(PrintWriter printer) {
      printer.println("  private com.google.protobuf.LazyStringArrayList " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");

      printer.println("  private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
      printer.println("    if (!" + variables.get("name") + "_.isModifiable()) {");
      printer.println("      " + variables.get("name") + "_ = new com.google.protobuf.LazyStringArrayList(" + variables.get("name") + "_);");
      printer.println("    }");
      printer.println("  }");

      printer.println("  public com.google.protobuf.ProtocolStringList get" + variables.get("capitalized_name") + "List() {");
      printer.println("    " + variables.get("name") + "_.makeImmutable();");
      printer.println("    return " + variables.get("name") + "_;");
      printer.println("  }");

      printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
      printer.println("    return " + variables.get("name") + "_.size();");
      printer.println("  }");

      printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "(int index) {");
      printer.println("    return " + variables.get("name") + "_.get(index);");
      printer.println("  }");

      printer.println("  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes(int index) {");
      printer.println("    return " + variables.get("name") + "_.getByteString(index);");
      printer.println("  }");

      printer.println("  public Builder set" + variables.get("capitalized_name") + "(int index, java.lang.String value) {");
      printer.println("    if (value == null) { throw new NullPointerException(); }");
      printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
      printer.println("    " + variables.get("name") + "_.set(index, value);");
      printer.println("    // onChanged();");
      printer.println("    return this;");
      printer.println("  }");

      printer.println("  public Builder add" + variables.get("capitalized_name") + "(java.lang.String value) {");
      printer.println("    if (value == null) { throw new NullPointerException(); }");
      printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
      printer.println("    " + variables.get("name") + "_.add(value);");
      printer.println("    // onChanged();");
      printer.println("    return this;");
      printer.println("  }");

      printer.println("  public Builder addAll" + variables.get("capitalized_name") + "(java.lang.Iterable<java.lang.String> values) {");
      printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
      printer.println("    com.google.protobuf.AbstractMessageLite.Builder.addAll(values, " + variables.get("name") + "_);");
      printer.println("    // onChanged();");
      printer.println("    return this;");
      printer.println("  }");

      printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
      printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
      printer.println("    return this;");
      printer.println("  }");

      printer.println("  public Builder add" + variables.get("capitalized_name") + "Bytes(com.google.protobuf.ByteString value) {");
      printer.println("    if (value == null) { throw new NullPointerException(); }");
      printer.println("    // checkByteStringIsUtf8(value);");
      printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
      printer.println("    " + variables.get("name") + "_.add(value);");
      printer.println("    // onChanged();");
      printer.println("    return this;");
      printer.println("  }");
    }

    @Override
    public void generateInitializationCode(PrintWriter printer) {
      printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
    }

    @Override
    public void generateBuilderClearCode(PrintWriter printer) {
      printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
    }

    @Override
    public void generateMergingCode(PrintWriter printer) {
       // Placeholder
    }

    @Override
    public void generateBuildingCode(PrintWriter printer) {
        printer.println("      if (true) { // TODO: check if mutable");
        printer.println("        " + variables.get("name") + "_.makeImmutable();");
        printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
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
        return "java.lang.String";
    }
  }
}
