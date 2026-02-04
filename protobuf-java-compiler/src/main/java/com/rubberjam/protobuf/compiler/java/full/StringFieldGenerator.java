package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

/**
 * For generating string and bytes fields.
 * Ported from java/full/string_field.cc.
 */
public class StringFieldGenerator extends ImmutableFieldGenerator {
  public StringFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    variables.put("type", Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING
        ? "java.lang.String" : "com.google.protobuf.ByteString");

    String defaultValue = Helpers.defaultValue(descriptor, true, context.getNameResolver(), context.getOptions());
    variables.put("default", defaultValue);
    variables.put("default_init", defaultValue);

    variables.put("tag", String.valueOf(
        (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));

    variables.put("null_check",
        "if (value == null) { throw new NullPointerException(); }");
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

    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.BYTES_GETTER, context.getOptions());
      printer.emit(variables,
          "com.google.protobuf.ByteString\n" +
          "    get$capitalized_name$Bytes();\n");
    }
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "@SuppressWarnings(\"serial\")\n");
    printer.emit(variables, "private volatile java.lang.Object $name$_ = $default$;\n");

    if (Helpers.supportFieldPresence(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(messageBitIndex) + ";\n" +
          "}\n");
    }

    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public java.lang.String get$capitalized_name$() {\n" +
          "  java.lang.Object ref = $name$_;\n" +
          "  if (ref instanceof java.lang.String) {\n" +
          "    return (java.lang.String) ref;\n" +
          "  } else {\n" +
        "    com.google.protobuf.ByteString bs = \n" +
          "        (com.google.protobuf.ByteString) ref;\n" +
          "    java.lang.String s = bs.toStringUtf8();\n" +
          (InternalHelpers.checkUtf8(descriptor) ?
          "    $name$_ = s;\n" :
          "    if (bs.isValidUtf8()) {\n" +
          "      $name$_ = s;\n" +
          "    }\n") +
          "    return s;\n" +
          "  }\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.BYTES_GETTER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public com.google.protobuf.ByteString\n" +
          "    get$capitalized_name$Bytes() {\n" +
          "  java.lang.Object ref = $name$_;\n" +
          "  if (ref instanceof java.lang.String) {\n" +
        "    com.google.protobuf.ByteString b = \n" +
          "        com.google.protobuf.ByteString.copyFromUtf8(\n" +
          "            (java.lang.String) ref);\n" +
          "    $name$_ = b;\n" +
          "    return b;\n" +
          "  } else {\n" +
          "    return (com.google.protobuf.ByteString) ref;\n" +
          "  }\n" +
          "}\n");
    } else {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "public com.google.protobuf.ByteString get$capitalized_name$() {\n" +
          "  return (com.google.protobuf.ByteString) $name$_;\n" +
          "}\n");
    }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    printer.emit(variables,
        "private java.lang.Object $name$_ = $default$;\n");

    if (Helpers.supportFieldPresence(descriptor)) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions(), true);
      printer.emit(variables,
          "public boolean has$capitalized_name$() {\n" +
          "  return " + Helpers.generateGetBit(builderBitIndex) + ";\n" +
          "}\n");
    }

    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
      printer.emit(variables,
          "public java.lang.String get$capitalized_name$() {\n" +
          "  java.lang.Object ref = $name$_;\n" +
          "  if (!(ref instanceof java.lang.String)) {\n" +
        "    com.google.protobuf.ByteString bs =\n" +
          "        (com.google.protobuf.ByteString) ref;\n" +
          "    java.lang.String s = bs.toStringUtf8();\n" +
          (InternalHelpers.checkUtf8(descriptor) ?
          "    $name$_ = s;\n" :
          "    if (bs.isValidUtf8()) {\n" +
          "      $name$_ = s;\n" +
          "    }\n") +
          "    return s;\n" +
          "  } else {\n" +
          "    return (java.lang.String) ref;\n" +
          "  }\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.BYTES_GETTER, context.getOptions(), true);
      printer.emit(variables,
          "public com.google.protobuf.ByteString\n" +
          "    get$capitalized_name$Bytes() {\n" +
          "  java.lang.Object ref = $name$_;\n" +
          "  if (ref instanceof String) {\n" +
        "    com.google.protobuf.ByteString b = \n" +
          "        com.google.protobuf.ByteString.copyFromUtf8(\n" +
          "            (java.lang.String) ref);\n" +
          "    $name$_ = b;\n" +
          "    return b;\n" +
          "  } else {\n" +
          "    return (com.google.protobuf.ByteString) ref;\n" +
          "  }\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder set$capitalized_name$(\n" +
          "    java.lang.String value) {\n" +
          "  $null_check$\n" +
          "  $name$_ = value;\n" +
          "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder clear$capitalized_name$() {\n" +
          "  $name$_ = getDefaultInstance().get$capitalized_name$();\n" +
          "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.BYTES_SETTER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder set$capitalized_name$Bytes(\n" +
          "    com.google.protobuf.ByteString value) {\n" +
          "  $null_check$\n" +
          (InternalHelpers.checkUtf8(descriptor) ? "  checkByteStringIsUtf8(value);\n" : "") +
          "  $name$_ = value;\n" +
          "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
    } else {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions(), true);
      printer.emit(variables,
          "public com.google.protobuf.ByteString get$capitalized_name$() {\n" +
          "  return (com.google.protobuf.ByteString) $name$_;\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder set$capitalized_name$(com.google.protobuf.ByteString value) {\n" +
          "  $null_check$" +
          "  $name$_ = value;\n" +
          "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
      printer.emit(variables,
          "public Builder clear$capitalized_name$() {\n" +
          "  $name$_ = $default$;\n" +
          "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n");
    }
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     printer.emit(variables, "$name$_ = $default$;\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     printer.emit(variables, "$name$_ = $default$;\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
    if (descriptor.hasPresence()) {
       printer.emit(variables,
           "if (other.has$capitalized_name$()) {\n" +
           "  $name$_ = other.$name$_;\n" +
           "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
           "  onChanged();\n" +
           "}\n");
    } else {
       // Proto3 implicit string
       printer.emit(variables,
           "if (!other.get$capitalized_name$().isEmpty()) {\n" +
           "  $name$_ = other.$name$_;\n" +
           "  onChanged();\n" +
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
    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
       if (InternalHelpers.checkUtf8(descriptor)) {
           printer.emit(variables,
               "$name$_ = input.readStringRequireUtf8();\n" +
               (Helpers.supportFieldPresence(descriptor) ? Helpers.generateSetBit(builderBitIndex) + ";\n" : ""));
       } else {
           printer.emit(variables,
               "$name$_ = input.readBytes();\n" +
               (Helpers.supportFieldPresence(descriptor) ? Helpers.generateSetBit(builderBitIndex) + ";\n" : ""));
       }
    } else {
       printer.emit(variables,
           "$name$_ = input.readBytes();\n" +
           (Helpers.supportFieldPresence(descriptor) ? Helpers.generateSetBit(builderBitIndex) + ";\n" : ""));
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
    // Strings/Bytes size calc
    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
        printer.emit(variables,
            "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "!get$capitalized_name$Bytes().isEmpty()") + ") {\n" +
            "  size += com.google.protobuf.GeneratedMessage.computeStringSize($number$, $name$_);\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
            "  size += com.google.protobuf.CodedOutputStream\n" +
            "    .computeBytesSize($number$, $name$_);\n" +
            "}\n");
    }
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING) {
        printer.emit(variables,
            "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "!get$capitalized_name$Bytes().isEmpty()") + ") {\n" +
            "  com.google.protobuf.GeneratedMessage.writeString(output, $number$, $name$_);\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "if (" + (Helpers.supportFieldPresence(descriptor) ? Helpers.generateGetBit(messageBitIndex) : "$name$_ != $default$") + ") {\n" +
            "  output.writeBytes($number$, $name$_);\n" +
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
         "if (!get$capitalized_name$()\n" +
         "    .equals(other.get$capitalized_name$())) return false;\n");
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
         "hash = (53 * hash) + get$capitalized_name$().hashCode();\n");
     if (Helpers.supportFieldPresence(descriptor)) {
        printer.outdent();
        printer.print("}\n");
     }
  }
}
