package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating repeated enum fields.
 * Ported from java/full/enum_field.cc.
 */
public class RepeatedEnumFieldGenerator extends ImmutableFieldGenerator {
  public RepeatedEnumFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    variables.put("type", Helpers.getJavaType(descriptor) == Helpers.JavaType.ENUM
        ? context.getNameResolver().getClassName(descriptor.getEnumType(), true)
        : "");

    boolean supportUnknownEnumValue = InternalHelpers.supportUnknownEnumValue(descriptor);
    variables.put("support_unknown_enum_value", supportUnknownEnumValue);

    if (supportUnknownEnumValue) {
       variables.put("unknown", descriptor.getEnumType().getName() + ".UNRECOGNIZED");
    } else {
       variables.put("unknown", variables.get("default"));
    }

    int wireType = descriptor.isPacked() ?
        com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED :
        com.google.protobuf.WireFormat.WIRETYPE_VARINT;
    variables.put("tag", (descriptor.getNumber() << 3) | wireType);
    variables.put("tag_size", com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber()));
  }

  @Override
  public int getNumBitsForMessage() {
    return 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    // Presence bit for repeated field often indicates immutability/builder state.
    // Actually for repeated fields in builder, we usually have a bit to track if the list is mutable.
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables, "java.util.List<$type$> get$capitalized_name$List();\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
     printer.emit(variables, "int get$capitalized_name$Count();\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables, "$type$ get$capitalized_name$(int index);\n");

     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
         printer.emit(variables, "java.util.List<java.lang.Integer>\n" +
             "get$capitalized_name$ValueList();\n");

         DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables, "int get$capitalized_name$Value(int index);\n");
     }
  }

  @Override
  public void generateMembers(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
        printer.emit(variables,
            "private java.util.List<java.lang.Integer> $name$_;\n" +
            "private static final com.google.protobuf.Internal.ListAdapter.Converter<\n" +
            "    java.lang.Integer, $type$> $name$_converter_ =\n" +
            "        new com.google.protobuf.Internal.ListAdapter.Converter<\n" +
            "            java.lang.Integer, $type$>() {\n" +
            "          public $type$ convert(java.lang.Integer from) {\n" +
            "            $type$ result = $type$.forNumber(from);\n" +
            "            return result == null ? $type$.UNRECOGNIZED : result;\n" +
            "          }\n" +
            "        };\n");

        printer.emit(variables,
            "@java.lang.Override\n" +
            "public java.util.List<java.lang.Integer>\n" +
            "get$capitalized_name$ValueList() {\n" +
            "  return $name$_;\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public int get$capitalized_name$Value(int index) {\n" +
            "  return $name$_.get(index);\n" +
            "}\n");

        printer.emit(variables,
            "@java.lang.Override\n" +
            "public java.util.List<$type$> get$capitalized_name$List() {\n" +
            "  return new com.google.protobuf.Internal.ListAdapter<\n" +
            "      java.lang.Integer, $type$>($name$_, $name$_converter_);\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public int get$capitalized_name$Count() {\n" +
            "  return $name$_.size();\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public $type$ get$capitalized_name$(int index) {\n" +
            "  return $name$_converter_.convert($name$_.get(index));\n" +
            "}\n");
     } else {
        printer.emit(variables,
            "private java.util.List<$type$> $name$_;\n");

        printer.emit(variables,
            "@java.lang.Override\n" +
            "public java.util.List<$type$> get$capitalized_name$List() {\n" +
            "  return $name$_;\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public java.util.List<? extends $type$> get$capitalized_name$OrBuilderList() {\n" +
            "  return $name$_;\n" +
            "}\n" + // OrBuilderList usually for messages? For enums it's just List.

            "@java.lang.Override\n" +
            "public int get$capitalized_name$Count() {\n" +
            "  return $name$_.size();\n" +
            "}\n" +
            "@java.lang.Override\n" +
            "public $type$ get$capitalized_name$(int index) {\n" +
            "  return $name$_.get(index);\n" +
            "}\n");
     }

     if (descriptor.isPacked()) {
       printer.emit(variables, "private int $name$MemoizedSerializedSize;\n");
     }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
         printer.emit(variables,
             "private java.util.List<java.lang.Integer> $name$_ = java.util.Collections.emptyList();\n" +
             "private void ensure$capitalized_name$IsMutable() {\n" +
             "  if (!" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
             "    $name$_ = new java.util.ArrayList<java.lang.Integer>($name$_);\n" +
             "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
             "  }\n" +
             "}\n");

         printer.emit(variables,
             "public java.util.List<java.lang.Integer>\n" +
             "    get$capitalized_name$ValueList() {\n" +
             "  return java.util.Collections.unmodifiableList($name$_);\n" +
             "}\n" +
             "public int get$capitalized_name$Value(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n" +
             "public Builder set$capitalized_name$Value(\n" +
             "    int index, int value) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.set(index, value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder add$capitalized_name$Value(int value) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder addAll$capitalized_name$Value(\n" +
             "    java.lang.Iterable<java.lang.Integer> values) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
             "      values, $name$_);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");

         // Enum methods
         printer.emit(variables,
             "public java.util.List<$type$> get$capitalized_name$List() {\n" +
             "  return new com.google.protobuf.Internal.ListAdapter<\n" +
             "      java.lang.Integer, $type$>($name$_, $name$_converter_);\n" +
             "}\n" +
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n" +
             "public $type$ get$capitalized_name$(int index) {\n" +
             "  return $name$_converter_.convert($name$_.get(index));\n" +
             "}\n" +
             "public Builder set$capitalized_name$(\n" +
             "    int index, $type$ value) {\n" +
             "  if (value == null) {\n" +
             "    throw new NullPointerException();\n" +
             "  }\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.set(index, value.getNumber());\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder add$capitalized_name$($type$ value) {\n" +
             "  if (value == null) {\n" +
             "    throw new NullPointerException();\n" +
             "  }\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value.getNumber());\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder addAll$capitalized_name$(\n" +
             "    java.lang.Iterable<? extends $type$> values) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  for ($type$ value : values) {\n" +
             "    $name$_.add(value.getNumber());\n" +
             "  }\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder clear$capitalized_name$() {\n" +
             "  $name$_ = java.util.Collections.emptyList();\n" +
             "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");

     } else {
         printer.emit(variables,
             "private java.util.List<$type$> $name$_ =\n" +
             "  java.util.Collections.emptyList();\n" +
             "private void ensure$capitalized_name$IsMutable() {\n" +
             "  if (!" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
             "    $name$_ = new java.util.ArrayList<$type$>($name$_);\n" +
             "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
             "  }\n" +
             "}\n");

         printer.emit(variables,
             "public java.util.List<$type$> get$capitalized_name$List() {\n" +
             "  return java.util.Collections.unmodifiableList($name$_);\n" +
             "}\n" +
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n" +
             "public $type$ get$capitalized_name$(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n" +
             "public Builder set$capitalized_name$(\n" +
             "    int index, $type$ value) {\n" +
             "  if (value == null) {\n" +
             "    throw new NullPointerException();\n" +
             "  }\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.set(index, value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder add$capitalized_name$($type$ value) {\n" +
             "  if (value == null) {\n" +
             "    throw new NullPointerException();\n" +
             "  }\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder addAll$capitalized_name$(\n" +
             "    java.lang.Iterable<? extends $type$> values) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
             "      values, $name$_);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n" +
             "public Builder clear$capitalized_name$() {\n" +
             "  $name$_ = java.util.Collections.emptyList();\n" +
             "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
     }
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     printer.emit(variables, "$name$_ = java.util.Collections.emptyList();\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     printer.emit(variables,
         "$name$_ = java.util.Collections.emptyList();\n" +
         Helpers.generateClearBit(builderBitIndex) + ";\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
     printer.emit(variables,
         "if (!other.$name$_.isEmpty()) {\n" +
         "  if ($name$_.isEmpty()) {\n" +
         "    $name$_ = other.$name$_;\n" +
         "    " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "  } else {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.addAll(other.$name$_);\n" +
         "  }\n" +
         "  onChanged();\n" +
         "}\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     printer.emit(variables,
         "if (" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
         "  $name$_ = java.util.Collections.unmodifiableList($name$_);\n" +
         "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "}\n" +
         "result.$name$_ = $name$_;\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     // Handling packed vs unpacked is important
     // C++ checks if packed
     boolean isPacked = descriptor.isPacked();
     if (isPacked) {
         printer.emit(variables,
             "int length = input.readRawVarint32();\n" +
             "int oldLimit = input.pushLimit(length);\n" +
             "while(input.getBytesUntilLimit() > 0) {\n");

         if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
             printer.emit(variables,
                 "  int rawValue = input.readEnum();\n" +
                 "  ensure$capitalized_name$IsMutable();\n" +
                 "  $name$_.add(rawValue);\n");
         } else {
             printer.emit(variables,
                 "  int rawValue = input.readEnum();\n" +
                 "  $type$ value = $type$.forNumber(rawValue);\n" +
                 "  if (value == null) {\n" +
                 "    unknownFields.mergeVarintField($number$, rawValue);\n" +
                 "  } else {\n" +
                 "    ensure$capitalized_name$IsMutable();\n" +
                 "    $name$_.add(value);\n" +
                 "  }\n");
         }
         printer.emit(variables,
             "}\n" +
             "input.popLimit(oldLimit);\n");
     } else {
         if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
             printer.emit(variables,
                 "int rawValue = input.readEnum();\n" +
                 "ensure$capitalized_name$IsMutable();\n" +
                 "$name$_.add(rawValue);\n");
         } else {
             printer.emit(variables,
                 "int rawValue = input.readEnum();\n" +
                 "$type$ value = $type$.forNumber(rawValue);\n" +
                 "if (value == null) {\n" +
                 "  unknownFields.mergeVarintField($number$, rawValue);\n" +
                 "} else {\n" +
                 "  ensure$capitalized_name$IsMutable();\n" +
                 "  $name$_.add(value);\n" +
                 "}\n");
         }
     }
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer) {
      printer.emit(variables,
          "int length = input.readRawVarint32();\n" +
          "int oldLimit = input.pushLimit(length);\n" +
          "while(input.getBytesUntilLimit() > 0) {\n");

      if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
          printer.emit(variables,
              "  int rawValue = input.readEnum();\n" +
              "  ensure$capitalized_name$IsMutable();\n" +
              "  $name$_.add(rawValue);\n");
      } else {
          printer.emit(variables,
              "  int rawValue = input.readEnum();\n" +
              "  $type$ value = $type$.forNumber(rawValue);\n" +
              "  if (value == null) {\n" +
              "    unknownFields.mergeVarintField($number$, rawValue);\n" +
              "  } else {\n" +
              "    ensure$capitalized_name$IsMutable();\n" +
              "    $name$_.add(value);\n" +
              "  }\n");
      }
      printer.emit(variables,
          "}\n" +
          "input.popLimit(oldLimit);\n");
  }

  @Override
  public void generateParsingDoneCode(Printer printer) {
     // No op
  }

  @Override
  public void generateSerializedSizeCode(Printer printer) {
     // Serialization size
     printer.emit(variables, "{\n");
     try (AutoCloseable scope = printer.withIndent()) {
         printer.emit(variables, "int dataSize = 0;\n");
         printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
         if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
             printer.emit(variables,
                 "  dataSize += com.google.protobuf.CodedOutputStream\n" +
                 "    .computeEnumSizeNoTag($name$_.get(i));\n");
         } else {
             printer.emit(variables,
                 "  dataSize += com.google.protobuf.CodedOutputStream\n" +
                 "    .computeEnumSizeNoTag($name$_.get(i).getNumber());\n");
         }
         printer.emit(variables, "}\n");

         printer.emit(variables, "size += dataSize;\n");

         if (descriptor.isPacked()) {
             printer.emit(variables,
                 "if (!get$capitalized_name$List().isEmpty()) {" +
                 "  size += $tag_size$;\n" +
                 "  size += com.google.protobuf.CodedOutputStream\n" +
                 "    .computeUInt32SizeNoTag(dataSize);\n" +
                 "}");
             printer.emit(variables, "$name$MemoizedSerializedSize = dataSize;\n");
         } else {
             printer.emit(variables,
                 "size += $tag_size$ * $name$_.size();\n");
         }
     } catch (Exception e) {
         throw new RuntimeException(e);
     }
     printer.emit(variables, "}\n");
  }

  @Override
  public void generateSerializationCode(Printer printer) {
     if (descriptor.isPacked()) {
        printer.emit(variables,
            "if (get$capitalized_name$List().size() > 0) {\n" +
            "  output.writeUInt32NoTag($tag$);\n" +
            "  output.writeUInt32NoTag($name$MemoizedSerializedSize);\n" +
            "}\n");
        printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
        if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
            printer.emit(variables,
                "  output.writeEnumNoTag($name$_.get(i));\n");
        } else {
             printer.emit(variables,
                "  output.writeEnumNoTag($name$_.get(i).getNumber());\n");
        }
        printer.emit(variables, "}\n");
     } else {
        printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
        if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
            printer.emit(variables,
                "  output.writeEnum($number$, $name$_.get(i));\n");
        } else {
             printer.emit(variables,
                "  output.writeEnum($number$, $name$_.get(i).getNumber());\n");
        }
        printer.emit(variables, "}\n");
     }
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     printer.emit(variables,
         "if (!$name$_.equals(other.$name$_)) return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     printer.emit(variables,
         "if (get$capitalized_name$Count() > 0) {\n" +
         "  hash = (37 * hash) + $constant_name$;\n" +
         "  hash = (53 * hash) + $name$_.hashCode();\n" +
         "}\n");
  }
}
