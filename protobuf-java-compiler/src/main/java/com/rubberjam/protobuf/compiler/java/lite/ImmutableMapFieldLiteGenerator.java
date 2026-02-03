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

public class ImmutableMapFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final Map<String, Object> variables;

  public ImmutableMapFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);

    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");

    variables.put("key_type", Helpers.getJavaType(keyField) == Helpers.JavaType.STRING ? "java.lang.String" :
        (Helpers.getJavaType(keyField) == Helpers.JavaType.INT ? "java.lang.Integer" :
        (Helpers.getJavaType(keyField) == Helpers.JavaType.LONG ? "java.lang.Long" :
        (Helpers.getJavaType(keyField) == Helpers.JavaType.BOOLEAN ? "java.lang.Boolean" :
        ""))));

    variables.put("value_type", Helpers.getJavaType(valueField) == Helpers.JavaType.STRING ? "java.lang.String" :
        (Helpers.getJavaType(valueField) == Helpers.JavaType.INT ? "java.lang.Integer" :
        (Helpers.getJavaType(valueField) == Helpers.JavaType.LONG ? "java.lang.Long" :
        (Helpers.getJavaType(valueField) == Helpers.JavaType.BOOLEAN ? "java.lang.Boolean" :
        (Helpers.getJavaType(valueField) == Helpers.JavaType.MESSAGE ?
            nameResolver.getImmutableClassName(valueField.getMessageType()) :
            (Helpers.getJavaType(valueField) == Helpers.JavaType.ENUM ?
                nameResolver.getImmutableClassName(valueField.getEnumType()) :
                ""))))));

    variables.put("key_wire_type",
        "com.google.protobuf.WireFormat.FieldType." + Helpers.getFieldTypeName(keyField.getType()));
    variables.put("value_wire_type",
        "com.google.protobuf.WireFormat.FieldType." + Helpers.getFieldTypeName(valueField.getType()));

    variables.put("key_default_value", Helpers.defaultValue(keyField, true, nameResolver, context.getOptions()));
    variables.put("value_default_value", Helpers.defaultValue(valueField, true, nameResolver, context.getOptions()));

    variables.put("default_entry", "$capitalized_name$DefaultEntryHolder.defaultEntry");
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
     printer.emit(variables,
         "int ${$get$capitalized_name$Count$}$();\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
     printer.emit(variables,
         "boolean ${$contains$capitalized_name$$}$(\n" +
         "    $key_type$ key);\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Deprecated\n" +
         "java.util.Map<$key_type$, $value_type$>\n" +
         "    ${$get$capitalized_name$$}$();\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "java.util.Map<$key_type$, $value_type$>\n" +
         "    ${$get$capitalized_name$Map$}$();\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "$value_type$ ${$get$capitalized_name$OrDefault$}$(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue);\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "$value_type$ ${$get$capitalized_name$OrThrow$}$(\n" +
         "    $key_type$ key);\n");
     // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
     printer.emit(variables,
         "private static final class $capitalized_name$DefaultEntryHolder {\n" +
         "  static final com.google.protobuf.MapEntryLite<\n" +
         "      $key_type$, $value_type$> defaultEntry =\n" +
         "          com.google.protobuf.MapEntryLite\n" +
         "          .<$key_type$, $value_type$>newDefaultInstance(\n" +
         "              $key_wire_type$,\n" +
         "              $key_default_value$,\n" +
         "              $value_wire_type$,\n" +
         "              $value_default_value$);\n" +
         "}\n");

     printer.emit(variables,
         "private com.google.protobuf.MapFieldLite<\n" +
         "    $key_type$, $value_type$> $name$_ =\n" +
         "        com.google.protobuf.MapFieldLite.emptyMapField();\n" +
         "private com.google.protobuf.MapFieldLite<$key_type$, $value_type$>\n" +
         "internalGet$capitalized_name$() {\n" +
         "  return $name$_;\n" +
         "}\n" +
         "private com.google.protobuf.MapFieldLite<$key_type$, $value_type$>\n" +
         "internalGetMutable$capitalized_name$() {\n" +
         "  if (!$name$_.isMutable()) {\n" +
         "    $name$_ = $name$_.mutableCopy();\n" +
         "  }\n" +
         "  return $name$_;\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public int ${$get$capitalized_name$Count$}$() {\n" +
         "  return internalGet$capitalized_name$().size();\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public boolean ${$contains$capitalized_name$$}$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  return internalGet$capitalized_name$().containsKey(key);\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "@java.lang.Deprecated\n" +
         "public java.util.Map<$key_type$, $value_type$> ${$get$capitalized_name$$}$() {\n" +
         "  return get$capitalized_name$Map();\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.Map<$key_type$, $value_type$> ${$get$capitalized_name$Map$}$() {\n" +
         "  return java.util.Collections.unmodifiableMap(\n" +
         "      internalGet$capitalized_name$());\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $value_type$ ${$get$capitalized_name$OrDefault$}$(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$();\n" +
         "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $value_type$ ${$get$capitalized_name$OrThrow$}$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$();\n" +
         "  if (!map.containsKey(key)) {\n" +
         "    throw new java.lang.IllegalArgumentException();\n" +
         "  }\n" +
         "  return map.get(key);\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     // Private mutator for Builder to use
     printer.emit(variables,
         "private java.util.Map<$key_type$, $value_type$>\n" +
         "getMutable$capitalized_name$Map() {\n" +
         "  return internalGetMutable$capitalized_name$();\n" +
         "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public int ${$get$capitalized_name$Count$}$() {\n" +
         "  return instance.get$capitalized_name$Map().size();\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public boolean ${$contains$capitalized_name$$}$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  return instance.get$capitalized_name$Map().containsKey(key);\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "@java.lang.Deprecated\n" +
         "public java.util.Map<$key_type$, $value_type$> ${$get$capitalized_name$$}$() {\n" +
         "  return get$capitalized_name$Map();\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.Map<$key_type$, $value_type$> ${$get$capitalized_name$Map$}$() {\n" +
         "  return java.util.Collections.unmodifiableMap(\n" +
         "      instance.get$capitalized_name$Map());\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $value_type$ ${$get$capitalized_name$OrDefault$}$(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      instance.get$capitalized_name$Map();\n" +
         "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $value_type$ ${$get$capitalized_name$OrThrow$}$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      instance.get$capitalized_name$Map();\n" +
         "  if (!map.containsKey(key)) {\n" +
         "    throw new java.lang.IllegalArgumentException();\n" +
         "  }\n" +
         "  return map.get(key);\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.MAP_ENTRY_ADDER, context.getOptions());
     printer.emit(variables,
         "public Builder ${$put$capitalized_name$$}$(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ value) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  if (value == null) { throw new NullPointerException(); }\n" +
         "  copyOnWrite();\n" +
         "  instance.getMutable$capitalized_name$Map().put(key, value);\n" +
         "  return this;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.MAP_MULTI_ADDER, context.getOptions());
     printer.emit(variables,
         "public Builder ${$putAll$capitalized_name$$}$(\n" +
         "    java.util.Map<$key_type$, $value_type$> values) {\n" +
         "  copyOnWrite();\n" +
         "  instance.getMutable$capitalized_name$Map().putAll(values);\n" +
         "  return this;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions());
     printer.emit(variables,
         "public Builder ${$clear$capitalized_name$$}$() {\n" +
         "  copyOnWrite();\n" +
         "  instance.getMutable$capitalized_name$Map().clear();\n" +
         "  return this;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.MAP_ENTRY_REMOVER, context.getOptions());
     printer.emit(variables,
         "public Builder ${$remove$capitalized_name$$}$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  copyOnWrite();\n" +
         "  instance.getMutable$capitalized_name$Map().remove(key);\n" +
         "  return this;\n" +
         "}\n");
     // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    printer.emit(variables, "\"$name$_\",\n");
    printer.emit(variables, "$default_entry$,\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    printer.emit(variables, "$name$_ = com.google.protobuf.MapFieldLite.emptyMapField();\n");
  }

  @Override
  public String getBoxedType() {
    return (String) variables.get("value_type"); // Map field doesn't really have a single boxed type, but consistent with return null?
    // Actually ImmutableFieldLiteGenerator interface might not need this for Maps.
    // getBoxedType is used for generic services or similar?
    // Default implementation returns null if not primitive?
  }
}
