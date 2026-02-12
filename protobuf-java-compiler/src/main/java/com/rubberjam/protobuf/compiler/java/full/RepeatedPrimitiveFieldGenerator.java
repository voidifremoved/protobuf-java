package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating repeated primitive fields.
 * Ported from java/full/primitive_field.cc.
 */
public class RepeatedPrimitiveFieldGenerator extends ImmutableFieldGenerator {
  public RepeatedPrimitiveFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);
    variables.put("type", Helpers.getPrimitiveTypeName(Helpers.getJavaType(descriptor)));
    variables.put("boxed_type", Helpers.getBoxedPrimitiveTypeName(Helpers.getJavaType(descriptor)));
    variables.put("capitalized_type", Helpers.getCapitalizedType(descriptor, true));
    variables.put("tag", String.valueOf(Helpers.getWireFormatForField(descriptor)));
    variables.put("packed_tag", String.valueOf((descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED));
    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("fixed_size", String.valueOf(Helpers.getFixedSize(descriptor.getType())));
    variables.put("on_changed", "onChanged();");

    String listType = (String) variables.get("capitalized_type");
    if (listType.endsWith("32")) listType = listType.substring(0, listType.length() - 2);
    if (listType.endsWith("64")) listType = listType.substring(0, listType.length() - 2);
    if (listType.equals("Bool")) listType = "Boolean";
    variables.put("list_type", listType);

    variables.put("repeated_get", "get" + variables.get("capitalized_name") + "List()");
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
     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables, "java.util.List<$boxed_type$> get$capitalized_name$List();\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
     printer.emit(variables, "int get$capitalized_name$Count();\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
     printer.emit(variables, "$type$ get$capitalized_name$(int index);\n");
  }

  @Override
  public void generateMembers(Printer printer) {
     printer.emit(variables,
         "@SuppressWarnings(\"serial\")\n" +
         "private com.google.protobuf.Internal.$list_type$List $name$_ =\n" +
         "    empty$list_type$List();\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.List<$boxed_type$>\n" +
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
         "public $type$ get$capitalized_name$(int index) {\n" +
         "  return $name$_.get$list_type$(index);\n" +
         "}\n");

     if (descriptor.isPacked()) {
         printer.emit(variables, "private int $name$MemoizedSerializedSize = -1;\n");
     }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     printer.emit(variables,
         "private com.google.protobuf.Internal.$list_type$List $name$_ =\n" +
         "    empty$list_type$List();\n" +
         "private void ensure$capitalized_name$IsMutable() {\n" +
         "  if (!" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
         "    $name$_ = super.mutableCopy($name$_);\n" +
         "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
         "  }\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions(), true);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.List<$boxed_type$>\n" +
         "    get$capitalized_name$List() {\n" +
         "  return " + Helpers.generateGetBit(builderBitIndex) + " ?\n" +
         "      java.util.Collections.unmodifiableList($name$_) : $name$_;\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions(), true);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public int get$capitalized_name$Count() {\n" +
         "  return $name$_.size();\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions(), true);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $type$ get$capitalized_name$(int index) {\n" +
         "  return $name$_.get$list_type$(index);\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
     printer.emit(variables,
         "public Builder set$capitalized_name$(\n" +
         "    int index, $type$ value) {\n" +
         "\n" +
         "  ensure$capitalized_name$IsMutable();\n" +
         "  $name$_.set$list_type$(index, value);\n" +
         "  $on_changed$\n" +
         "  return this;\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
     printer.emit(variables,
         "public Builder add$capitalized_name$($type$ value) {\n" +
         "\n" +
         "  ensure$capitalized_name$IsMutable();\n" +
         "  $name$_.add$list_type$(value);\n" +
         "  $on_changed$\n" +
         "  return this;\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER, context.getOptions(), true);
     printer.emit(variables,
         "public Builder addAll$capitalized_name$(\n" +
         "    java.lang.Iterable<? extends $boxed_type$> values) {\n" +
         "  ensure$capitalized_name$IsMutable();\n" +
         "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
         "      values, $name$_);\n" +
         "  $on_changed$\n" +
         "  return this;\n" +
         "}\n");

     DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
     printer.emit(variables,
         "public Builder clear$capitalized_name$() {\n" +
         "  $name$_ = empty$list_type$List();\n" +
         "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "  $on_changed$\n" +
         "  return this;\n" +
         "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     printer.emit(variables, "$name$_ = empty$list_type$List();\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
    printer.emit(variables, "$name$_ = empty$list_type$List();\n");
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
         "  $on_changed$\n" +
         "}\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     printer.emit(variables,
         "if ($get_has_field_bit_from_local$) {\n" +
         "  $name$_.makeImmutable();\n" +
         "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "}\n" +
         "result.$name$_ = $name$_;\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     printer.emit(variables,
         "ensure$capitalized_name$IsMutable();\n" +
         "$name$_.add$list_type$(input.read$capitalized_type$());\n");
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer) {
     printer.emit(variables,
         "int length = input.readRawVarint32();\n" +
         "int limit = input.pushLimit(length);\n" +
         "ensure$capitalized_name$IsMutable();\n" +
         "while (input.getBytesUntilLimit() > 0) {\n" +
         "  $name$_.add$list_type$(input.read$capitalized_type$());\n" +
         "}\n" +
         "input.popLimit(limit);\n");
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
      int fixedSize = Helpers.getFixedSize(descriptor.getType());
      if (fixedSize != -1) {
          printer.emit(variables, "dataSize = $fixed_size$ * $repeated_get$.size();\n");
      } else {
          printer.emit(variables,
              "for (int i = 0; i < $name$_.size(); i++) {\n" +
              "  dataSize += com.google.protobuf.CodedOutputStream\n" +
              "    .compute$capitalized_type$SizeNoTag($name$_.get$list_type$(i));\n" +
              "}\n");
      }
      printer.emit(variables, "size += dataSize;\n");
      if (descriptor.isPacked()) {
          printer.emit(variables,
              "if (!$repeated_get$.isEmpty()) {\n" +
              "  size += $tag_size$;\n" +
              "  size += com.google.protobuf.CodedOutputStream\n" +
              "      .computeInt32SizeNoTag(dataSize);\n" +
              "}\n" +
              "$name$MemoizedSerializedSize = dataSize;\n");
      } else {
          printer.emit(variables, "size += $tag_size$ * $repeated_get$.size();\n");
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
            "  output.writeUInt32NoTag($packed_tag$);\n" +
            "  output.writeUInt32NoTag($name$MemoizedSerializedSize);\n" +
            "}\n" +
            "for (int i = 0; i < $name$_.size(); i++) {\n" +
            "  output.write$capitalized_type$NoTag($name$_.get$list_type$(i));\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "for (int i = 0; i < $name$_.size(); i++) {\n" +
            "  output.write$capitalized_type$($number$, $name$_.get$list_type$(i));\n" +
            "}\n");
    }
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
