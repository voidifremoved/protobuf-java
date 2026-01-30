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

public class RepeatedImmutablePrimitiveFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final Map<String, Object> variables;

  public RepeatedImmutablePrimitiveFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);
    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    String primitiveType = Helpers.getPrimitiveTypeName(javaType);
    String boxedType = Helpers.getBoxedPrimitiveTypeName(javaType);

    variables.put("type", primitiveType);
    variables.put("boxed_type", boxedType);
    variables.put("field_type", primitiveType);
    variables.put("list_type", getListType(javaType));
    variables.put("empty_list", getEmptyListCall(javaType));
    variables.put("repeated_get", "get" + variables.get("capitalized_name") + "List");
    variables.put("repeated_count", "get" + variables.get("capitalized_name") + "Count");
    variables.put("repeated_get_index", "get" + variables.get("capitalized_name"));

    variables.put("add_method_suffix", getAddMethodSuffix(javaType));
  }

  private String getListType(Helpers.JavaType type) {
    switch (type) {
      case INT: return "com.google.protobuf.Internal.IntList";
      case LONG: return "com.google.protobuf.Internal.LongList";
      case FLOAT: return "com.google.protobuf.Internal.FloatList";
      case DOUBLE: return "com.google.protobuf.Internal.DoubleList";
      case BOOLEAN: return "com.google.protobuf.Internal.BooleanList";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  private String getEmptyListCall(Helpers.JavaType type) {
    switch (type) {
      case INT: return "emptyIntList()";
      case LONG: return "emptyLongList()";
      case FLOAT: return "emptyFloatList()";
      case DOUBLE: return "emptyDoubleList()";
      case BOOLEAN: return "emptyBooleanList()";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  private String getAddMethodSuffix(Helpers.JavaType type) {
    switch (type) {
      case INT: return "Int";
      case LONG: return "Long";
      case FLOAT: return "Float";
      case DOUBLE: return "Double";
      case BOOLEAN: return "Boolean";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "java.util.List<$boxed_type$> $repeated_get$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "int $repeated_count$();\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "$type$ $repeated_get_index$(int index);\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private $list_type$ $name$_;\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.List<$boxed_type$> $repeated_get$() {\n" +
        "  return $name$_;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public int $repeated_count$() {\n" +
        "  return $name$_.size();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ $repeated_get_index$(int index) {\n" +
        "  return $name$_.get$add_method_suffix$(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    printer.emit(variables,
        "private void ensure$capitalized_name$IsMutable() {\n" +
        "  com.google.protobuf.Internal.ProtobufList<$boxed_type$> tmp = $name$_;\n" +
        "  if (!tmp.isModifiable()) {\n" +
        "    $name$_ = com.google.protobuf.GeneratedMessageLite.mutableCopy(tmp);\n" +
        "  }\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$(\n" +
        "    int index, $type$ value) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.set$add_method_suffix$(index, value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void add$capitalized_name$($type$ value) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add$add_method_suffix$(value);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<? extends $boxed_type$> values) {\n" +
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
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.List<$boxed_type$> $repeated_get$() {\n" +
        "  return java.util.Collections.unmodifiableList(\n" +
        "      instance.$repeated_get$());\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public int $repeated_count$() {\n" +
        "  return instance.$repeated_count$();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ $repeated_get_index$(int index) {\n" +
        "  return instance.$repeated_get_index$(index);\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder set$capitalized_name$(\n" +
        "    int index, $type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(index, value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder add$capitalized_name$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.add$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<? extends $boxed_type$> values) {\n" +
        "  copyOnWrite();\n" +
        "  instance.addAll$capitalized_name$(values);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
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
