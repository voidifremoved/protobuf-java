package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;

/**
 * For generating primitive fields (int, long, float, double, boolean).
 * Ported from java/full/primitive_field.cc.
 */
public class PrimitiveFieldGenerator extends ImmutableFieldGenerator {
  public PrimitiveFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    String typeName = getJavaTypeName(javaType);
    String capitalizedType = getCapitalizedType(descriptor);

    variables.put("type", typeName);
    variables.put("boxed_type", Helpers.getBoxedPrimitiveTypeName(javaType));
    variables.put("capitalized_type", capitalizedType);

    String defaultValue = Helpers.defaultValue(descriptor, true, context.getNameResolver(), context.getOptions());
    variables.put("default", defaultValue);
    variables.put("default_init", defaultValue);

    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) | getWireType(descriptor)));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("null_check", ""); // Primitives don't need null checks
  }

  private int getWireType(FieldDescriptor descriptor) {
      switch (descriptor.getType()) {
          case DOUBLE: return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
          case FLOAT: return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
          case INT32:
          case INT64:
          case UINT32:
          case UINT64:
          case FIXED32:
          case FIXED64:
          case SFIXED32:
          case SFIXED64:
          case SINT32:
          case SINT64:
              // INT32/INT64 etc use VARINT
              if (descriptor.getType() == FieldDescriptor.Type.FIXED64 || descriptor.getType() == FieldDescriptor.Type.SFIXED64) {
                   return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
              }
              if (descriptor.getType() == FieldDescriptor.Type.FIXED32 || descriptor.getType() == FieldDescriptor.Type.SFIXED32) {
                   return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
              }
              return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
          case BOOL: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
          default: throw new RuntimeException("Not a primitive type: " + descriptor.getType());
      }
  }

  private String getJavaTypeName(Helpers.JavaType type) {
    switch (type) {
      case INT: return "int";
      case LONG: return "long";
      case FLOAT: return "float";
      case DOUBLE: return "double";
      case BOOLEAN: return "boolean";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  private String getCapitalizedType(FieldDescriptor descriptor) {
    switch (descriptor.getType()) {
      case INT32: return "Int32";
      case UINT32: return "UInt32";
      case SINT32: return "SInt32";
      case FIXED32: return "Fixed32";
      case SFIXED32: return "SFixed32";
      case INT64: return "Int64";
      case UINT64: return "UInt64";
      case SINT64: return "SInt64";
      case FIXED64: return "Fixed64";
      case SFIXED64: return "SFixed64";
      case FLOAT: return "Float";
      case DOUBLE: return "Double";
      case BOOL: return "Bool";
      case STRING: return "String";
      case BYTES: return "Bytes";
      case ENUM: return "Enum";
      case GROUP: return "Group";
      case MESSAGE: return "Message";
      default: return "";
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
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables, "$type$ get$capitalized_name$();\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    if (Helpers.supportFieldPresence(descriptor)) {
      printer.emit(variables, "private $type$ $name$_ = $default$;\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(messageBitIndex) + ";\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public $type$ get$capitalized_name$() {\n" +
          "  return $name$_;\n" +
          "}\n");
    } else {
      // Proto3 implicit
      printer.emit(variables, "private $type$ $name$_ = $default$;\n");
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
    if (Helpers.supportFieldPresence(descriptor)) {
      if (Helpers.isDefaultValueJavaDefault(descriptor)) {
        printer.emit(variables, "private $type$ $name$_ ;\n");
      } else {
        printer.emit(variables, "private $type$ $name$_ = $default_init$;\n");
      }

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions(), true);
      printer.emit(variables,
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(builderBitIndex) + ";\n" +
          "}\n");

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
      printer.emit(variables,
          "public $type$ get$capitalized_name$() {\n" +
          "  return $name$_;\n" +
          "}\n");

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder set$capitalized_name$($type$ value) {\n" +
          "  $null_check$\n" +
          "  $name$_ = value;\n" +
          "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder clear$capitalized_name$() {\n" +
          "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
          "  $name$_ = $default$;\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
    } else {
      // Proto3 implicit
      printer.emit(variables,
          "private $type$ $name$_;\n"); // Default is 0/false by JVM

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
      printer.emit(variables,
          "public $type$ get$capitalized_name$() {\n" +
          "  return $name$_;\n" +
          "}\n");

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder set$capitalized_name$($type$ value) {\n" +
          "  $null_check$\n" +
          "  $name$_ = value;\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");

      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder clear$capitalized_name$() {\n" +
          "  \n" +
          "  $name$_ = $default$;\n" + // Helper default should be 0/false
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
    }
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    if (!Helpers.isDefaultValueJavaDefault(descriptor)) {
      printer.emit(variables, "$name$_ = $default$;\n");
    }
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
      printer.emit(variables,
          "$name$_ = $default$;\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
    if (descriptor.hasPresence()) {
      printer.emit(variables,
          "if (other.has$capitalized_name$()) {\n" +
          "  set$capitalized_name$(other.get$capitalized_name$());\n" +
          "}\n");
    } else {
      // Proto3 implicit: check if value is not default
      printer.emit(variables,
          "if (other.get$capitalized_name$() != $default$) {\n" +
          "  set$capitalized_name$(other.get$capitalized_name$());\n" +
          "}\n");
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
      printer.emit(variables,
          "result.$name$_ = $name$_;\n");
    }
  }

  @Override
  public void generateParsingCode(Printer printer) {
    printer.emit(variables,
        "$name$_ = input.read$capitalized_type$();\n");
    if (Helpers.supportFieldPresence(descriptor)) {
       printer.emit(variables,
           Helpers.generateSetBit(builderBitIndex) + ";\n");
    }
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer) {
    GeneratorCommon.reportUnexpectedPackedFieldsCall();
  }

  @Override
  public void generateParsingDoneCode(Printer printer) {
    // No post-processing needed for primitives
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    printer.emit(variables,
        "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
        "  output.write$capitalized_type$($number$, $name$_);\n" +
        "}\n");
  }

  @Override
  public void generateSerializedSizeCode(Printer printer) {
    printer.emit(variables,
        "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
        "  size += com.google.protobuf.CodedOutputStream\n" +
        "    .compute$capitalized_type$Size($number$, $name$_);\n" +
        "}\n");
  }

  @Override
  public void generateEqualsCode(Printer printer) {
    if (Helpers.supportFieldPresence(descriptor)) {
      printer.emit(variables,
          "if (has$capitalized_name$() != other.has$capitalized_name$()) return false;\n" +
          "if (has$capitalized_name$()) {\n");
      printer.indent();
    }
    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    if (javaType == Helpers.JavaType.FLOAT) {
        printer.emit(variables,
            "if (java.lang.Float.floatToIntBits(get$capitalized_name$())\n" +
            "    != java.lang.Float.floatToIntBits(\n" +
            "        other.get$capitalized_name$())) return false;\n");
    } else if (javaType == Helpers.JavaType.DOUBLE) {
        printer.emit(variables,
            "if (java.lang.Double.doubleToLongBits(get$capitalized_name$())\n" +
            "    != java.lang.Double.doubleToLongBits(\n" +
            "        other.get$capitalized_name$())) return false;\n");
    } else {
        printer.emit(variables,
            "if (get$capitalized_name$()\n" +
            "    != other.get$capitalized_name$()) return false;\n");
    }
    if (Helpers.supportFieldPresence(descriptor)) {
      printer.outdent();
      printer.print("}\n");
    }
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
    if (Helpers.supportFieldPresence(descriptor)) {
      printer.emit(variables, "if (has$capitalized_name$()) {\n");
      printer.indent();
    }
    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    printer.emit(variables, "hash = (37 * hash) + $constant_name$;\n");
    if (javaType == Helpers.JavaType.INT || javaType == Helpers.JavaType.BOOLEAN) {
        if (javaType == Helpers.JavaType.BOOLEAN) {
             printer.emit(variables, "hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(\n" +
                 "    get$capitalized_name$());\n");
        } else {
             printer.emit(variables, "hash = (53 * hash) + get$capitalized_name$();\n");
        }
    } else if (javaType == Helpers.JavaType.LONG) {
        printer.emit(variables, "hash = (53 * hash) + com.google.protobuf.Internal.hashLong(\n" +
            "    get$capitalized_name$());\n");
    } else if (javaType == Helpers.JavaType.FLOAT) {
        printer.emit(variables, "hash = (53 * hash) + java.lang.Float.floatToIntBits(\n" +
            "    get$capitalized_name$());\n");
    } else if (javaType == Helpers.JavaType.DOUBLE) {
        printer.emit(variables, "hash = (53 * hash) + com.google.protobuf.Internal.hashLong(\n" +
            "    java.lang.Double.doubleToLongBits(get$capitalized_name$()));\n");
    }
    if (Helpers.supportFieldPresence(descriptor)) {
      printer.outdent();
      printer.print("}\n");
    }
  }
}
