package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

/**
 * For generating map fields.
 * Ported from java/full/map_field.cc.
 */
public class MapFieldGenerator extends ImmutableFieldGenerator
{
  private final boolean isEnumValue;
  private final boolean isMessageValue;
  private final boolean supportUnknownEnumValue;
  private final boolean isNullableValue;

  private static String typeName(FieldDescriptor field, ClassNameResolver nameResolver, boolean boxed)
  {
    Helpers.JavaType javaType = Helpers.getJavaType(field);
    if (javaType == Helpers.JavaType.MESSAGE)
    {
      return nameResolver.getImmutableClassName(field.getMessageType());
    }
    else if (javaType == Helpers.JavaType.ENUM)
    {
      return nameResolver.getImmutableClassName(field.getEnumType());
    }
    else
    {
      return boxed ? Helpers.getBoxedPrimitiveTypeName(javaType) : Helpers.getPrimitiveTypeName(javaType);
    }
  }

  public MapFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
  {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
    ClassNameResolver nameResolver = context.getNameResolver();

    Helpers.JavaType keyJavaType = Helpers.getJavaType(keyField);
    Helpers.JavaType valueJavaType = Helpers.getJavaType(valueField);

    this.isEnumValue = valueJavaType == Helpers.JavaType.ENUM;
    this.isMessageValue = valueJavaType == Helpers.JavaType.MESSAGE;
    this.supportUnknownEnumValue = isEnumValue && InternalHelpers.supportUnknownEnumValue(valueField);
    this.isNullableValue = isReferenceType(valueJavaType);

    variables.put("key_type", typeName(keyField, nameResolver, false));
    String boxedKeyType = typeName(keyField, nameResolver, true);
    variables.put("boxed_key_type", boxedKeyType);
    String shortKeyType = boxedKeyType.substring(boxedKeyType.lastIndexOf('.') + 1);
    variables.put("short_key_type", shortKeyType);
    variables.put("key_wire_type", "com.google.protobuf.WireFormat.FieldType." + keyField.getType().name());
    variables.put("key_default_value", Helpers.defaultValue(keyField, true, nameResolver, context.getOptions()));
    variables.put("key_null_check",
        isReferenceType(keyJavaType)
            ? "if (key == null) { throw new NullPointerException(\"map key\"); }"
            : "");

    if (isEnumValue)
    {
      variables.put("value_type", "int");
      variables.put("boxed_value_type", "java.lang.Integer");
      variables.put("value_wire_type", "com.google.protobuf.WireFormat.FieldType." + valueField.getType().name());
      variables.put("value_default_value",
          Helpers.defaultValue(valueField, true, nameResolver, context.getOptions()) + ".getNumber()");

      String valueEnumType = typeName(valueField, nameResolver, false);
      variables.put("value_enum_type", valueEnumType);

      if (supportUnknownEnumValue)
      {
        variables.put("unrecognized_value", valueEnumType + ".UNRECOGNIZED");
      }
      else
      {
        variables.put("unrecognized_value",
            Helpers.defaultValue(valueField, true, nameResolver, context.getOptions()));
      }

      variables.put("value_null_check", "");
    }
    else
    {
      String valueType = typeName(valueField, nameResolver, false);
      variables.put("value_type", valueType);
      variables.put("boxed_value_type", typeName(valueField, nameResolver, true));
      variables.put("value_wire_type", "com.google.protobuf.WireFormat.FieldType." + valueField.getType().name());
      variables.put("value_default_value", Helpers.defaultValue(valueField, true, nameResolver, context.getOptions()));

      variables.put("value_null_check",
          valueJavaType != Helpers.JavaType.ENUM && isReferenceType(valueJavaType)
              ? "if (value == null) { throw new NullPointerException(\"map value\"); }"
              : "");
    }

    String typeParameters = variables.get("boxed_key_type") + ", " + variables.get("boxed_value_type");
    variables.put("type_parameters", typeParameters);

    variables.put("default_entry", variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry");
    variables.put("map_field_parameter", variables.get("default_entry"));
    variables.put("descriptor", nameResolver.getClassName(descriptor.getFile(), true) + ".internal_static_" +
        Helpers.uniqueFileScopeIdentifier(descriptor.getMessageType()) + "_descriptor");

    variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
    variables.put("get_has_field_bit_from_local", Helpers.generateGetBit("from_", builderBitIndex));
    variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
    variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
    variables.put("on_changed", "onChanged();");

    if (isMessageValue)
    {
      variables.put("value_interface_type", variables.get("boxed_value_type") + "OrBuilder");
      variables.put("value_builder_type", variables.get("boxed_value_type") + ".Builder");
      variables.put("builder_type_parameters",
          boxedKeyType + ", " + variables.get("value_interface_type") + ", " +
          variables.get("boxed_value_type") + ", " + variables.get("value_builder_type"));
    }
  }

  private static boolean isReferenceType(Helpers.JavaType type)
  {
    return type == Helpers.JavaType.STRING || type == Helpers.JavaType.BYTES || type == Helpers.JavaType.MESSAGE || type == Helpers.JavaType.ENUM;
  }

  private void emitNullableType(Printer printer, String typeName, String prefix)
  {
    printer.print(prefix + "/* nullable */\n");
    printer.writeNoIndent(typeName);
  }

  @Override
  public int getNumBitsForMessage()
  {
    return 0;
  }

  @Override
  public int getNumBitsForBuilder()
  {
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer)
  {
    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables, "int get$capitalized_name$Count();\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "boolean contains$capitalized_name$(\n" +
        "    $key_type$ key);\n");

    if (isEnumValue)
    {
      printer.emit(variables,
          "/**\n" +
          " * Use {@link #get$capitalized_name$Map()} instead.\n" +
          " */\n" +
          "@java.lang.Deprecated\n" +
          "java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "get$capitalized_name$();\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "get$capitalized_name$Map();\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      emitNullableType(printer, (String) variables.get("value_enum_type"), "");
      printer.emit(variables,
          " get$capitalized_name$OrDefault(\n" +
          "    $key_type$ key,\n");
      emitNullableType(printer, (String) variables.get("value_enum_type"), "    ");
      printer.emit(variables,
          "         defaultValue);\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "$value_enum_type$ get$capitalized_name$OrThrow(\n" +
          "    $key_type$ key);\n");

      if (supportUnknownEnumValue)
      {
        printer.emit(variables,
            "/**\n" +
            " * Use {@link #get$capitalized_name$ValueMap()} instead.\n" +
            " */\n" +
            "@java.lang.Deprecated\n" +
            "java.util.Map<$type_parameters$>\n" +
            "get$capitalized_name$Value();\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "java.util.Map<$type_parameters$>\n" +
            "get$capitalized_name$ValueMap();\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "int get$capitalized_name$ValueOrDefault(\n" +
            "    $key_type$ key,\n" +
            "    int defaultValue);\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "int get$capitalized_name$ValueOrThrow(\n" +
            "    $key_type$ key);\n");
      }
    }
    else
    {
      printer.emit(variables,
          "/**\n" +
          " * Use {@link #get$capitalized_name$Map()} instead.\n" +
          " */\n" +
          "@java.lang.Deprecated\n" +
          "java.util.Map<$type_parameters$>\n" +
          "get$capitalized_name$();\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "java.util.Map<$type_parameters$>\n" +
          "get$capitalized_name$Map();\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      if (isMessageValue)
      {
        printer.emit(variables,
            "\n" +
            "    /* nullable */\n" +
            "$value_type$ get$capitalized_name$OrDefault(\n" +
            "        $key_type$ key,\n" +
            "        /* nullable */\n" +
            "$value_type$ defaultValue);\n");
      }
      else
      {
        if (isNullableValue)
        {
          emitNullableType(printer, (String) variables.get("value_type"), "");
          printer.emit(variables,
              " get$capitalized_name$OrDefault(\n" +
              "    $key_type$ key,\n");
          emitNullableType(printer, (String) variables.get("value_type"), "    ");
          printer.emit(variables,
              " defaultValue);\n");
        }
        else
        {
          printer.emit(variables,
              "$value_type$ get$capitalized_name$OrDefault(\n" +
              "    $key_type$ key,\n" +
              "    $value_type$ defaultValue);\n");
        }
      }

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "$value_type$ get$capitalized_name$OrThrow(\n" +
          "    $key_type$ key);\n");
    }
  }

  @Override
  public void generateMembers(Printer printer)
  {
    printer.emit(variables,
        "private static final class $capitalized_name$DefaultEntryHolder {\n" +
        "  static final com.google.protobuf.MapEntry<\n" +
        "      $type_parameters$> defaultEntry =\n" +
        "          com.google.protobuf.MapEntry\n" +
        "          .<$type_parameters$>newDefaultInstance(\n" +
        "              $descriptor$, \n" +
        "              $key_wire_type$,\n" +
        "              $key_default_value$,\n" +
        "              $value_wire_type$,\n" +
        "              $value_default_value$);\n" +
        "}\n" +
        "@SuppressWarnings(\"serial\")\n" +
        "private com.google.protobuf.MapField<\n" +
        "    $type_parameters$> $name$_;\n" +
        "private com.google.protobuf.MapField<$type_parameters$>\n" +
        "internalGet$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    return com.google.protobuf.MapField.emptyMapField(\n" +
        "        $map_field_parameter$);\n" +
        "  }\n" +
        "  return $name$_;\n" +
        "}\n");

    if (isEnumValue)
    {
      printer.emit(variables,
          "private static final\n" +
          "com.google.protobuf.Internal.MapAdapter.Converter<\n" +
          "    java.lang.Integer, $value_enum_type$> $name$ValueConverter =\n" +
          "        com.google.protobuf.Internal.MapAdapter.newEnumConverter(\n" +
          "            $value_enum_type$.internalGetValueMap(),\n" +
          "            $unrecognized_value$);\n");
      printer.emit(variables,
          "private static final java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "internalGetAdapted$capitalized_name$Map(\n" +
          "    java.util.Map<$boxed_key_type$, $boxed_value_type$> map) {\n" +
          "  return new com.google.protobuf.Internal.MapAdapter<\n" +
          "      $boxed_key_type$, $value_enum_type$, java.lang.Integer>(\n" +
          "          map, $name$ValueConverter);\n" +
          "}\n");
    }

    generateMapGetters(printer);
  }

  @Override
  public void generateBuilderMembers(Printer printer)
  {
    if (isMessageValue)
    {
      generateMessageMapBuilderMembers(printer);
      return;
    }

    printer.emit(variables,
        "private com.google.protobuf.MapField<\n" +
        "    $type_parameters$> $name$_;\n" +
        "private com.google.protobuf.MapField<$type_parameters$>\n" +
        "    internalGet$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    return com.google.protobuf.MapField.emptyMapField(\n" +
        "        $map_field_parameter$);\n" +
        "  }\n" +
        "  return $name$_;\n" +
        "}\n" +
        "private com.google.protobuf.MapField<$type_parameters$>\n" +
        "    internalGetMutable$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    $name$_ = com.google.protobuf.MapField.newMapField(\n" +
        "        $map_field_parameter$);\n" +
        "  }\n" +
        "  if (!$name$_.isMutable()) {\n" +
        "    $name$_ = $name$_.copy();\n" +
        "  }\n" +
        "  $set_has_field_bit_builder$\n" +
        "  $on_changed$\n" +
        "  return $name$_;\n" +
        "}\n");

    generateMapGetters(printer);

    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  $clear_has_field_bit_builder$\n" +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .clear();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder remove$capitalized_name$(\n" +
        "    $key_type$ key) {\n" +
        "  $key_null_check$\n" +
        "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
        "      .remove(key);\n" +
        "  return this;\n" +
        "}\n");

    if (isEnumValue)
    {
      printer.emit(variables,
          "/**\n" +
          " * Use alternate mutation accessors instead.\n" +
          " */\n" +
          "@java.lang.Deprecated\n" +
          "public java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "    getMutable$capitalized_name$() {\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return internalGetAdapted$capitalized_name$Map(\n" +
          "       internalGetMutable$capitalized_name$().getMutableMap());\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "public Builder put$capitalized_name$(\n" +
          "    $key_type$ key,\n" +
          "    $value_enum_type$ value) {\n" +
          "  $key_null_check$\n" +
          "  $value_null_check$\n" +
          "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
          "      .put(key, $name$ValueConverter.doBackward(value));\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return this;\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "public Builder putAll$capitalized_name$(\n" +
          "    java.util.Map<$boxed_key_type$, $value_enum_type$> values) {\n" +
          "  internalGetAdapted$capitalized_name$Map(\n" +
          "      internalGetMutable$capitalized_name$().getMutableMap())\n" +
          "          .putAll(values);\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return this;\n" +
          "}\n");

      if (supportUnknownEnumValue)
      {
        printer.emit(variables,
            "/**\n" +
            " * Use alternate mutation accessors instead.\n" +
            " */\n" +
            "@java.lang.Deprecated\n" +
            "public java.util.Map<$boxed_key_type$, $boxed_value_type$>\n" +
            "getMutable$capitalized_name$Value() {\n" +
            "  $set_has_field_bit_builder$\n" +
            "  return internalGetMutable$capitalized_name$().getMutableMap();\n" +
            "}\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "public Builder put$capitalized_name$Value(\n" +
            "    $key_type$ key,\n" +
            "    $value_type$ value) {\n" +
            "  $key_null_check$\n" +
            "  $value_null_check$\n" +
            "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
            "      .put(key, value);\n" +
            "  $set_has_field_bit_builder$\n" +
            "  return this;\n" +
            "}\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "public Builder putAll$capitalized_name$Value(\n" +
            "    java.util.Map<$boxed_key_type$, $boxed_value_type$> values) {\n" +
            "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
            "      .putAll(values);\n" +
            "  $set_has_field_bit_builder$\n" +
            "  return this;\n" +
            "}\n");
      }
    }
    else
    {
      printer.emit(variables,
          "/**\n" +
          " * Use alternate mutation accessors instead.\n" +
          " */\n" +
          "@java.lang.Deprecated\n" +
          "public java.util.Map<$type_parameters$>\n" +
          "    getMutable$capitalized_name$() {\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return internalGetMutable$capitalized_name$().getMutableMap();\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "public Builder put$capitalized_name$(\n" +
          "    $key_type$ key,\n" +
          "    $value_type$ value) {\n" +
          "  $key_null_check$\n" +
          "  $value_null_check$\n" +
          "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
          "      .put(key, value);\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return this;\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "public Builder putAll$capitalized_name$(\n" +
          "    java.util.Map<$type_parameters$> values) {\n" +
          "  internalGetMutable$capitalized_name$().getMutableMap()\n" +
          "      .putAll(values);\n" +
          "  $set_has_field_bit_builder$\n" +
          "  return this;\n" +
          "}\n");
    }
  }

  private void generateMessageMapBuilderMembers(Printer printer)
  {
    printer.emit(variables,
        "private static final class $capitalized_name$Converter implements " +
        "com.google.protobuf.MapFieldBuilder.Converter<$boxed_key_type$, " +
        "$value_interface_type$, $boxed_value_type$> {\n");
    printer.indent();
    printer.print("@java.lang.Override\n");
    printer.emit(variables,
        "public $boxed_value_type$ build($value_interface_type$ val) {\n");
    printer.indent();
    printer.emit(variables,
        "if (val instanceof $boxed_value_type$) { return ($boxed_value_type$) val; }\n");
    printer.emit(variables,
        "return (($value_builder_type$) val).build();\n");
    printer.outdent();
    printer.print("}\n\n");
    printer.print("@java.lang.Override\n");
    printer.emit(variables,
        "public com.google.protobuf.MapEntry<$boxed_key_type$, " +
        "$boxed_value_type$> defaultEntry() {\n");
    printer.indent();
    printer.emit(variables,
        "return $capitalized_name$DefaultEntryHolder.defaultEntry;\n");
    printer.outdent();
    printer.print("}\n");
    printer.outdent();
    printer.print("};\n");
    printer.emit(variables,
        "private static final $capitalized_name$Converter " +
        "$name$Converter = new $capitalized_name$Converter();\n\n");

    printer.emit(variables,
        "private com.google.protobuf.MapFieldBuilder<\n" +
        "    $builder_type_parameters$> $name$_;\n" +
        "private com.google.protobuf.MapFieldBuilder<$builder_type_parameters$>\n" +
        "    internalGet$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    return new com.google.protobuf.MapFieldBuilder<>($name$Converter);\n" +
        "  }\n" +
        "  return $name$_;\n" +
        "}\n" +
        "private com.google.protobuf.MapFieldBuilder<$builder_type_parameters$>\n" +
        "    internalGetMutable$capitalized_name$() {\n" +
        "  if ($name$_ == null) {\n" +
        "    $name$_ = new com.google.protobuf.MapFieldBuilder<>($name$Converter);\n" +
        "  }\n" +
        "  $set_has_field_bit_builder$\n" +
        "  $on_changed$\n" +
        "  return $name$_;\n" +
        "}\n");

    generateMessageMapGetters(printer);

    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  $clear_has_field_bit_builder$\n" +
        "  internalGetMutable$capitalized_name$().clear();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder remove$capitalized_name$(\n" +
        "    $key_type$ key) {\n" +
        "  $key_null_check$\n" +
        "  internalGetMutable$capitalized_name$().ensureBuilderMap()\n" +
        "      .remove(key);\n" +
        "  return this;\n" +
        "}\n");

    printer.emit(variables,
        "/**\n" +
        " * Use alternate mutation accessors instead.\n" +
        " */\n" +
        "@java.lang.Deprecated\n" +
        "public java.util.Map<$type_parameters$>\n" +
        "    getMutable$capitalized_name$() {\n" +
        "  $set_has_field_bit_builder$\n" +
        "  return internalGetMutable$capitalized_name$().ensureMessageMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder put$capitalized_name$(\n" +
        "    $key_type$ key,\n" +
        "    $value_type$ value) {\n" +
        "  $key_null_check$\n" +
        "  $value_null_check$\n" +
        "  internalGetMutable$capitalized_name$().ensureBuilderMap()\n" +
        "      .put(key, value);\n" +
        "  $set_has_field_bit_builder$\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public Builder putAll$capitalized_name$(\n" +
        "    java.util.Map<$type_parameters$> values) {\n" +
        "  for (java.util.Map.Entry<$type_parameters$> e : values.entrySet()) {\n" +
        "    if (e.getKey() == null || e.getValue() == null) {\n" +
        "      throw new NullPointerException();\n" +
        "    }\n" +
        "  }\n" +
        "  internalGetMutable$capitalized_name$().ensureBuilderMap()\n" +
        "      .putAll(values);\n" +
        "  $set_has_field_bit_builder$\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "public $value_builder_type$ put$capitalized_name$BuilderIfAbsent(\n" +
        "    $key_type$ key) {\n" +
        "  java.util.Map<$boxed_key_type$, $value_interface_type$> builderMap = " +
        "internalGetMutable$capitalized_name$().ensureBuilderMap();\n" +
        "  $value_interface_type$ entry = builderMap.get(key);\n" +
        "  if (entry == null) {\n" +
        "    entry = $value_type$.newBuilder();\n" +
        "    builderMap.put(key, entry);\n" +
        "  }\n" +
        "  if (entry instanceof $value_type$) {\n" +
        "    entry = (($value_type$) entry).toBuilder();\n" +
        "    builderMap.put(key, entry);\n" +
        "  }\n" +
        "  return ($value_builder_type$) entry;\n" +
        "}\n");
  }

  private void generateMapGetters(Printer printer)
  {
    printer.emit(variables,
        "public int get$capitalized_name$Count() {\n" +
        "  return internalGet$capitalized_name$().getMap().size();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    Helpers.JavaType keyType = Helpers.getJavaType(keyField);

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean contains$capitalized_name$(\n" +
        "    $key_type$ key) {\n" +
        (keyType == Helpers.JavaType.STRING
            ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  return internalGet$capitalized_name$().getMap().containsKey(key);\n" +
        "}\n");

    if (isEnumValue)
    {
      printer.emit(variables,
          "/**\n" +
          " * Use {@link #get$capitalized_name$Map()} instead.\n" +
          " */\n" +
          "@java.lang.Override\n" +
          "@java.lang.Deprecated\n" +
          "public java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "get$capitalized_name$() {\n" +
          "  return get$capitalized_name$Map();\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public java.util.Map<$boxed_key_type$, $value_enum_type$>\n" +
          "get$capitalized_name$Map() {\n" +
          "  return internalGetAdapted$capitalized_name$Map(\n" +
          "      internalGet$capitalized_name$().getMap());" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.print("@java.lang.Override\n");
      printer.print("public ");
      emitNullableType(printer, (String) variables.get("value_enum_type"), "");
      printer.emit(variables,
          " get$capitalized_name$OrDefault(\n" +
          "    $key_type$ key,\n");
      emitNullableType(printer, (String) variables.get("value_enum_type"), "    ");
      printer.emit(variables,
          " defaultValue) {\n" +
          "  $key_null_check$\n" +
          "  java.util.Map<$boxed_key_type$, $boxed_value_type$> map =\n" +
          "      internalGet$capitalized_name$().getMap();\n" +
          "  return map.containsKey(key)\n" +
          "         ? $name$ValueConverter.doForward(map.get(key))\n" +
          "         : defaultValue;\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public $value_enum_type$ get$capitalized_name$OrThrow(\n" +
          "    $key_type$ key) {\n" +
          "  $key_null_check$\n" +
          "  java.util.Map<$boxed_key_type$, $boxed_value_type$> map =\n" +
          "      internalGet$capitalized_name$().getMap();\n" +
          "  if (!map.containsKey(key)) {\n" +
          "    throw new java.lang.IllegalArgumentException();\n" +
          "  }\n" +
          "  return $name$ValueConverter.doForward(map.get(key));\n" +
          "}\n");

      if (supportUnknownEnumValue)
      {
        printer.emit(variables,
            "/**\n" +
            " * Use {@link #get$capitalized_name$ValueMap()} instead.\n" +
            " */\n" +
            "@java.lang.Override\n" +
            "@java.lang.Deprecated\n" +
            "public java.util.Map<$boxed_key_type$, $boxed_value_type$>\n" +
            "get$capitalized_name$Value() {\n" +
            "  return get$capitalized_name$ValueMap();\n" +
            "}\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "@java.lang.Override\n" +
            "public java.util.Map<$boxed_key_type$, $boxed_value_type$>\n" +
            "get$capitalized_name$ValueMap() {\n" +
            "  return internalGet$capitalized_name$().getMap();\n" +
            "}\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "@java.lang.Override\n" +
            "public int get$capitalized_name$ValueOrDefault(\n" +
            "    $key_type$ key,\n" +
            "    int defaultValue) {\n" +
            "  $key_null_check$\n" +
            "  java.util.Map<$boxed_key_type$, $boxed_value_type$> map =\n" +
            "      internalGet$capitalized_name$().getMap();\n" +
            "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
            "}\n");

        DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
        printer.emit(variables,
            "@java.lang.Override\n" +
            "public int get$capitalized_name$ValueOrThrow(\n" +
            "    $key_type$ key) {\n" +
            "  $key_null_check$\n" +
            "  java.util.Map<$boxed_key_type$, $boxed_value_type$> map =\n" +
            "      internalGet$capitalized_name$().getMap();\n" +
            "  if (!map.containsKey(key)) {\n" +
            "    throw new java.lang.IllegalArgumentException();\n" +
            "  }\n" +
            "  return map.get(key);\n" +
            "}\n");
      }
    }
    else
    {
      printer.emit(variables,
          "/**\n" +
          " * Use {@link #get$capitalized_name$Map()} instead.\n" +
          " */\n" +
          "@java.lang.Override\n" +
          "@java.lang.Deprecated\n" +
          "public java.util.Map<$type_parameters$> get$capitalized_name$() {\n" +
          "  return get$capitalized_name$Map();\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public java.util.Map<$type_parameters$> get$capitalized_name$Map() {\n" +
          "  return internalGet$capitalized_name$().getMap();\n" +
          "}\n");

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      if (isNullableValue)
      {
        printer.print("@java.lang.Override\n");
        printer.print("public ");
        emitNullableType(printer, (String) variables.get("value_type"), "");
        printer.emit(variables,
            " get$capitalized_name$OrDefault(\n" +
            "    $key_type$ key,\n");
        emitNullableType(printer, (String) variables.get("value_type"), "    ");
        printer.emit(variables,
            " defaultValue) {\n" +
            "  $key_null_check$\n" +
            "  java.util.Map<$type_parameters$> map =\n" +
            "      internalGet$capitalized_name$().getMap();\n" +
            "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
            "}\n");
      }
      else
      {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "public $value_type$ get$capitalized_name$OrDefault(\n" +
            "    $key_type$ key,\n" +
            "    $value_type$ defaultValue) {\n" +
            "  $key_null_check$\n" +
            "  java.util.Map<$type_parameters$> map =\n" +
            "      internalGet$capitalized_name$().getMap();\n" +
            "  return map.containsKey(key) ? map.get(key) : defaultValue;\n" +
            "}\n");
      }

      DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public $value_type$ get$capitalized_name$OrThrow(\n" +
          "    $key_type$ key) {\n" +
          "  $key_null_check$\n" +
          "  java.util.Map<$type_parameters$> map =\n" +
          "      internalGet$capitalized_name$().getMap();\n" +
          "  if (!map.containsKey(key)) {\n" +
          "    throw new java.lang.IllegalArgumentException();\n" +
          "  }\n" +
          "  return map.get(key);\n" +
          "}\n");
    }
  }

  private void generateMessageMapGetters(Printer printer)
  {
    printer.emit(variables,
        "public int get$capitalized_name$Count() {\n" +
        "  return internalGet$capitalized_name$().ensureBuilderMap().size();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
    Helpers.JavaType keyType = Helpers.getJavaType(keyField);

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean contains$capitalized_name$(\n" +
        "    $key_type$ key) {\n" +
        (keyType == Helpers.JavaType.STRING
            ? "  if (key == null) { throw new NullPointerException(\"map key\"); }\n" : "\n") +
        "  return internalGet$capitalized_name$().ensureBuilderMap().containsKey(key);\n" +
        "}\n");

    printer.emit(variables,
        "/**\n" +
        " * Use {@link #get$capitalized_name$Map()} instead.\n" +
        " */\n" +
        "@java.lang.Override\n" +
        "@java.lang.Deprecated\n" +
        "public java.util.Map<$type_parameters$> get$capitalized_name$() {\n" +
        "  return get$capitalized_name$Map();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.Map<$type_parameters$> get$capitalized_name$Map() {\n" +
        "  return internalGet$capitalized_name$().getImmutableMap();\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.print("@java.lang.Override\n");
    printer.print("public ");
    emitNullableType(printer, (String) variables.get("value_type"), "");
    printer.emit(variables,
        " get$capitalized_name$OrDefault(\n" +
        "    $key_type$ key,\n");
    emitNullableType(printer, (String) variables.get("value_type"), "    ");
    printer.emit(variables,
        " defaultValue) {\n" +
        "  $key_null_check$\n" +
        "  java.util.Map<$boxed_key_type$, $value_interface_type$> map = " +
        "internalGetMutable$capitalized_name$().ensureBuilderMap();\n" +
        "  return map.containsKey(key) ? $name$Converter.build(map.get(key)) : defaultValue;\n" +
        "}\n");

    DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $value_type$ get$capitalized_name$OrThrow(\n" +
        "    $key_type$ key) {\n" +
        "  $key_null_check$\n" +
        "  java.util.Map<$boxed_key_type$, $value_interface_type$> map = " +
        "internalGetMutable$capitalized_name$().ensureBuilderMap();\n" +
        "  if (!map.containsKey(key)) {\n" +
        "    throw new java.lang.IllegalArgumentException();\n" +
        "  }\n" +
        "  return $name$Converter.build(map.get(key));\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer)
  {
  }

  @Override
  public void generateBuilderClearCode(Printer printer)
  {
    printer.emit(variables, "internalGetMutable$capitalized_name$().clear();\n");
  }

  @Override
  public void generateMergingCode(Printer printer)
  {
    printer.emit(variables, "internalGetMutable$capitalized_name$().mergeFrom(\n" +
        "    other.internalGet$capitalized_name$());\n" +
        "$set_has_field_bit_builder$\n");
  }

  @Override
  public void generateBuildingCode(Printer printer)
  {
    if (isMessageValue)
    {
      printer.emit(variables,
          "if ($get_has_field_bit_from_local$) {\n" +
          "  result.$name$_ = internalGet$capitalized_name$().build($map_field_parameter$);\n" +
          "}\n");
      return;
    }
    printer.emit(variables,
        "if ($get_has_field_bit_from_local$) {\n" +
        "  result.$name$_ = internalGet$capitalized_name$();\n" +
        "  result.$name$_.makeImmutable();\n" +
        "}\n");
  }

  @Override
  public void generateParsingCode(Printer printer)
  {
    FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
    if (isMessageValue)
    {
      printer.emit(variables,
          "com.google.protobuf.MapEntry<$type_parameters$>\n" +
          "$name$__ = input.readMessage(\n" +
          "    $default_entry$.getParserForType(), extensionRegistry);\n" +
          "internalGetMutable$capitalized_name$().ensureBuilderMap().put(\n" +
          "    $name$__.getKey(), $name$__.getValue());\n" +
          "$set_has_field_bit_builder$\n");
      return;
    }
    if (!InternalHelpers.supportUnknownEnumValue(valueField) && isEnumValue)
    {
      printer.emit(variables,
          "com.google.protobuf.ByteString bytes = input.readBytes();\n" +
          "com.google.protobuf.MapEntry<$type_parameters$>\n" +
          "$name$__ = $default_entry$.getParserForType().parseFrom(bytes);\n" +
          "if ($value_enum_type$.forNumber($name$__.getValue()) == null) {\n" +
          "  mergeUnknownLengthDelimitedField($number$, bytes);\n" +
          "} else {\n" +
          "  internalGetMutable$capitalized_name$().getMutableMap().put(\n" +
          "      $name$__.getKey(), $name$__.getValue());\n" +
          "  $set_has_field_bit_builder$\n" +
          "}\n");
      return;
    }
    printer.emit(variables,
        "com.google.protobuf.MapEntry<$type_parameters$>\n" +
        "$name$__ = input.readMessage(\n" +
        "    $default_entry$.getParserForType(), extensionRegistry);\n" +
        "internalGetMutable$capitalized_name$().getMutableMap().put(\n" +
        "    $name$__.getKey(), $name$__.getValue());\n" +
        "$set_has_field_bit_builder$\n");
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer)
  {
    GeneratorCommon.reportUnexpectedPackedFieldsCall();
  }

  @Override
  public void generateParsingDoneCode(Printer printer)
  {
  }

  @Override
  public void generateSerializedSizeCode(Printer printer)
  {
    printer.emit(variables,
        "for (java.util.Map.Entry<$type_parameters$> entry\n" +
        "     : internalGet$capitalized_name$().getMap().entrySet()) {\n" +
        "  com.google.protobuf.MapEntry<$type_parameters$>\n" +
        "  $name$__ = $default_entry$.newBuilderForType()\n" +
        "      .setKey(entry.getKey())\n" +
        "      .setValue(entry.getValue())\n" +
        "      .build();\n" +
        "  size += com.google.protobuf.CodedOutputStream\n" +
        "      .computeMessageSize($number$, $name$__);\n" +
        "}\n");
  }

  @Override
  public void generateSerializationCode(Printer printer)
  {
    printer.emit(variables,
        "com.google.protobuf.GeneratedMessage\n" +
        "  .serialize$short_key_type$MapTo(\n" +
        "    output,\n" +
        "    internalGet$capitalized_name$(),\n" +
        "    $default_entry$,\n" +
        "    $number$);\n");
  }

  @Override
  public void generateEqualsCode(Printer printer)
  {
    printer.emit(variables,
        "if (!internalGet$capitalized_name$().equals(\n" +
        "    other.internalGet$capitalized_name$())) return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer)
  {
    printer.emit(variables,
        "if (!internalGet$capitalized_name$().getMap().isEmpty()) {\n" +
        "  hash = (37 * hash) + $constant_name$;\n" +
        "  hash = (53 * hash) + internalGet$capitalized_name$().hashCode();\n" +
        "}\n");
  }
}
