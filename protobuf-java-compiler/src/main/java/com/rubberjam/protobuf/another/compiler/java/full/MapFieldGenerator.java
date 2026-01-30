package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating map fields.
 * Ported from java/full/map_field.cc.
 */
public class MapFieldGenerator extends ImmutableFieldGenerator {
  public MapFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

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
            context.getNameResolver().getClassName(valueField.getMessageType(), true) :
            (Helpers.getJavaType(valueField) == Helpers.JavaType.ENUM ?
                context.getNameResolver().getClassName(valueField.getEnumType(), true) :
                ""))))));

    variables.put("key_wire_type",
        "com.google.protobuf.WireFormat.FieldType." + Helpers.getFieldTypeName(keyField.getType()));
    variables.put("value_wire_type",
        "com.google.protobuf.WireFormat.FieldType." + Helpers.getFieldTypeName(valueField.getType()));

    variables.put("key_default_value", Helpers.defaultValue(keyField, true, context.getNameResolver(), context.getOptions()));
    variables.put("value_default_value", Helpers.defaultValue(valueField, true, context.getNameResolver(), context.getOptions()));

    variables.put("descriptor_accessor",
        context.getNameResolver().getClassName(descriptor.getMessageType(), true) + ".getDescriptor()");

    // Default value handling for Map?
    // Map fields don't have explicit defaults in proto syntax, they are empty by default.
    variables.put("default_entry", descriptor.getMessageType().getName() + ".getDefaultInstance()");

    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
     printer.emit(variables,
         "int get$capitalized_name$Count();\n" +
         "boolean contains$capitalized_name$(\n" +
         "    $key_type$ key);\n" +
         "@java.lang.Deprecated\n" +
         "java.util.Map<$key_type$, $value_type$>\n" +
         "    get$capitalized_name$();\n" +
         "java.util.Map<$key_type$, $value_type$>\n" +
         "    get$capitalized_name$Map();\n" +
         "$value_type$ get$capitalized_name$OrDefault(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue);\n" +
         "$value_type$ get$capitalized_name$OrThrow(\n" +
         "    $key_type$ key);\n");
  }

  @Override
  public void generateMembers(Printer printer) {
     printer.emit(variables,
         "private static final class $capitalized_name$DefaultEntryHolder {\n" +
         "  static final com.google.protobuf.MapEntry<\n" +
         "      $key_type$, $value_type$> defaultEntry =\n" +
         "          com.google.protobuf.MapEntry\n" +
         "          .<$key_type$, $value_type$>newDefaultInstance(\n" +
         "              $descriptor_accessor$,\n" +
         "              $key_wire_type$,\n" +
         "              $key_default_value$,\n" +
         "              $value_wire_type$,\n" +
         "              $value_default_value$);\n" +
         "}\n");

     printer.emit(variables,
         "private com.google.protobuf.MapField<\n" +
         "    $key_type$, $value_type$> $name$_;\n" +
         "private com.google.protobuf.MapField<$key_type$, $value_type$>\n" +
         "internalGet$capitalized_name$() {\n" +
         "  if ($name$_ == null) {\n" +
         "    return com.google.protobuf.MapField.emptyMapField(\n" +
         "        $capitalized_name$DefaultEntryHolder.defaultEntry);\n" +
         "  }\n" +
         "  return $name$_;\n" +
         "}\n");

     printer.emit(variables,
         "@java.lang.Override\n" +
         "public int get$capitalized_name$Count() {\n" +
         "  return internalGet$capitalized_name$().getMap().size();\n" +
         "}\n" +
         "@java.lang.Override\n" +
         "public boolean contains$capitalized_name$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  return internalGet$capitalized_name$().getMap().containsKey(key);\n" +
         "}\n" +
         "@java.lang.Deprecated\n" +
         "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$() {\n" +
         "  return get$capitalized_name$Map();\n" +
         "}\n" +
         "@java.lang.Override\n" +
         "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$Map() {\n" +
         "  return internalGet$capitalized_name$().getMap();\n" +
         "}\n" +
         "@java.lang.Override\n" +
         "public $value_type$ get$capitalized_name$OrDefault(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$().getMap();\n" +
         "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
         "}\n" +
         "@java.lang.Override\n" +
         "public $value_type$ get$capitalized_name$OrThrow(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$().getMap();\n" +
         "  if (!map.containsKey(key)) {\n" +
         "    throw new java.lang.IllegalArgumentException();\n" +
         "  }\n" +
         "  return map.get(key);\n" +
         "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     printer.emit(variables,
         "private com.google.protobuf.MapField<\n" +
         "    $key_type$, $value_type$> $name$_;\n" +
         "private com.google.protobuf.MapField<$key_type$, $value_type$>\n" +
         "internalGet$capitalized_name$() {\n" +
         "  if ($name$_ == null) {\n" +
         "    return com.google.protobuf.MapField.emptyMapField(\n" +
         "        $capitalized_name$DefaultEntryHolder.defaultEntry);\n" +
         "  }\n" +
         "  return $name$_;\n" +
         "}\n" +
         "private com.google.protobuf.MapField<$key_type$, $value_type$>\n" +
         "internalGetMutable$capitalized_name$() {\n" +
         "  onChanged();\n" + // Important: call onChanged
         "  if ($name$_ == null) {\n" +
         "    $name$_ = com.google.protobuf.MapField.newMapField(\n" +
         "        $capitalized_name$DefaultEntryHolder.defaultEntry);\n" +
         "  }\n" +
         "  if (!$name$_.isMutable()) {\n" +
         "    $name$_ = $name$_.copy();\n" +
         "  }\n" +
         "  return $name$_;\n" +
         "}\n");

     printer.emit(variables,
         "public int get$capitalized_name$Count() {\n" +
         "  return internalGet$capitalized_name$().getMap().size();\n" +
         "}\n" +
         "public boolean contains$capitalized_name$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  return internalGet$capitalized_name$().getMap().containsKey(key);\n" +
         "}\n" +
         "@java.lang.Deprecated\n" +
         "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$() {\n" +
         "  return get$capitalized_name$Map();\n" +
         "}\n" +
         "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$Map() {\n" +
         "  return internalGet$capitalized_name$().getMap();\n" +
         "}\n" +
         "public $value_type$ get$capitalized_name$OrDefault(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ defaultValue) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$().getMap();\n" +
         "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
         "}\n" +
         "public $value_type$ get$capitalized_name$OrThrow(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  java.util.Map<$key_type$, $value_type$> map =\n" +
         "      internalGet$capitalized_name$().getMap();\n" +
         "  if (!map.containsKey(key)) {\n" +
         "    throw new java.lang.IllegalArgumentException();\n" +
         "  }\n" +
         "  return map.get(key);\n" +
         "}\n" +
         "public Builder clear$capitalized_name$() {\n" +
         "  internalGetMutable$capitalized_name$().getMutableMap().clear();\n" +
         "  return this;\n" +
         "}\n" +
         "public Builder remove$capitalized_name$(\n" +
         "    $key_type$ key) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  internalGetMutable$capitalized_name$().getMutableMap().remove(key);\n" +
         "  return this;\n" +
         "}\n" +
         "@java.lang.Deprecated\n" +
         "public java.util.Map<$key_type$, $value_type$>\n" +
         "    getMutable$capitalized_name$() {\n" +
         "  return internalGetMutable$capitalized_name$().getMutableMap();\n" +
         "}\n" +
         "public Builder put$capitalized_name$(\n" +
         "    $key_type$ key,\n" +
         "    $value_type$ value) {\n" +
         "  if (key == null) { throw new NullPointerException(); }\n" +
         "  if (value == null) { throw new NullPointerException(); }\n" +
         "  internalGetMutable$capitalized_name$().getMutableMap().put(key, value);\n" +
         "  return this;\n" +
         "}\n" +
         "public Builder putAll$capitalized_name$(\n" +
         "    java.util.Map<$key_type$, $value_type$> values) {\n" +
         "  internalGetMutable$capitalized_name$().getMutableMap().putAll(values);\n" +
         "  return this;\n" +
         "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     // Nothing to init for MapField usually, it handles null lazily?
     // Actually in Message implementation, it might need to clear or nullify.
     // But internalGet handles null.
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     printer.emit(variables,
         "internalGetMutable$capitalized_name$().clear();\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
     printer.emit(variables,
         "internalGetMutable$capitalized_name$().mergeFrom(\n" +
         "    other.internalGet$capitalized_name$());\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     printer.emit(variables,
         "result.$name$_ = internalGet$capitalized_name$();\n" +
         "result.$name$_.makeImmutable();\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     printer.emit(variables,
         "if (!$name$_.isMutable()) {\n" +
         "  $name$_ = $name$_.copy();\n" +
         "}\n" +
         "com.google.protobuf.MapEntry<$key_type$, $value_type$>\n" +
         "$name$__ = input.readMessage(\n" +
         "    $capitalized_name$DefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);\n" +
         "$name$_.getMutableMap().put(\n" +
         "    $name$__.getKey(), $name$__.getValue());\n");
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer) {
    GeneratorCommon.reportUnexpectedPackedFieldsCall();
  }

  @Override
  public void generateParsingDoneCode(Printer printer) {
    // No op
  }

  @Override
  public void generateSerializedSizeCode(Printer printer) {
    printer.emit(variables,
        "for (java.util.Map.Entry<$key_type$, $value_type$> entry\n" +
        "     : internalGet$capitalized_name$().getMap().entrySet()) {\n" +
        "  com.google.protobuf.MapEntry<$key_type$, $value_type$>\n" +
        "  $name$__ = $capitalized_name$DefaultEntryHolder.defaultEntry.newBuilderForType()\n" +
        "      .setKey(entry.getKey())\n" +
        "      .setValue(entry.getValue())\n" +
        "      .build();\n" +
        "  size += com.google.protobuf.CodedOutputStream\n" +
        "      .computeMessageSize($number$, $name$__);\n" +
        "}\n");
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    printer.emit(variables,
        "com.google.protobuf.GeneratedMessageV3\n" +
        "  .serialize$capitalized_type$MapTo(\n" +
        "    output,\n" +
        "    internalGet$capitalized_name$(),\n" +
        "    $capitalized_name$DefaultEntryHolder.defaultEntry,\n" +
        "    $number$);\n");
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     printer.emit(variables,
         "if (!internalGet$capitalized_name$().equals(\n" +
         "    other.internalGet$capitalized_name$())) return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     printer.emit(variables,
         "if (!internalGet$capitalized_name$().getMap().isEmpty()) {\n" +
         "  hash = (37 * hash) + $constant_name$;\n" +
         "  hash = (53 * hash) + internalGet$capitalized_name$().hashCode();\n" +
         "}\n");
  }
}
