package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating repeated string and bytes fields.
 * Ported from java/full/string_field.cc.
 */
public class RepeatedStringFieldGenerator extends ImmutableFieldGenerator {
  public RepeatedStringFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));

    variables.put("null_check", "if (value == null) { throw new NullPointerException(); }");
    variables.put("on_changed", "onChanged();");
  }

  private boolean isString() {
      return Helpers.getJavaType(descriptor) == Helpers.JavaType.STRING;
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
     if (isString()) {
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
         printer.emit(variables, "java.util.List<java.lang.String>\n" +
             "    get$capitalized_name$List();\n");

         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
         printer.emit(variables, "int get$capitalized_name$Count();\n");

         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables, "java.lang.String get$capitalized_name$(int index);\n");

         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables, "com.google.protobuf.ByteString\n" +
             "    get$capitalized_name$Bytes(int index);\n");
     } else {
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
         printer.emit(variables, "java.util.List<com.google.protobuf.ByteString>\n" +
             "    get$capitalized_name$List();\n");

         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
         printer.emit(variables, "int get$capitalized_name$Count();\n");

         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables, "com.google.protobuf.ByteString get$capitalized_name$(int index);\n");
     }
  }

  @Override
  public void generateMembers(Printer printer) {
     if (isString()) {
         printer.emit(variables,
             "@SuppressWarnings(\"serial\")\n" +
             "private com.google.protobuf.LazyStringArrayList $name$_ =\n" +
             "    com.google.protobuf.LazyStringArrayList.emptyList();\n");

         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
         printer.emit(variables,
             "public com.google.protobuf.ProtocolStringList\n" +
             "    get$capitalized_name$List() {\n" +
             "  return $name$_;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
         printer.emit(variables,
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables,
             "public java.lang.String get$capitalized_name$(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables,
             "public com.google.protobuf.ByteString\n" +
             "    get$capitalized_name$Bytes(int index) {\n" +
             "  return $name$_.getByteString(index);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "@SuppressWarnings(\"serial\")\n" +
             "private java.util.List<com.google.protobuf.ByteString> $name$_ =\n" +
             "    java.util.Collections.emptyList();\n");

         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
         printer.emit(variables,
             "public java.util.List<com.google.protobuf.ByteString> get$capitalized_name$List() {\n" +
             "  return $name$_;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
         printer.emit(variables,
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
         printer.emit(variables,
             "public com.google.protobuf.ByteString get$capitalized_name$(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n");
     }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     if (isString()) {
         printer.emit(variables,
             "private com.google.protobuf.LazyStringArrayList $name$_ =\n" +
             "    com.google.protobuf.LazyStringArrayList.emptyList();\n" +
             "private void ensure$capitalized_name$IsMutable() {\n" +
             "  if (!$name$_.isModifiable()) {\n" +
             "    $name$_ = new com.google.protobuf.LazyStringArrayList($name$_);\n" +
             "  }\n" +
             "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
             "}\n");

         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions(), true);
         printer.emit(variables,
             "public com.google.protobuf.ProtocolStringList\n" +
             "    get$capitalized_name$List() {\n" +
             "  $name$_.makeImmutable();\n" +
             "  return $name$_;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions(), true);
         printer.emit(variables,
             "public java.lang.String get$capitalized_name$(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions(), true);
         printer.emit(variables,
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions(), true);
         printer.emit(variables,
             "public com.google.protobuf.ByteString\n" +
             "    get$capitalized_name$Bytes(int index) {\n" +
             "  return $name$_.getByteString(index);\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder set$capitalized_name$(\n" +
             "    int index, java.lang.String value) {\n" +
             "  $null_check$\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.set(index, value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder add$capitalized_name$(\n" +
             "    java.lang.String value) {\n" +
             "  $null_check$\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder addAll$capitalized_name$(\n" +
             "    java.lang.Iterable<java.lang.String> values) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
             "      values, $name$_);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder clear$capitalized_name$() {\n" +
             "  $name$_ =\n" +
             "    com.google.protobuf.LazyStringArrayList.emptyList();\n" +
             "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder add$capitalized_name$Bytes(\n" +
             "    com.google.protobuf.ByteString value) {\n" +
             "  $null_check$\n" +
             (InternalHelpers.checkUtf8(descriptor) ? "  checkByteStringIsUtf8(value);\n" : "") +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "private java.util.List<com.google.protobuf.ByteString> $name$_ =\n" +
             "    java.util.Collections.emptyList();\n" +
             "private void ensure$capitalized_name$IsMutable() {\n" +
             "  if (!" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
             "    $name$_ = new java.util.ArrayList<com.google.protobuf.ByteString>($name$_);\n" +
             "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
             "  }\n" +
             "}\n");

         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions(), true);
         printer.emit(variables,
             "public java.util.List<com.google.protobuf.ByteString>\n" +
             "    get$capitalized_name$List() {\n" +
             "  return java.util.Collections.unmodifiableList($name$_);\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions(), true);
         printer.emit(variables,
             "public com.google.protobuf.ByteString get$capitalized_name$(int index) {\n" +
             "  return $name$_.get(index);\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions(), true);
         printer.emit(variables,
             "public int get$capitalized_name$Count() {\n" +
             "  return $name$_.size();\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder set$capitalized_name$(\n" +
             "    int index, com.google.protobuf.ByteString value) {\n" +
             "  $null_check$\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.set(index, value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder add$capitalized_name$(\n" +
             "    com.google.protobuf.ByteString value) {\n" +
             "  $null_check$\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(value);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldStringBytesAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER, context.getOptions(), true);
         printer.emit(variables,
             "public Builder addAll$capitalized_name$(\n" +
             "    java.lang.Iterable<? extends com.google.protobuf.ByteString> values) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
             "      values, $name$_);\n" +
             "  onChanged();\n" +
             "  return this;\n" +
             "}\n");
         DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
         printer.emit(variables,
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
     if (isString()) {
         printer.emit(variables,
             "$name$_ =\n" +
             "    com.google.protobuf.LazyStringArrayList.emptyList();\n");
     } else {
         printer.emit(variables,
             "$name$_ = java.util.Collections.emptyList();\n");
     }
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     if (isString()) {
         printer.emit(variables,
             "$name$_ =\n" +
             "    com.google.protobuf.LazyStringArrayList.emptyList();\n");
     } else {
         printer.emit(variables,
             "$name$_ = java.util.Collections.emptyList();\n");
     }
  }

  @Override
  public void generateMergingCode(Printer printer) {
     printer.emit(variables,
         "if (!other.$name$_.isEmpty()) {\n" +
         "  if ($name$_.isEmpty()) {\n" +
         "    $name$_ = other.$name$_;\n" +
         "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
         "  } else {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.addAll(other.$name$_);\n" +
         "  }\n" +
         "  onChanged();\n" +
         "}\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     if (isString()) {
         printer.emit(variables,
             "if ($get_has_field_bit_from_local$) {\n" +
             "  $name$_.makeImmutable();\n" +
             "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
             "  result.$name$_ = $name$_;\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "if ($get_has_field_bit_from_local$) {\n" +
             "  $name$_ = java.util.Collections.unmodifiableList($name$_);\n" +
             "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
             "  result.$name$_ = $name$_;\n" +
             "}\n");
     }
  }

  @Override
  public void generateParsingCode(Printer printer) {
    if (isString()) {
       if (InternalHelpers.checkUtf8(descriptor)) {
           printer.emit(variables,
               "java.lang.String s = input.readStringRequireUtf8();\n" +
               "ensure$capitalized_name$IsMutable();\n" +
               "$name$_.add(s);\n");
       } else {
           printer.emit(variables,
               "com.google.protobuf.ByteString bs = input.readBytes();\n" +
               "ensure$capitalized_name$IsMutable();\n" +
               "$name$_.add(bs);\n");
       }
    } else {
       printer.emit(variables,
           "com.google.protobuf.ByteString bs = input.readBytes();\n" +
           "ensure$capitalized_name$IsMutable();\n" +
           "$name$_.add(bs);\n");
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
     printer.emit(variables, "{\n");
     try (AutoCloseable scope = printer.withIndent()) {
         printer.emit(variables, "int dataSize = 0;\n");

         printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
         if (isString()) {
             printer.emit(variables,
                 "  dataSize += computeStringSizeNoTag($name$_.getRaw(i));\n");
         } else {
             printer.emit(variables,
                 "  dataSize += com.google.protobuf.CodedOutputStream\n" +
                 "    .computeBytesSizeNoTag($name$_.get(i));\n");
         }
         printer.emit(variables, "}\n");

         printer.emit(variables, "size += dataSize;\n");
         printer.emit(variables, "size += $tag_size$ * get$capitalized_name$List().size();\n");
     } catch (Exception e) {
         throw new RuntimeException(e);
     }
     printer.emit(variables, "}\n");
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
    if (isString()) {
        printer.emit(variables,
            "  com.google.protobuf.GeneratedMessage.writeString(output, $number$, $name$_.getRaw(i));\n");
    } else {
        printer.emit(variables,
            "  output.writeBytes($number$, $name$_.get(i));\n");
    }
    printer.emit(variables, "}\n");
  }

  @Override
  public void generateEqualsCode(Printer printer) {
     printer.emit(variables,
         "if (!get$capitalized_name$List()\n" +
         "    .equals(other.get$capitalized_name$List())) return false;\n");
  }

  @Override
  public void generateHashCodeCode(Printer printer) {
     printer.emit(variables,
         "if (get$capitalized_name$Count() > 0) {\n" +
         "  hash = (37 * hash) + $constant_name$;\n" +
         "  hash = (53 * hash) + get$capitalized_name$List().hashCode();\n" +
         "}\n");
  }
}
