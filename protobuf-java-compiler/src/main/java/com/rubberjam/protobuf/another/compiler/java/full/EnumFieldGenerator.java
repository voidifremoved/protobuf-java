package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
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
    } else {
      variables.put("unknown", variables.get("default")); // fallback
    }
  }

  @Override
  public int getNumBitsForMessage() {
    return descriptor.hasPresence() ? 1 : 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return descriptor.hasPresence() ? 1 : 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    if (descriptor.hasPresence()) {
      printer.emit(variables, "boolean has$capitalized_name$();\n");
    }
    printer.emit(variables, "$type$ get$capitalized_name$();\n");

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
       printer.emit(variables, "int get$capitalized_name$Value();\n");
    }
  }

  @Override
  public void generateMembers(Printer printer) {
    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
      printer.emit(variables, "private int $name$_;\n");
    } else {
      printer.emit(variables, "private $type$ $name$_;\n");
    }

    if (descriptor.hasPresence()) {
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(messageBitIndex) + ";\n" +
          "}\n");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
       // getEnumValue() returns int
       printer.emit(variables,
           "@java.lang.Override\n" +
           "public int get$capitalized_name$Value() {\n" +
           "  return $name$_;\n" +
           "}\n");

       // getEnum() converts int to Enum
       printer.emit(variables,
           "@java.lang.Override\n" +
           "public $type$ get$capitalized_name$() {\n" +
           "  $type$ result = $type$.forNumber($name$_);\n" +
           "  return result == null ? $type$.UNRECOGNIZED : result;\n" +
           "}\n");
    } else {
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
       // Init value:
       String defaultNum = "0";
       if (descriptor.hasDefaultValue()) {
          defaultNum = String.valueOf(((com.google.protobuf.Descriptors.EnumValueDescriptor)descriptor.getDefaultValue()).getNumber());
       } else if (descriptor.getEnumType().getValues().size() > 0) {
           // Proto3 default is 0, but technically it's the first value's number (usually 0).
           defaultNum = String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber());
       }
       variables.put("default_number", defaultNum);

       printer.emit(variables, "private int $name$_ = $default_number$;\n");
    } else {
       printer.emit(variables, "private $type$ $name$_ = $default$;\n");
    }

    if (descriptor.hasPresence()) {
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(builderBitIndex) + ";\n" +
          "}\n");
    }

    if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "@java.lang.Override\n" +
            "public int get$capitalized_name$Value() {\n" +
            "  return $name$_;\n" +
            "}\n" +
            "public Builder set$capitalized_name$Value(int value) {\n" +
            "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
            "  $name$_ = value;\n" +
            "  onChanged();\n" +
            "  return this;\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public $type$ get$capitalized_name$() {\n" +
            "  $type$ result = $type$.forNumber($name$_);\n" +
            "  return result == null ? $type$.UNRECOGNIZED : result;\n" +
            "}\n" +
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
            "@java.lang.Override\n" +
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
         // Default for int is 0. If default value number is 0, no init needed if presence not tracked?
         // But let's be safe.
         String defaultNum = "0";
         if (descriptor.hasDefaultValue()) {
             defaultNum = String.valueOf(((com.google.protobuf.Descriptors.EnumValueDescriptor)descriptor.getDefaultValue()).getNumber());
         } else if (descriptor.getEnumType().getValues().size() > 0) {
             defaultNum = String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber());
         }
         variables.put("default_number", defaultNum);
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
     if (descriptor.hasPresence()) {
         printer.emit(variables, Helpers.generateClearBit(builderBitIndex) + ";\n");
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
            // Proto3 shouldn't be here if not supportUnknownEnumValue (which implies open enum)
            // But if it is...
             printer.emit(variables,
                "if (other.get$capitalized_name$() != $default$) {\n" +
                "  set$capitalized_name$(other.get$capitalized_name$());\n" +
                "}\n");
        }
     }
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     if (descriptor.hasPresence()) {
        printer.emit(variables,
          "if (" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
          "  " + Helpers.generateSetBit(messageBitIndex).replace("bitField", "to_bitField") + ";\n" +
          "}\n");
     }
     printer.emit(variables, "result.$name$_ = $name$_;\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "$name$_ = input.readEnum();\n");
        if (descriptor.hasPresence()) {
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
            (descriptor.hasPresence() ? Helpers.generateSetBit(builderBitIndex) + ";\n" : "") +
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
             "if (" + (descriptor.hasPresence() ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default_number$") + ") {\n" +
             "  size += com.google.protobuf.CodedOutputStream\n" +
             "    .computeEnumSize($number$, $name$_);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "if (" + (descriptor.hasPresence() ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
             "  size += com.google.protobuf.CodedOutputStream\n" +
             "    .computeEnumSize($number$, $name$_.getNumber());\n" +
             "}\n");
     }
  }

  @Override
  public void generateSerializationCode(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         printer.emit(variables,
             "if (" + (descriptor.hasPresence() ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default_number$") + ") {\n" +
             "  output.writeEnum($number$, $name$_);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "if (" + (descriptor.hasPresence() ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
             "  output.writeEnum($number$, $name$_.getNumber());\n" +
             "}\n");
     }
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     printer.emit(variables,
         "if (" + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + " != other." + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + ") return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     printer.emit(variables,
         "hash = (37 * hash) + $constant_name$;\n" +
         "hash = (53 * hash) + " + (InternalHelpers.supportUnknownEnumValue(descriptor) ? "$name$_" : "$name$_.getNumber()") + ";\n");
  }
}
