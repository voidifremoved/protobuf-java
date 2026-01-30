package com.rubberjam.protobuf.another.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.FieldCommon;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImmutableStringFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  protected final FieldDescriptor descriptor;
  protected final Map<String, Object> variables;
  protected final int messageBitIndex;
  protected final Context context;
  protected final ClassNameResolver nameResolver;

  public ImmutableStringFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.messageBitIndex = messageBitIndex;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);
    variables.put("default", Helpers.immutableDefaultValue(descriptor, nameResolver, context.getOptions()));
    variables.put("default_init",
        "= " + Helpers.immutableDefaultValue(descriptor, nameResolver, context.getOptions()));
    variables.put("tag", String.valueOf(Helpers.makeTag(descriptor.getNumber(), Helpers.getWireTypeForFieldType(descriptor.getType()))));
    variables.put("tag_size", String.valueOf(com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("required", descriptor.isRequired() ? "true" : "false");
    variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
    variables.put("null_check", "  java.lang.Class<?> valueClass = value.getClass();\n");

    if (InternalHelpers.hasHasbit(descriptor)) {
      variables.put("set_has_field_bit_message", Helpers.generateSetBit(messageBitIndex) + ";");
      variables.put("clear_has_field_bit_message", Helpers.generateClearBit(messageBitIndex) + ";");
      variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
    } else {
      variables.put("set_has_field_bit_message", "");
      variables.put("clear_has_field_bit_message", "");
      variables.put("is_field_present_message", "!" + variables.get("name") + "_.isEmpty()");
    }

    variables.put("{", "");
    variables.put("}", "");
  }

  @Override
  public int getNumBitsForMessage() {
    return InternalHelpers.hasHasbit(descriptor) ? 1 : 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    if (descriptor.hasPresence()) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables, "$deprecation$boolean has$capitalized_name$();\n");
    }
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables, "$deprecation$java.lang.String ${$get$capitalized_name$$}$();\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables, "$deprecation$com.google.protobuf.ByteString ${$get$capitalized_name$Bytes$}$();\n");
    printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private java.lang.String $name$_;\n");

    if (descriptor.hasPresence()) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "$deprecation$public boolean ${$has$capitalized_name$$}$() {\n" +
          "  return $is_field_present_message$;\n" +
          "}\n");
      printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$() {\n" +
        "  return $name$_;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$() {\n" +
        "  return com.google.protobuf.ByteString.copyFromUtf8($name$_);\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$(\n" +
        "    java.lang.String value) {\n" +
        "$null_check$" +
        "  $set_has_field_bit_message$\n" +
        "  $name$_ = value;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void clear$capitalized_name$() {\n" +
        "  $clear_has_field_bit_message$\n" +
        "  $name$_ = getDefaultInstance().get$capitalized_name$();\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$Bytes(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  checkByteStringIsUtf8(value);\n" +
        "  $name$_ = value.toStringUtf8();\n" +
        "  $set_has_field_bit_message$\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    if (descriptor.hasPresence()) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "$deprecation$public boolean ${$has$capitalized_name$$}$() {\n" +
          "  return instance.has$capitalized_name$();\n" +
          "}\n");
      printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$() {\n" +
        "  return instance.get$capitalized_name$();\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$() {\n" +
        "  return instance.get$capitalized_name$Bytes();\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    java.lang.String value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$clear$capitalized_name$$}$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$Bytes$}$(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$Bytes(value);\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    if (InternalHelpers.hasHasbit(descriptor)) {
      Helpers.writeIntToUtf16CharSequence(messageBitIndex, output);
    }
    printer.emit(variables, "\"$name$_\",\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    printer.emit(variables, "$name$_ = $default$;\n");
  }

  @Override
  public String getBoxedType() {
    return "java.lang.String";
  }
}
