package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating enum fields.
 * Ported from java/full/enum_field.cc.
 */
public class EnumFieldGenerator extends ImmutableFieldGenerator {
  public EnumFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    variables.put("type", Helpers.getJavaType(descriptor) == Helpers.JavaType.ENUM
        ? context.getNameResolver().getClassName(descriptor.getEnumType(), true)
        : "");
    variables.put("default", Helpers.defaultValue(descriptor, true, context.getNameResolver(), context.getOptions()));
    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_VARINT));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));

    // For unknown enum values support (usually true for proto3, false for proto2
    // unless legacy_closed_enum=false)
    boolean supportUnknownEnumValue = InternalHelpers.supportUnknownEnumValue(descriptor);
    variables.put("support_unknown_enum_value", supportUnknownEnumValue);

    String defaultNum = "0";
    if (descriptor.hasDefaultValue()) {
      defaultNum = String
          .valueOf(((com.google.protobuf.Descriptors.EnumValueDescriptor) descriptor.getDefaultValue()).getNumber());
    } else if (descriptor.getEnumType().getValues().size() > 0) {
      defaultNum = String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber());
    }
    variables.put("default_number", defaultNum);

    if (supportUnknownEnumValue) {
      variables.put("unknown", variables.get("type") + ".UNRECOGNIZED");
    } else {
      variables.put("unknown", variables.get("default"));
    }
  }

  @Override
  public int getNumBitsForMessage() {
    return Helpers.hasHasbit(descriptor) ? 1 : 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    if (Helpers.supportFieldPresence(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER,
          context.getOptions());
      printer.emit(variables, "boolean has$capitalized_name$();\n");
    }
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER,
          context.getOptions());
      printer.emit(variables, "int get$capitalized_name$Value();\n");
    }
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables, "$type$ get$capitalized_name$();\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER,
          context.getOptions());
      printer.emit(variables,
          "public boolean has$capitalized_name$() {\n" +
              "  return $oneof_name$Case_ == $number$;\n" +
              "}\n");
    } else {
      printer.emit(variables, "private int $name$_ = $default_number$;\n");

      if (descriptor.hasPresence()) {
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER,
            context.getOptions());
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public boolean has$capitalized_name$() {\n" +
                "  return "
                + (Helpers.hasHasbit(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$")
                + ";\n" +
                "}\n");
      }
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      // getEnumValue() returns int
      DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER,
          context.getOptions());
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "public int get$capitalized_name$Value() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    return (java.lang.Integer) $oneof_name$_;\n" +
                "  }\n" +
                "  return $default_number$;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override public int get$capitalized_name$Value() {\n" +
                "  return $name$_;\n" +
                "}\n");
      }

      // getEnum() converts int to Enum
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER,
          context.getOptions());
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "public $type$ get$capitalized_name$() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    $type$ result = $type$.forNumber(\n" +
                "        (java.lang.Integer) $oneof_name$_);\n" +
                "    return result == null ? $unknown$ : result;\n" +
                "  }\n" +
                "  return $default$;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override public $type$ get$capitalized_name$() {\n" +
                "  $type$ result = $type$.forNumber($name$_);\n" +
                "  return result == null ? $unknown$ : result;\n" +
                "}\n");
      }
    } else {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER,
          context.getOptions());
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "public $type$ get$capitalized_name$() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    return ($type$) $oneof_name$_;\n" +
                "  }\n" +
                "  return $default$;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override public $type$ get$capitalized_name$() {\n" +
                "  return $name$_;\n" +
                "}\n");
      }
    }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions(),
          true);
      printer.emit(variables,
          "@java.lang.Override\n" +
              "public boolean has$capitalized_name$() {\n" +
              "  return $oneof_name$Case_ == $number$;\n" +
              "}\n");
    } else {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        // Storage is int.
        printer.emit(variables, "private int $name$_ = $default_number$;\n");
      } else {
        printer.emit(variables, "private $type$ $name$_ = $default$;\n");
      }

      if (descriptor.hasPresence()) {
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER,
            context.getOptions(), true);
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public boolean has$capitalized_name$() {\n" +
                "  return "
                + (Helpers.hasHasbit(descriptor) ? Helpers.generateGetBit(builderBitIndex) : "$name$_ != $default$")
                + ";\n" +
                "}\n");
      }
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER,
          context.getOptions(), true);
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public int get$capitalized_name$Value() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    return ((java.lang.Integer) $oneof_name$_).intValue();\n" +
                "  }\n" +
                "  return $default_number$;\n" +
                "}\n");
        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
            context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$Value(int value) {\n" +
                "  $oneof_name$Case_ = $number$;\n" +
                "  $oneof_name$_ = value;\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override public int get$capitalized_name$Value() {\n" +
                "  return $name$_;\n" +
                "}\n");
        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
            context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$Value(int value) {\n" +
                "  $name$_ = value;\n" +
                "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      }
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(),
          true);
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public $type$ get$capitalized_name$() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    $type$ result = $type$.forNumber(\n" +
                "        (java.lang.Integer) $oneof_name$_);\n" +
                "    return result == null ? $unknown$ : result;\n" +
                "  }\n" +
                "  return $default$;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public $type$ get$capitalized_name$() {\n" +
                "  $type$ result = $type$.forNumber($name$_);\n" +
                "  return result == null ? $unknown$ : result;\n" +
                "}\n");
      }
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(),
          true);
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "public Builder set$capitalized_name$($type$ value) {\n" +
                "  if (value == null) { throw new NullPointerException(); }\n" +
                "  $oneof_name$Case_ = $number$;\n" +
                "  $oneof_name$_ = value.getNumber();\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "public Builder set$capitalized_name$($type$ value) {\n" +
                "  if (value == null) { throw new NullPointerException(); }\n" +
                "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
                "  $name$_ = value.getNumber();\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      }
    } else {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(),
          true);
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public $type$ get$capitalized_name$() {\n" +
                "  if ($oneof_name$Case_ == $number$) {\n" +
                "    $type$ result = $type$.forNumber(\n" +
                "        (java.lang.Integer) $oneof_name$_);\n" +
                "    return result == null ? $unknown$ : result;\n" +
                "  }\n" +
                "  return $default$;\n" +
                "}\n");
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
            context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$($type$ value) {\n" +
                "  if (value == null) { throw new NullPointerException(); }\n" +
                "  $oneof_name$Case_ = $number$;\n" +
                "  $oneof_name$_ = value;\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "@java.lang.Override\n" +
                "public $type$ get$capitalized_name$() {\n" +
                "  return $name$_;\n" +
                "}\n");
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
            context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$($type$ value) {\n" +
                "  if (value == null) { throw new NullPointerException(); }\n" +
                "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
                "  $name$_ = value;\n" +
                "  onChanged();\n" +
                "  return this;\n" +
                "}\n");
      }
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(),
        true);
    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n");
    if (Helpers.isRealOneof(descriptor)) {
      printer.emit(variables,
          "  if ($oneof_name$Case_ == $number$) {\n" +
              "    $oneof_name$Case_ = 0;\n" +
              "    $oneof_name$_ = null;\n" +
              "    onChanged();\n" +
              "  }\n");
    } else {
      printer.emit(variables,
          "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
              (InternalHelpers.supportUnknownEnumValue(descriptor)
                  ? "  $name$_ = $default_number$;\n"
                  : "  $name$_ = $default$;\n")
              +
              "  onChanged();\n");
    }
    printer.emit("  return this;\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    printer.emit(variables, "$name$_ = $default_number$;\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      return;
    }
    printer.emit(variables, "$name$_ = $default_number$;\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      printer.emit(variables,
          "set$capitalized_name$Value(other.get$capitalized_name$Value());\n");
    } else if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      if (descriptor.hasPresence()) {
        printer.emit(variables,
            "if (other.has$capitalized_name$()) {\n" +
                "  set$capitalized_name$Value(other.get$capitalized_name$Value());\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "if (other.$name$_ != $default_number$) {\n" +
                "  set$capitalized_name$Value(other.get$capitalized_name$Value());\n" +
                "}\n");
      }
    } else {
      if (descriptor.hasPresence()) {
        printer.emit(variables,
            "if (other.has$capitalized_name$()) {\n" +
                "  set$capitalized_name$(other.get$capitalized_name$());\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "if (other.get$capitalized_name$() != $default$) {\n" +
                "  set$capitalized_name$(other.get$capitalized_name$());\n" +
                "}\n");
      }
    }
  }

  @Override
  public void generateBuildingCode(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      return;
    }
    printer.emit(variables,
        "if ($get_has_field_bit_from_local$) {\n" +
            "  result.$name$_ = $name$_;\n");
    if (getNumBitsForMessage() > 0) {
      printer.emit(variables, "  $set_has_field_bit_to_local$;\n");
    }
    printer.print("}\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      printer.emit(variables,
          "int rawValue = input.readEnum();\n");
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "$oneof_name$Case_ = $number$;\n" +
                "$oneof_name$_ = rawValue;\n");
      } else {
        printer.emit(variables, "$name$_ = rawValue;\n");
        if (Helpers.hasHasbit(descriptor)) {
          printer.emit(variables, Helpers.generateSetBit(builderBitIndex) + ";\n");
        }
      }
    } else {
      // Legacy closed enum: readEnum returns int, need to verify
      printer.emit(variables,
          "int rawValue = input.readEnum();\n" +
              "$type$ value = $type$.forNumber(rawValue);\n" +
              "if (value == null) {\n" +
              "  unknownFields.mergeVarintField($number$, rawValue);\n" +
              "} else {\n" +
              (Helpers.isRealOneof(descriptor) ? "  $oneof_name$Case_ = $number$;\n" +
                  "  $oneof_name$_ = value;\n"
                  : (Helpers.hasHasbit(descriptor) ? "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" : "") +
                      "  $name$_ = value;\n")
              +
              "}\n");
    }
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
    if (Helpers.isRealOneof(descriptor)) {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "if ($oneof_name$Case_ == $number$) {\n" +
                "  size += com.google.protobuf.CodedOutputStream\n" +
                "    .computeEnumSize($number$, (java.lang.Integer) $oneof_name$_);\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "if ($oneof_name$Case_ == $number$) {\n" +
                "  size += com.google.protobuf.CodedOutputStream\n" +
                "    .computeEnumSize($number$, (($type$) $oneof_name$_).getNumber());\n" +
                "}\n");
      }
      return;
    }

    String condition;
    if (Helpers.hasHasbit(descriptor)) {
      condition = Helpers.generateGetBit(messageBitIndex);
    } else {
      condition = (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_ != $default$.getNumber()"
          : "$name$_ != $default$");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      printer.emit(variables,
          "if (" + condition + ") {\n" +
              "  size += com.google.protobuf.CodedOutputStream\n" +
              "    .computeEnumSize($number$, $name$_);\n" +
              "}\n");
    } else {
      printer.emit(variables,
          "if (" + condition + ") {\n" +
              "  size += com.google.protobuf.CodedOutputStream\n" +
              "    .computeEnumSize($number$, $name$_.getNumber());\n" +
              "}\n");
    }
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    if (Helpers.isRealOneof(descriptor)) {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "if ($oneof_name$Case_ == $number$) {\n" +
                "  output.writeEnum($number$, (java.lang.Integer) $oneof_name$_);\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "if ($oneof_name$Case_ == $number$) {\n" +
                "  output.writeEnum($number$, (($type$) $oneof_name$_).getNumber());\n" +
                "}\n");
      }
      return;
    }

    String condition;
    if (Helpers.hasHasbit(descriptor)) {
      condition = Helpers.generateGetBit(messageBitIndex);
    } else {
      condition = (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_ != $default$.getNumber()"
          : "$name$_ != $default$");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      printer.emit(variables,
          "if (" + condition + ") {\n" +
              "  output.writeEnum($number$, $name$_);\n" +
              "}\n");
    } else {
      printer.emit(variables,
          "if (" + condition + ") {\n" +
              "  output.writeEnum($number$, $name$_.getNumber());\n" +
              "}\n");
    }
  }

  @Override
  public void generateEqualsCode(Printer printer) {
    if (!Helpers.isRealOneof(descriptor) && descriptor.hasPresence()) {
      printer.emit(variables,
          "if (has$capitalized_name$() != other.has$capitalized_name$()) return false;\n" +
              "if (has$capitalized_name$()) {\n");
      printer.indent();
    }
    if (Helpers.isRealOneof(descriptor)) {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "if (get$capitalized_name$Value()\n" +
                "    != other.get$capitalized_name$Value()) return false;\n");
      } else {
        printer.emit(variables,
            "if (!get$capitalized_name$()\n" +
                "    .equals(other.get$capitalized_name$())) return false;\n");
      }
    } else {
      printer.emit(variables,
          "if ($name$_ != other.$name$_) return false;\n");
    }
    if (!Helpers.isRealOneof(descriptor) && descriptor.hasPresence()) {
      printer.outdent();
      printer.print("}\n");
    }
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
    if (!Helpers.isRealOneof(descriptor) && descriptor.hasPresence()) {
      printer.emit(variables,
          "if (has$capitalized_name$()) {\n");
      printer.indent();
    }
    if (Helpers.isRealOneof(descriptor)) {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "hash = (37 * hash) + $constant_name$;\n" +
                "hash = (53 * hash) + get$capitalized_name$Value();\n");
      } else {
        printer.emit(variables,
            "hash = (37 * hash) + $constant_name$;\n" +
                "hash = (53 * hash) + get$capitalized_name$().getNumber();\n");
      }
    } else {
      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "hash = (37 * hash) + $constant_name$;\n" +
                "hash = (53 * hash) + $name$_;\n");
      } else {
        printer.emit(variables,
            "hash = (37 * hash) + $constant_name$;\n" +
                "hash = (53 * hash) + $name$_.getNumber();\n");
      }
    }
    if (!Helpers.isRealOneof(descriptor) && descriptor.hasPresence()) {
      printer.outdent();
      printer.print("}\n");
    }
  }

  @Override
  public void generateBuilderParsingCode(Printer printer) {
    int tag = (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_VARINT;
    printer.print("case " + tag + ": {\n");
    printer.indent();
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "int rawValue = input.readEnum();\n" +
                "$oneof_name$Case_ = $number$;\n" +
                "$oneof_name$_ = rawValue;\n");
      } else {
        printer.emit(variables,
            "$name$_ = input.readEnum();\n" +
                Helpers.generateSetBit(builderBitIndex) + ";\n");
      }
    } else {
      if (Helpers.isRealOneof(descriptor)) {
        printer.emit(variables,
            "int rawValue = input.readEnum();\n" +
                "$type$ value =\n" +
                "    $type$.forNumber(rawValue);\n" +
                "if (value == null) {\n" +
                "  mergeUnknownVarintField($number$, rawValue);\n" +
                "} else {\n" +
                "  $oneof_name$Case_ = $number$;\n" +
                "  $oneof_name$_ = rawValue;\n" +
                "}\n");
      } else {
        printer.emit(variables,
            "int tmpRaw = input.readEnum();\n" +
                "$type$ tmpValue =\n" +
                "    $type$.forNumber(tmpRaw);\n" +
                "if (tmpValue == null) {\n" +
                "  mergeUnknownVarintField($number$, tmpRaw);\n" +
                "} else {\n" +
                "  $name$_ = tmpRaw;\n" +
                "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
                "}\n");
      }
    }
    printer.print("break;\n");
    printer.outdent();
    printer.print("} // case " + tag + "\n");
  }
}
