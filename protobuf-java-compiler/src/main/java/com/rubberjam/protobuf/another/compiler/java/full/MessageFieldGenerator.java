package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating message fields.
 * Ported from java/full/message_field.cc.
 */
public class MessageFieldGenerator extends ImmutableFieldGenerator {
  public MessageFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    variables.put("type", context.getNameResolver().getClassName(descriptor.getMessageType(), true));
    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) |
        (descriptor.getType() == FieldDescriptor.Type.GROUP
            ? com.google.protobuf.WireFormat.WIRETYPE_START_GROUP
            : com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED)));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));

    // Group support
    variables.put("is_group", descriptor.getType() == FieldDescriptor.Type.GROUP);
  }

  @Override
  public int getNumBitsForMessage() {
    return 1;
  }

  @Override
  public int getNumBitsForBuilder() {
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    printer.emit(variables,
        "boolean has$capitalized_name$();\n" +
        "$type$ get$capitalized_name$();\n" +
        "$type$OrBuilder get$capitalized_name$OrBuilder();\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables,
        "private $type$ $name$_;\n");

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean has$capitalized_name$() {\n" +
        "  return " + Helpers.generateGetBit(messageBitIndex) + ";\n" +
        "}\n");

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ get$capitalized_name$() {\n" +
        "  return $name$_ == null ? $type$.getDefaultInstance() : $name$_;\n" +
        "}\n");

    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$OrBuilder get$capitalized_name$OrBuilder() {\n" +
        "  return $name$_ == null ? $type$.getDefaultInstance() : $name$_;\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    // We use SingleFieldBuilderV3 for message fields in builders to support nested builders.
    printer.emit(variables,
        "private $type$ $name$_;\n" +
        "private com.google.protobuf.SingleFieldBuilderV3<\n" +
        "    $type$, $type$.Builder, $type$OrBuilder> $name$Builder_;\n");

    // has
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public boolean has$capitalized_name$() {\n" +
        "  return " + Helpers.generateGetBit(builderBitIndex) + " || $name$Builder_ != null;\n" +
        "}\n");

    // get
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ get$capitalized_name$() {\n" +
        "  if ($name$Builder_ == null) {\n" +
        "    return $name$_ == null ? $type$.getDefaultInstance() : $name$_;\n" +
        "  } else {\n" +
        "    return $name$Builder_.getMessage();\n" +
        "  }\n" +
        "}\n");

    // set
    printer.emit(variables,
        "public Builder set$capitalized_name$($type$ value) {\n" +
        "  if ($name$Builder_ == null) {\n" +
        "    if (value == null) {\n" +
        "      throw new NullPointerException();\n" +
        "    }\n" +
        "    $name$_ = value;\n" +
        "  } else {\n" +
        "    $name$Builder_.setMessage(value);\n" +
        "  }\n" +
        "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    // set builder
    printer.emit(variables,
        "public Builder set$capitalized_name$(\n" +
        "    $type$.Builder builderForValue) {\n" +
        "  if ($name$Builder_ == null) {\n" +
        "    $name$_ = builderForValue.build();\n" +
        "  } else {\n" +
        "    $name$Builder_.setMessage(builderForValue.build());\n" +
        "  }\n" +
        "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    // merge
    printer.emit(variables,
        "public Builder merge$capitalized_name$($type$ value) {\n" +
        "  if ($name$Builder_ == null) {\n" +
        "    if (" + Helpers.generateGetBit(builderBitIndex) + " &&\n" +
        "        $name$_ != null &&\n" +
        "        $name$_ != $type$.getDefaultInstance()) {\n" +
        "      get$capitalized_name$Builder().mergeFrom(value);\n" +
        "    } else {\n" +
        "      $name$_ = value;\n" +
        "    }\n" +
        "  } else {\n" +
        "    $name$Builder_.mergeFrom(value);\n" +
        "  }\n" +
        "  if ($name$_ != null) {\n" +
        "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "    onChanged();\n" +
        "  }\n" +
        "  return this;\n" +
        "}\n");

    // clear
    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
        "  $name$_ = null;\n" +
        "  if ($name$Builder_ != null) {\n" +
        "    $name$Builder_.dispose();\n" +
        "    $name$Builder_ = null;\n" +
        "  }\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    // getBuilder
    printer.emit(variables,
        "public $type$.Builder get$capitalized_name$Builder() {\n" +
        "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "  onChanged();\n" +
        "  return get$capitalized_name$FieldBuilder().getBuilder();\n" +
        "}\n");

    // getOrBuilder
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$OrBuilder get$capitalized_name$OrBuilder() {\n" +
        "  if ($name$Builder_ != null) {\n" +
        "    return $name$Builder_.getMessageOrBuilder();\n" +
        "  } else {\n" +
        "    return $name$_ == null ?\n" +
        "        $type$.getDefaultInstance() : $name$_;\n" +
        "  }\n" +
        "}\n");

    // getFieldBuilder (private)
    printer.emit(variables,
        "private com.google.protobuf.SingleFieldBuilderV3<\n" +
        "    $type$, $type$.Builder, $type$OrBuilder> \n" +
        "    get$capitalized_name$FieldBuilder() {\n" +
        "  if ($name$Builder_ == null) {\n" +
        "    $name$Builder_ = new com.google.protobuf.SingleFieldBuilderV3<\n" +
        "        $type$, $type$.Builder, $type$OrBuilder>(\n" +
        "            get$capitalized_name$(),\n" +
        "            getParentForChildren(),\n" +
        "            isClean());\n" +
        "    $name$_ = null;\n" +
        "  }\n" +
        "  return $name$Builder_;\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     printer.emit(variables, "$name$_ = null;\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     printer.emit(variables,
         "$name$_ = null;\n" +
         "if ($name$Builder_ != null) {\n" +
         "  $name$Builder_.dispose();\n" +
         "  $name$Builder_ = null;\n" +
         "}\n" +
         Helpers.generateClearBit(builderBitIndex) + ";\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
     printer.emit(variables,
         "if (other.has$capitalized_name$()) {\n" +
         "  merge$capitalized_name$(other.get$capitalized_name$());\n" +
         "}\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     printer.emit(variables,
         "if (" + Helpers.generateGetBit("from_", builderBitIndex) + ") {\n" +
         "  " + Helpers.generateSetBit(messageBitIndex).replace("bitField", "to_bitField") + ";\n" +
         "}\n");

     printer.emit(variables,
         "if ($name$Builder_ == null) {\n" +
         "  result.$name$_ = $name$_;\n" +
         "} else {\n" +
         "  result.$name$_ = $name$Builder_.build();\n" +
         "}\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     printer.emit(variables,
         "input.readMessage(\n" +
         "    get$capitalized_name$FieldBuilder().getBuilder(),\n" +
         "    extensionRegistry);\n" +
         Helpers.generateSetBit(builderBitIndex) + ";\n");
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
    // computeGroupSize or computeMessageSize
    if (descriptor.getType() == FieldDescriptor.Type.GROUP) {
        printer.emit(variables,
            "if (" + Helpers.generateGetBit(messageBitIndex) + ") {\n" +
            "  size += com.google.protobuf.CodedOutputStream\n" +
            "    .computeGroupSize($number$, get$capitalized_name$());\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "if (" + Helpers.generateGetBit(messageBitIndex) + ") {\n" +
            "  size += com.google.protobuf.CodedOutputStream\n" +
            "    .computeMessageSize($number$, get$capitalized_name$());\n" +
            "}\n");
    }
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    if (descriptor.getType() == FieldDescriptor.Type.GROUP) {
        printer.emit(variables,
            "if (" + Helpers.generateGetBit(messageBitIndex) + ") {\n" +
            "  output.writeGroup($number$, get$capitalized_name$());\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "if (" + Helpers.generateGetBit(messageBitIndex) + ") {\n" +
            "  output.writeMessage($number$, get$capitalized_name$());\n" +
            "}\n");
    }
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     printer.emit(variables,
         "if (!get$capitalized_name$()\n" +
         "    .equals(other.get$capitalized_name$())) return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     printer.emit(variables,
         "hash = (37 * hash) + $constant_name$;\n" +
         "hash = (53 * hash) + get$capitalized_name$().hashCode();\n");
  }
}
