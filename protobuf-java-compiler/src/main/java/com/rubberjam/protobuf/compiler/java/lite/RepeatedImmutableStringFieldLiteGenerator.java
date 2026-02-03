package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepeatedImmutableStringFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final Map<String, Object> variables;

  public RepeatedImmutableStringFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
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
    variables.put("null_check", "if (value == null) {\n  throw new NullPointerException();\n}\n");

    variables.put("empty_list", "com.google.protobuf.GeneratedMessageLite.emptyProtobufList()");
    variables.put("make_name_unmodifiable", variables.get("name") + "_.makeImmutable()");
    variables.put("repeated_get", variables.get("name") + "_.get");
    variables.put("repeated_add", variables.get("name") + "_.add");
    variables.put("repeated_set", variables.get("name") + "_.set");
    variables.put("visit_type", "ByteString"); // Not exactly sure what visit_type matches in C++ logic for non-primitive repeated enums in lite?
    // In C++ RepeatedImmutableStringFieldLiteGenerator variables:
    // variables["field_list_type"] = "com.google.protobuf.Internal.ProtobufList<java.lang.String>";
    variables.put("field_list_type", "com.google.protobuf.Internal.ProtobufList<java.lang.String>");

    variables.put("{", "");
    variables.put("}", "");
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "$deprecation$java.util.List<java.lang.String> ${$get$capitalized_name$List$}$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "$deprecation$int ${$get$capitalized_name$Count$}$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "$deprecation$java.lang.String ${$get$capitalized_name$$}$(int index);\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "$deprecation$com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$(int index);\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private $field_list_type$ $name$_;\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.util.List<java.lang.String> ${$get$capitalized_name$List$}$() {\n" +
        "  return $name$_;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public int ${$get$capitalized_name$Count$}$() {\n" +
        "  return $name$_.size();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$(int index) {\n" +
        "  return $name$_.get(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$(int index) {\n" +
        "  return com.google.protobuf.ByteString.copyFromUtf8(\n" +
        "      $name$_.get(index));\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    if (descriptor.isPacked() && context.hasGeneratedMethods(descriptor.getContainingType())) {
      printer.emit(variables, "private int $name$MemoizedSerializedSize = -1;\n");
    }

    printer.emit(variables,
        "private void ensure$capitalized_name$IsMutable() {\n" +
        "  $field_list_type$ tmp = $name$_;\n" +
        "  if (!tmp.isModifiable()) {\n" +
        "    $name$_ =\n" +
        "        com.google.protobuf.GeneratedMessageLite.mutableCopy(tmp);\n" +
        "   }\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$(\n" +
        "    int index, java.lang.String value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.set(index, value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$(java.lang.String value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add(value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<java.lang.String> values) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  com.google.protobuf.AbstractMessageLite.addAll(\n" +
        "      values, $name$_);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void clear$capitalized_name$() {\n" +
        "  $name$_ = $empty_list$;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$Bytes(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  checkByteStringIsUtf8(value);\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add(value.toStringUtf8());\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.util.List<java.lang.String> ${$get$capitalized_name$List$}$() {\n" +
        "  return java.util.Collections.unmodifiableList(\n" +
        "      instance.get$capitalized_name$List());\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public int ${$get$capitalized_name$Count$}$() {\n" +
        "  return instance.get$capitalized_name$Count();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$(int index) {\n" +
        "  return instance.get$capitalized_name$(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$(int index) {\n" +
        "  return instance.get$capitalized_name$Bytes(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    int index, java.lang.String value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(index, value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$(java.lang.String value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$addAll$capitalized_name$$}$(\n" +
        "    java.lang.Iterable<java.lang.String> values) {\n" +
        "  copyOnWrite();\n" +
        "  instance.addAll$capitalized_name$(values);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$clear$capitalized_name$$}$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$Bytes$}$(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$Bytes(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    printer.emit(variables, "\"$name$_\",\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    printer.emit(variables, "$name$_ = $empty_list$;\n");
  }

  @Override
  public String getBoxedType() {
    return "java.lang.String";
  }
}
