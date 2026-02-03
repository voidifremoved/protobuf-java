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

public class RepeatedImmutableMessageFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final Map<String, Object> variables;

  public RepeatedImmutableMessageFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);
    String type = nameResolver.getImmutableClassName(descriptor.getMessageType());
    variables.put("type", type);
    variables.put("boxed_type", type);
    variables.put("field_type", type);
    variables.put("default", Helpers.immutableDefaultValue(descriptor, nameResolver, context.getOptions()));
    variables.put("tag", String.valueOf(Helpers.makeTag(descriptor.getNumber(), Helpers.getWireTypeForFieldType(descriptor.getType()))));
    variables.put("tag_size", String.valueOf(com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("required", descriptor.isRequired() ? "true" : "false");
    variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
    variables.put("null_check", "if (value == null) {\n  throw new NullPointerException();\n}\n");
    variables.put("group_or_message", Helpers.getType(descriptor) == FieldDescriptor.Type.GROUP ? "Group" : "Message");

    variables.put("empty_list", "emptyProtobufList()");
    variables.put("make_name_unmodifiable", variables.get("name") + "_.makeImmutable()");
    variables.put("repeated_get", variables.get("name") + "_.get");
    variables.put("repeated_add", variables.get("name") + "_.add");
    variables.put("repeated_set", variables.get("name") + "_.set");
    variables.put("visit_type", "ByteString");
    variables.put("field_list_type", "com.google.protobuf.Internal.ProtobufList<" + type + ">");

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
        "$deprecation$java.util.List<$type$> ${$get$capitalized_name$List$}$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "$deprecation$$type$ ${$get$capitalized_name$$}$(int index);\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "$deprecation$int ${$get$capitalized_name$Count$}$();\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private $field_list_type$ $name$_;\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.util.List<$type$> ${$get$capitalized_name$List$}$() {\n" +
        "  return $name$_;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "public java.util.List<? extends $type$OrBuilder> \n" +
        "    ${$get$capitalized_name$OrBuilderList$}$() {\n" +
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
        "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
        "  return $name$_.get(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "public $type$OrBuilder ${$get$capitalized_name$OrBuilder$}$(int index) {\n" +
        "  return $name$_.get(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

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
        "    int index, $type$ value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.set(index, value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$($type$ value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add(value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$(\n" +
        "    int index, $type$ value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add(index, value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<? extends $type$> values) {\n" +
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

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void remove$capitalized_name$(int index) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.remove(index);\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.util.List<$type$> ${$get$capitalized_name$List$}$() {\n" +
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
        "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
        "  return instance.get$capitalized_name$(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    int index, $type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(index, value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    int index, $type$.Builder builderForValue) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(index,\n" +
        "      builderForValue.build());\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$(\n" +
        "    int index, $type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(index, value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$(\n" +
        "    $type$.Builder builderForValue) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(builderForValue.build());\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$(\n" +
        "    int index, $type$.Builder builderForValue) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(index,\n" +
        "      builderForValue.build());\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$addAll$capitalized_name$$}$(\n" +
        "    java.lang.Iterable<? extends $type$> values) {\n" +
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

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$remove$capitalized_name$$}$(int index) {\n" +
        "  copyOnWrite();\n" +
        "  instance.remove$capitalized_name$(index);\n" +
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
    return (String) variables.get("boxed_type");
  }
}
