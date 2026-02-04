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

    // For unknown enum values support (usually true for proto3, false for proto2 unless legacy_closed_enum=false)
    boolean supportUnknownEnumValue = InternalHelpers.supportUnknownEnumValue(descriptor);
    variables.put("support_unknown_enum_value", supportUnknownEnumValue);

    if (supportUnknownEnumValue) {
      variables.put("unknown", descriptor.getEnumType().getName() + ".UNRECOGNIZED");
      String defaultNum = "0";
      if (descriptor.hasDefaultValue()) {
          defaultNum = String.valueOf(((com.google.protobuf.Descriptors.EnumValueDescriptor)descriptor.getDefaultValue()).getNumber());
      } else if (descriptor.getEnumType().getValues().size() > 0) {
          // Proto3 default is 0, but technically it's the first value's number (usually 0).
          defaultNum = String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber());
      }
      variables.put("default_number", defaultNum);
    } else {
      variables.put("unknown", variables.get("default")); // fallback
    }
  }

  @Override
  public int getNumBitsForMessage() {
    return Helpers.supportFieldPresence(descriptor) ? 1 : 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return Helpers.supportFieldPresence(descriptor) ? 1 : 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    if (Helpers.supportFieldPresence(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables, "boolean has$capitalized_name$();\n");
    }
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
       DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
       printer.emit(variables, "int get$capitalized_name$Value();\n");
    }
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables, "$type$ get$capitalized_name$();\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      printer.emit(variables, "private int $name$_;\n");
    } else {
      printer.emit(variables, "private $type$ $name$_;\n");
    }

    if (Helpers.supportFieldPresence(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(messageBitIndex) + ";\n" +
          "}\n");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
       // getEnumValue() returns int
       DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
       printer.emit(variables,
           "@java.lang.Override\n" +
           "public int get$capitalized_name$Value() {\n" +
           "  return $name$_;\n" +
           "}\n");

       // getEnum() converts int to Enum
       DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
       printer.emit(variables,
           "@java.lang.Override\n" +
           "public $type$ get$capitalized_name$() {\n" +
           "  $type$ result = $type$.forNumber($name$_);\n" +
           "  return result == null ? $type$.UNRECOGNIZED : result;\n" +
           "}\n");
    } else {
       DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
       printer.emit(variables,
           "@java.lang.Override\n" +
           "public $type$ get$capitalized_name$() {\n" +
           "  return $name$_;\n" +
           "}\n");
    }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
       // Storage is int.
       printer.emit(variables, "private int $name$_ = $default_number$;\n");
    } else {
       printer.emit(variables, "private $type$ $name$_ = $default$;\n");
    }

    if (Helpers.supportFieldPresence(descriptor)) {
      printer.emit(variables,
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(builderBitIndex) + ";\n" +
          "}\n");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
        printer.emit(variables,
            "public int get$capitalized_name$Value() {\n" +
            "  return $name$_;\n" +
            "}\n");
        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$Value(int value) {\n" +
            "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
            "  $name$_ = value;\n" +
            "  onChanged();\n" +
            "  return this;\n" +
            "}\n");
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
        printer.emit(variables,
            "public $type$ get$capitalized_name$() {\n" +
            "  $type$ result = $type$.forNumber($name$_);\n" +
            "  return result == null ? $type$.UNRECOGNIZED : result;\n" +
            "}\n");
        DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
        printer.emit(variables,
            "public Builder set$capitalized_name$($type$ value) {\n" +
            "  if (value == null) {\n" +
            "    throw new NullPointerException();\n" +
            "  }\n" +
            "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
            "  $name$_ = value.getNumber();\n" +
            "  onChanged();\n" +
            "  return this;\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "public $type$ get$capitalized_name$() {\n" +
            "  return $name$_;\n" +
            "}\n" +
            "public Builder set$capitalized_name$($type$ value) {\n" +
            "  if (value == null) {\n" +
            "    throw new NullPointerException();\n" +
            "  }\n" +
            "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
            "  $name$_ = value;\n" +
            "  onChanged();\n" +
            "  return this;\n" +
            "}\n");
    }

    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
        (InternalHelpers.supportUnknownEnumValue(descriptor)
            ? "  $name$_ = $default_number$;\n"
            : "  $name$_ = $default$;\n") +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         printer.emit(variables, "$name$_ = $default_number$;\n");
     } else {
         printer.emit(variables, "$name$_ = $default$;\n");
     }
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables, "$name$_ = $default_number$;\n");
     } else {
        printer.emit(variables, "$name$_ = $default$;\n");
     }
  }

  @Override
  public void generateMergingCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
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
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.emit(variables,
          "if (" + Helpers.generateGetBit("from_", builderBitIndex) + ") {\n" +
          "  result.$name$_ = $name$_;\n" +
          "  " + Helpers.generateSetBit(messageBitIndex).replace("bitField", "to_bitField") + ";\n" +
          "}\n");
     } else {
         printer.emit(variables, "result.$name$_ = $name$_;\n");
     }
  }

  @Override
  public void generateParsingCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "$name$_ = input.readEnum();\n");
        if (Helpers.supportFieldPresence(descriptor)) {
             printer.emit(variables, Helpers.generateSetBit(builderBitIndex) + ";\n");
        }
     } else {
        // Legacy closed enum: readEnum returns int, need to verify
        printer.emit(variables,
            "int rawValue = input.readEnum();\n" +
            "$type$ value = $type$.forNumber(rawValue);\n" +
            "if (value == null) {\n" +
            "  unknownFields.mergeVarintField($number$, rawValue);\n" +
            "} else {\n" +
            (Helpers.supportFieldPresence(descriptor) ? Helpers.generateSetBit(builderBitIndex) + ";\n" : "") +
            "  $name$_ = value;\n" +
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
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         printer.emit(variables,
             "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default_number$") + ") {\n" +
             "  size += com.google.protobuf.CodedOutputStream\n" +
             "    .computeEnumSize($number$, $name$_);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
             "  size += com.google.protobuf.CodedOutputStream\n" +
             "    .computeEnumSize($number$, $name$_.getNumber());\n" +
             "}\n");
     }
  }

  @Override
  public void generateSerializationCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         printer.emit(variables,
             "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default_number$") + ") {\n" +
             "  output.writeEnum($number$, $name$_);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
             "  output.writeEnum($number$, $name$_.getNumber());\n" +
             "}\n");
     }
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.emit(variables,
            "if (has$capitalized_name$() != other.has$capitalized_name$()) return false;\n" +
            "if (has$capitalized_name$()) {\n");
        printer.indent();
     }
     printer.emit(variables,
         "if (" + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + " != other." + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + ") return false;\n");
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.outdent();
        printer.print("}\n");
     }
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.emit(variables,
            "if (has$capitalized_name$()) {\n");
        printer.indent();
     }
     printer.emit(variables,
         "hash = (37 * hash) + $constant_name$;\n" +
         "hash = (53 * hash) + " + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + ";\n");
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.outdent();
        printer.print("}\n");
     }
  }
}
