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

public class RepeatedImmutableEnumFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final Map<String, Object> variables;

  public RepeatedImmutableEnumFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);
    String type = nameResolver.getImmutableClassName(descriptor.getEnumType());
    variables.put("type", type);
    variables.put("boxed_type", type);
    variables.put("field_type", type);
    variables.put("default", Helpers.immutableDefaultValue(descriptor, nameResolver, context.getOptions()));
    variables.put("tag", String.valueOf(Helpers.makeTag(descriptor.getNumber(), Helpers.getWireTypeForFieldType(descriptor.getType()))));
    variables.put("tag_size", String.valueOf(com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("required", descriptor.isRequired() ? "true" : "false");
    variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
    variables.put("null_check", "if (value == null) {\n  throw new NullPointerException();\n}\n");
    variables.put("int_type", "int");
    variables.put("is_closed_enum", !InternalHelpers.supportUnknownEnumValue(descriptor));

    variables.put("empty_list", "emptyIntList()");
    variables.put("make_name_unmodifiable", variables.get("name") + "_.makeImmutable()");
    variables.put("repeated_get", variables.get("name") + "_.getInt");
    variables.put("repeated_add", variables.get("name") + "_.addInt");
    variables.put("repeated_set", variables.get("name") + "_.setInt");
    variables.put("visit_type", "Int");
    variables.put("visit_type_list", "visitIntList");

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        variables.put("field_list_type", "com.google.protobuf.Internal.IntList");
    } else {
        variables.put("field_list_type", "com.google.protobuf.Internal.ProtobufList<" + type + ">");
        variables.put("empty_list", "emptyProtobufList()");
        variables.put("repeated_get", variables.get("name") + "_.get");
        variables.put("repeated_add", variables.get("name") + "_.add");
        variables.put("repeated_set", variables.get("name") + "_.set");
        variables.put("visit_type", "ByteString");
    }

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
        "$deprecation$java.util.List<$boxed_type$> ${$get$capitalized_name$List$}$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "$deprecation$int ${$get$capitalized_name$Count$}$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "$deprecation$$type$ ${$get$capitalized_name$$}$(int index);\n");
    // printer.annotate("{", "}", descriptor);

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
      printer.emit(variables,
          "$deprecation$java.util.List<java.lang.Integer>\n" +
          "${$get$capitalized_name$ValueList$}$();\n");
      // printer.annotate("{", "}", descriptor);

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
      printer.emit(variables,
          "$deprecation$int ${$get$capitalized_name$Value$}$(int index);\n");
      // printer.annotate("{", "}", descriptor);
    }
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private $field_list_type$ $name$_;\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<$boxed_type$>\n" +
            "    ${$get$capitalized_name$List$}$() {\n" +
            "  return new com.google.protobuf.Internal.IntListAdapter<$boxed_type$>(\n" +
            "      $name$_, $name$_converter_);\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<$boxed_type$> ${$get$capitalized_name$List$}$() {\n" +
            "  return $name$_;\n" +
            "}\n");
    }
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public int ${$get$capitalized_name$Count$}$() {\n" +
        "  return $name$_.size();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
            "  return $name$_converter_.convert($name$_.getInt(index));\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
            "  return $name$_.get(index);\n" +
            "}\n");
    }
    // printer.annotate("{", "}", descriptor);

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<java.lang.Integer>\n" +
            "    ${$get$capitalized_name$ValueList$}$() {\n" +
            "  return $name$_;\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public int ${$get$capitalized_name$Value$}$(int index) {\n" +
            "  return $name$_.getInt(index);\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.print(variables,
            "private static final com.google.protobuf.Internal.ListAdapter.Converter<\n" +
            "    java.lang.Integer, $boxed_type$> $name$_converter_ =\n" +
            "        new com.google.protobuf.Internal.ListAdapter.Converter<\n" +
            "            java.lang.Integer, $boxed_type$>() {\n" +
            "          @java.lang.Override\n" +
            "          public $boxed_type$ convert(java.lang.Integer from) {\n" +
            "            $boxed_type$ result = $boxed_type$.forNumber(from);\n" +
            "            return result == null ? $boxed_type$.UNRECOGNIZED : result;\n" +
            "          }\n" +
            "        };\n");
    }

    if (descriptor.isPacked() && context.hasGeneratedMethods(descriptor.getContainingType())) {
      printer.emit(variables, "private int $name$MemoizedSerializedSize = -1;\n");
    }

    printer.emit(variables,
        "private void ensure$capitalized_name$IsMutable() {\n" +
        "  $field_list_type$ tmp = $name$_;\n" +
        "  if (!tmp.isModifiable()) {\n" +
        "    $name$_ =\n" +
        "        com.google.protobuf.GeneratedMessageLite.mutableCopy(tmp);\n" +
        "  }\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$(\n" +
        "    int index, $type$ value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n");
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables, "  $name$_.setInt(index, value.getNumber());\n");
    } else {
        printer.emit(variables, "  $name$_.set(index, value);\n");
    }
    printer.emit(variables, "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$($type$ value) {\n" +
        "$null_check$" +
        "  ensure$capitalized_name$IsMutable();\n");
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables, "  $name$_.addInt(value.getNumber());\n");
    } else {
        printer.emit(variables, "  $name$_.add(value);\n");
    }
    printer.emit(variables, "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<? extends $boxed_type$> values) {\n" +
        "  ensure$capitalized_name$IsMutable();\n");
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "  for ($boxed_type$ value : values) {\n" +
            "    $name$_.addInt(value.getNumber());\n" +
            "  }\n");
    } else {
        printer.emit(variables,
            "  com.google.protobuf.AbstractMessageLite.addAll(\n" +
            "      values, $name$_);\n");
    }
    printer.emit(variables, "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void clear$capitalized_name$() {\n" +
        "  $name$_ = $empty_list$;\n" +
        "}\n");

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
            context.getOptions(), false, false, true);
        printer.emit(variables,
            "private void set$capitalized_name$Value(\n" +
            "    int index, int value) {\n" +
            "  ensure$capitalized_name$IsMutable();\n" +
            "  $name$_.setInt(index, value);\n" +
            "}\n");

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
            context.getOptions(), false, false, true);
        printer.emit(variables,
            "private void add$capitalized_name$Value(int value) {\n" +
            "  ensure$capitalized_name$IsMutable();\n" +
            "  $name$_.addInt(value);\n" +
            "}\n");

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
            context.getOptions(), false, false, true);
        printer.emit(variables,
            "private void addAll$capitalized_name$Value(\n" +
            "    java.lang.Iterable<java.lang.Integer> values) {\n" +
            "  ensure$capitalized_name$IsMutable();\n" +
            "  for (int value : values) {\n" +
            "    $name$_.addInt(value);\n" +
            "  }\n" +
            "}\n");
    }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<$boxed_type$> ${$get$capitalized_name$List$}$() {\n" +
            "  return new com.google.protobuf.Internal.IntListAdapter<$boxed_type$>(\n" +
            "      instance.get$capitalized_name$List(), $name$_converter_);\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<$boxed_type$> ${$get$capitalized_name$List$}$() {\n" +
            "  return java.util.Collections.unmodifiableList(\n" +
            "      instance.get$capitalized_name$List());\n" +
            "}\n");
    }
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public int ${$get$capitalized_name$Count$}$() {\n" +
        "  return instance.get$capitalized_name$Count();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
            "  return $name$_converter_.convert(instance.get$capitalized_name$Value(index));\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public $type$ ${$get$capitalized_name$$}$(int index) {\n" +
            "  return instance.get$capitalized_name$(index);\n" +
            "}\n");
    }
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

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$add$capitalized_name$$}$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$addAll$capitalized_name$$}$(\n" +
        "    java.lang.Iterable<? extends $boxed_type$> values) {\n" +
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

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public java.util.List<java.lang.Integer>\n" +
            "    ${$get$capitalized_name$ValueList$}$() {\n" +
            "  return java.util.Collections.unmodifiableList(\n" +
            "      instance.get$capitalized_name$ValueList());\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
        printer.emit(variables,
            "@java.lang.Override\n" +
            "$deprecation$public int ${$get$capitalized_name$Value$}$(int index) {\n" +
            "  return instance.get$capitalized_name$Value(index);\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
            context.getOptions(), true);
        printer.emit(variables,
            "$deprecation$public Builder ${$set$capitalized_name$Value$}$(\n" +
            "    int index, int value) {\n" +
            "  copyOnWrite();\n" +
            "  instance.set$capitalized_name$Value(index, value);\n" +
            "  return this;\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
            context.getOptions(), true);
        printer.emit(variables,
            "$deprecation$public Builder ${$add$capitalized_name$Value$}$(int value) {\n" +
            "  copyOnWrite();\n" +
            "  instance.add$capitalized_name$Value(value);\n" +
            "  return this;\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);

        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
            context.getOptions(), true);
        printer.emit(variables,
            "$deprecation$public Builder ${$addAll$capitalized_name$Value$}$(\n" +
            "    java.lang.Iterable<java.lang.Integer> values) {\n" +
            "  copyOnWrite();\n" +
            "  instance.addAll$capitalized_name$Value(values);\n" +
            "  return this;\n" +
            "}\n");
        // printer.annotate("{", "}", descriptor);
    }
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
