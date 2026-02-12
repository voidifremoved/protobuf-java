package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

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

    variables.put("key_type", Helpers.getJavaType(keyField) == Helpers.JavaType.STRING
        ? "java.lang.String" : Helpers.getBoxedPrimitiveTypeName(Helpers.getJavaType(keyField)));
    variables.put("value_type", Helpers.getJavaType(valueField) == Helpers.JavaType.STRING
        ? "java.lang.String" : Helpers.getBoxedPrimitiveTypeName(Helpers.getJavaType(valueField)));

    // For signatures, we often use unboxed types
    variables.put("key_type_sig", Helpers.getJavaType(keyField) == Helpers.JavaType.STRING
        ? "java.lang.String" : Helpers.getPrimitiveTypeName(Helpers.getJavaType(keyField)));
    variables.put("value_type_sig", Helpers.getJavaType(valueField) == Helpers.JavaType.STRING
        ? "java.lang.String" : Helpers.getPrimitiveTypeName(Helpers.getJavaType(valueField)));

    variables.put("type", "com.google.protobuf.MapField" + Helpers.getGeneratedCodeVersionSuffix() + "<\n" +
        "    " + variables.get("key_type") + ", " + variables.get("value_type") + ">");

    variables.put("repeated_get", "get" + variables.get("capitalized_name"));
    variables.put("repeated_count", "get" + variables.get("capitalized_name") + "Count");

    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED));

    variables.put("key_wire_type", "com.google.protobuf.WireFormat.FieldType." + keyField.getType().name());
    variables.put("value_wire_type", "com.google.protobuf.WireFormat.FieldType." + valueField.getType().name());

    variables.put("key_default_value", Helpers.defaultValue(keyField, true, context.getNameResolver(), context.getOptions()));
    variables.put("value_default_value", Helpers.defaultValue(valueField, true, context.getNameResolver(), context.getOptions()));

    variables.put("descriptor", context.getNameResolver().getClassName(descriptor.getFile(), true) + ".internal_static_" + Helpers.uniqueFileScopeIdentifier(descriptor.getMessageType()) + "_descriptor");

    variables.put("map_serialization_type", getMapSerializationType(keyField.getType()));
  }

  private String getMapSerializationType(FieldDescriptor.Type type) {
    switch (type) {
      case INT32:
      case UINT32:
      case SINT32:
      case FIXED32:
      case SFIXED32:
        return "Integer";
      case INT64:
      case UINT64:
      case SINT64:
      case FIXED64:
      case SFIXED64:
        return "Long";
      case BOOL:
        return "Boolean";
      case STRING:
        return "String";
      default:
        return "";
    }
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables, "int $repeated_count$();\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "boolean contains$capitalized_name$(\n" +
        "    $key_type_sig$ key);\n");

    printer.emit(variables,
        "/**\n" +
        " * Use {@link #get$capitalized_name$Map()} instead.\n" +
        " */\n" +
        "@java.lang.Deprecated\n" +
        "java.util.Map<$key_type$, $value_type$>\n" +
        "$repeated_get$();\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "java.util.Map<$key_type$, $value_type$>\n" +
        "get$capitalized_name$Map();\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);

    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
    Helpers.JavaType valueType = Helpers.getJavaType(valueField);
    boolean isNullableValue = (valueType == Helpers.JavaType.STRING || valueType == Helpers.JavaType.BYTES || valueType == Helpers.JavaType.MESSAGE);

    if (isNullableValue) {
      printer.emit(variables,
          "\n" +
          "    /* nullable */\n" +
          "$value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "        $key_type_sig$ key,\n" +
          "        /* nullable */\n" +
          "$value_type_sig$ defaultValue);\n");
    } else {
      printer.emit(variables,
          "$value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "    $key_type_sig$ key,\n" +
          "    $value_type_sig$ defaultValue);\n");
    }

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "$value_type_sig$ get$capitalized_name$OrThrow(\n" +
        "    $key_type_sig$ key);\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables,
        "private static final class $capitalized_name$DefaultEntryHolder {\n" +
        "  static final com.google.protobuf.MapEntry<\n" +
        "      $key_type$, $value_type$> defaultEntry =\n" +
        "          com.google.protobuf.MapEntry\n" +
        "          .<$key_type$, $value_type$>newDefaultInstance(\n" +
        "              $descriptor$, \n" +
        "              $key_wire_type$,\n" +
        "              $key_default_value$,\n" +
        "              $value_wire_type$,\n" +
        "              $value_default_value$);\n" +
        "}\n" +
        "@SuppressWarnings(\"serial\")\n" +
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
        "public int $repeated_count$() {\n" +
        "  return internalGet$capitalized_name$().getMap().size();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    Helpers.JavaType keyType = Helpers.getJavaType(keyField);

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean contains$capitalized_name$(\n" +
        "    $key_type_sig$ key) {\n" +
        (keyType == Helpers.JavaType.STRING
            ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  return internalGet$capitalized_name$().getMap().containsKey(key);\n" +
        "}\n");

    printer.emit(variables,
        "/**\n" +
        " * Use {@link #get$capitalized_name$Map()} instead.\n" +
        " */\n" +
        "@java.lang.Override\n" +
        "@java.lang.Deprecated\n" +
        "public java.util.Map<$key_type$, $value_type$> $repeated_get$() {\n" +
        "  return get$capitalized_name$Map();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$Map() {\n" +
        "  return internalGet$capitalized_name$().getMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
    Helpers.JavaType valueType = Helpers.getJavaType(valueField);
    boolean isNullableValue = (valueType == Helpers.JavaType.STRING || valueType == Helpers.JavaType.BYTES || valueType == Helpers.JavaType.MESSAGE);

    if (isNullableValue) {
      printer.emit(variables,
          "\n" +
          "    @java.lang.Override\n" +
          "    public /* nullable */\n" +
          "$value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "        $key_type_sig$ key,\n" +
          "        /* nullable */\n" +
          "$value_type_sig$ defaultValue) {\n");
      printer.indent(); printer.indent(); printer.indent();
      if (keyType == Helpers.JavaType.STRING) {
        printer.print("      if (key == null) { throw new NullPointerException(\"map key\"); }\n");
      } else {
        printer.print("\n");
      }
      printer.emit(variables,
          "      java.util.Map<$key_type$, $value_type$> map =\n" +
          "          internalGet$capitalized_name$().getMap();\n" +
          "      return map.containsKey(key) ? map.get(key) : defaultValue;\n");
      printer.outdent(); printer.outdent(); printer.outdent();
      printer.print("    }\n");
    } else {
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public $value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "    $key_type_sig$ key,\n" +
          "    $value_type_sig$ defaultValue) {\n" +
          (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
          "  java.util.Map<$key_type$, $value_type$> map =\n" +
          "      internalGet$capitalized_name$().getMap();\n" +
          "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
          "}\n");
    }

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $value_type_sig$ get$capitalized_name$OrThrow(\n" +
        "    $key_type_sig$ key) {\n" +
        (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
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
        "    internalGet$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    return com.google.protobuf.MapField.emptyMapField(\n" +
        "        $capitalized_name$DefaultEntryHolder.defaultEntry);\n" +
        "  }\n" +
        "  return $name$_;\n" +
        "}\n" +
        "private com.google.protobuf.MapField<$key_type$, $value_type$>\n" +
        "    internalGetMutable$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    $name$_ = com.google.protobuf.MapField.newMapField(\n" +
        "        $capitalized_name$DefaultEntryHolder.defaultEntry);\n" +
        "  }\n" +
        "  if (!$name$_.isMutable()) {\n" +
        "    $name$_ = $name$_.copy();\n" +
        "  }\n" +
        "  onChanged();\n" +
        "  return $name$_;\n" +
        "}\n");

    printer.emit(variables,
        "public int $repeated_count$() {\n" +
        "  return internalGet$capitalized_name$().getMap().size();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    Helpers.JavaType keyType = Helpers.getJavaType(keyField);

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean contains$capitalized_name$(\n" +
        "    $key_type_sig$ key) {\n" +
        (keyType == Helpers.JavaType.STRING
            ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  return internalGet$capitalized_name$().getMap().containsKey(key);\n" +
        "}\n");

    printer.emit(variables,
        "/**\n" +
        " * Use {@link #get$capitalized_name$Map()} instead.\n" +
        " */\n" +
        "@java.lang.Override\n" +
        "@java.lang.Deprecated\n" +
        "public java.util.Map<$key_type$, $value_type$> $repeated_get$() {\n" +
        "  return get$capitalized_name$Map();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.Map<$key_type$, $value_type$> get$capitalized_name$Map() {\n" +
        "  return internalGet$capitalized_name$().getMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
    Helpers.JavaType valueType = Helpers.getJavaType(valueField);
    boolean isNullableValue = (valueType == Helpers.JavaType.STRING || valueType == Helpers.JavaType.BYTES || valueType == Helpers.JavaType.MESSAGE);

    if (isNullableValue) {
      printer.emit(variables,
          "\n" +
          "    @java.lang.Override\n" +
          "    public /* nullable */\n" +
          "$value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "        $key_type_sig$ key,\n" +
          "        /* nullable */\n" +
          "$value_type_sig$ defaultValue) {\n");
      printer.indent(); printer.indent(); printer.indent();
      if (keyType == Helpers.JavaType.STRING) {
        printer.print("      if (key == null) { throw new NullPointerException(\"map key\"); }\n");
      } else {
        printer.print("\n");
      }
      printer.emit(variables,
          "      java.util.Map<$key_type$, $value_type$> map =\n" +
          "          internalGet$capitalized_name$().getMap();\n" +
          "      return map.containsKey(key) ? map.get(key) : defaultValue;\n");
      printer.outdent(); printer.outdent(); printer.outdent();
      printer.print("    }\n");
    } else {
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public $value_type_sig$ get$capitalized_name$OrDefault(\n" +
          "    $key_type_sig$ key,\n" +
          "    $value_type_sig$ defaultValue) {\n" +
          (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
          "  java.util.Map<$key_type$, $value_type$> map =\n" +
          "      internalGet$capitalized_name$().getMap();\n" +
          "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
          "}\n");
    }

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $value_type_sig$ get$capitalized_name$OrThrow(\n" +
        "    $key_type_sig$ key) {\n" +
        (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  java.util.Map<$key_type$, $value_type$> map =\n" +
        "      internalGet$capitalized_name$().getMap();\n" +
        "  if (!map.containsKey(key)) {\n" +
        "    throw new java.lang.IllegalArgumentException();\n" +
        "  }\n" +
        "  return map.get(key);\n" +
        "}\n");

    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .clear();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder remove$capitalized_name$(\n" +
        "    $key_type_sig$ key) {\n" +
        (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .remove(key);\n" +
        "  return this;\n" +
        "}\n");

    printer.emit(variables,
        "/**\n" +
        " * Use {@link #getMutable$capitalized_name$Map()} instead.\n" +
        " */\n" +
        "@java.lang.Deprecated\n" +
        "public java.util.Map<$key_type$, $value_type$>\n" +
        "getMutable$capitalized_name$() {\n" +
        "  return internalGetMutable$capitalized_name$().getMutableMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public java.util.Map<$key_type$, $value_type$>\n" +
        "getMutable$capitalized_name$Map() {\n" +
        "  return internalGetMutable$capitalized_name$().getMutableMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder put$capitalized_name$(\n" +
        "    $key_type_sig$ key,\n" +
        "    $value_type_sig$ value) {\n" +
        (keyType == Helpers.JavaType.STRING ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "") +
        (valueType == Helpers.JavaType.STRING || valueType == Helpers.JavaType.BYTES || valueType == Helpers.JavaType.MESSAGE ? "  if (value == null) { throw new NullPointerException(\"map value\"); }\n" : "") +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .put(key, value);\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder putAll$capitalized_name$(\n" +
        "    java.util.Map<$key_type$, $value_type$> values) {\n" +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .putAll(values);\n" +
        "  return this;\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
    printer.emit(variables, "$name$_ = null;\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
    printer.emit(variables, "internalGetMutable$capitalized_name$().mergeFrom(\n" +
        "    other.internalGet$capitalized_name$());\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
    printer.emit(variables, "result.$name$_ = internalGet$capitalized_name$();\n" +
        "result.$name$_.makeImmutable();\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
    printer.emit(variables,
        "com.google.protobuf.MapEntry<\n" +
        "    $key_type$, $value_type$> $name$__ = input.readMessage(\n" +
        "        $capitalized_name$DefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);\n" +
        "internalGetMutable$capitalized_name$().getMutableMap().put(\n" +
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
        "com.google.protobuf.GeneratedMessage\n" +
        "  .serialize$map_serialization_type$MapTo(\n" +
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
