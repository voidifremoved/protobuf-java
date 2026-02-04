package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;

/**
 * For generating repeated primitive fields.
 * Ported from java/full/primitive_field.cc.
 */
public class RepeatedPrimitiveFieldGenerator extends ImmutableFieldGenerator {
  public RepeatedPrimitiveFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    super(descriptor, messageBitIndex, builderBitIndex, context);

    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    String primitiveType = getPrimitiveTypeName(javaType);
    String capitalizedType = getCapitalizedType(descriptor);
    String listType = getListType(javaType);

    variables.put("type", primitiveType);
    variables.put("boxed_type", Helpers.getBoxedPrimitiveTypeName(javaType));
    variables.put("capitalized_type", capitalizedType);
    variables.put("list_type", listType);
    variables.put("empty_list", getEmptyListCall(javaType));

    variables.put("repeated_get", "get" + variables.get("capitalized_name") + "List");
    variables.put("repeated_count", "get" + variables.get("capitalized_name") + "Count");
    variables.put("repeated_get_index", "get" + variables.get("capitalized_name"));
    variables.put("repeated_add", "add" + variables.get("capitalized_name"));
    variables.put("repeated_set", "set" + variables.get("capitalized_name"));

    variables.put("tag_size", String.valueOf(
        com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));

    if (descriptor.isPacked()) {
        variables.put("packed_tag", String.valueOf(
            (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED));
    }
  }

  private String getPrimitiveTypeName(Helpers.JavaType type) {
    switch (type) {
      case INT: return "int";
      case LONG: return "long";
      case FLOAT: return "float";
      case DOUBLE: return "double";
      case BOOLEAN: return "boolean";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  private String getListType(Helpers.JavaType type) {
    switch (type) {
      case INT: return "com.google.protobuf.Internal.IntList";
      case LONG: return "com.google.protobuf.Internal.LongList";
      case FLOAT: return "com.google.protobuf.Internal.FloatList";
      case DOUBLE: return "com.google.protobuf.Internal.DoubleList";
      case BOOLEAN: return "com.google.protobuf.Internal.BooleanList";
      default: throw new IllegalArgumentException("Not a primitive type");
    }
  }

  private String getEmptyListCall(Helpers.JavaType type) {
    switch (type) {
      case INT: return "emptyIntList()";
      case LONG: return "emptyLongList()";
      case FLOAT: return "emptyFloatList()";
      case DOUBLE: return "emptyDoubleList()";
      case BOOLEAN: return "emptyBooleanList()";
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
    return 0;
  }

  @Override
  public int getNumBitsForBuilder() {
    return 1;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables, "java.util.List<$boxed_type$> $repeated_get$();\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables, "int $repeated_count$();\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables, "$type$ $repeated_get_index$(int index);\n");
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables,
        "@SuppressWarnings(\"serial\")\n" +
        "private $list_type$ $name$_ =\n" +
        "    $empty_list$;\n");

    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    String boxed = Helpers.getBoxedPrimitiveTypeName(javaType);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public java.util.List<" + boxed + ">\n" +
        "    $repeated_get$() {\n" +
        "  return $name$_;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions());
    printer.emit(variables,
        "public int $repeated_count$() {\n" +
        "  return $name$_.size();\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
    printer.emit(variables,
        "public $type$ $repeated_get_index$(int index) {\n" +
        "  return $name$_.get" + (javaType == Helpers.JavaType.INT ? "Int" : (javaType == Helpers.JavaType.LONG ? "Long" : (javaType == Helpers.JavaType.FLOAT ? "Float" : (javaType == Helpers.JavaType.DOUBLE ? "Double" : "Boolean")))) + "(index);\n" +
        "}\n");

    // Memoized size for packed fields
    if (descriptor.isPacked()) {
        printer.emit(variables,
            "private int $name$MemoizedSerializedSize = -1;\n");
    }
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    String boxed = Helpers.getBoxedPrimitiveTypeName(javaType);

    printer.emit(variables,
        "private $list_type$ $name$_ = $empty_list$;\n" +
        "private void ensure$capitalized_name$IsMutable() {\n" +
        "  if (!$name$_.isModifiable()) {\n" +
        "    $name$_ = makeMutableCopy($name$_);\n" +
        "  }\n" +
        "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "}\n" +
        "private void ensure$capitalized_name$IsMutable(int capacity) {\n" +
        "  if (!$name$_.isModifiable()) {\n" +
        "    $name$_ = makeMutableCopy($name$_, capacity);\n" +
        "  }\n" +
        "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER, context.getOptions(), true);
    printer.emit(variables,
        "public java.util.List<" + boxed + "> $repeated_get$() {\n" +
        "  return $name$_;\n" + // wrapper might be needed if unmodifiable view desired
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT, context.getOptions(), true);
    printer.emit(variables,
        "public int $repeated_count$() {\n" +
        "  return $name$_.size();\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions(), true);
    printer.emit(variables,
        "public $type$ $repeated_get_index$(int index) {\n" +
        "  return $name$_.get" + (javaType == Helpers.JavaType.INT ? "Int" : (javaType == Helpers.JavaType.LONG ? "Long" : (javaType == Helpers.JavaType.FLOAT ? "Float" : (javaType == Helpers.JavaType.DOUBLE ? "Double" : "Boolean")))) + "(index);\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
    printer.emit(variables,
        "public Builder $repeated_set$(int index, $type$ value) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.set" + (javaType == Helpers.JavaType.INT ? "Int" : (javaType == Helpers.JavaType.LONG ? "Long" : (javaType == Helpers.JavaType.FLOAT ? "Float" : (javaType == Helpers.JavaType.DOUBLE ? "Double" : "Boolean")))) + "(index, value);\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
    printer.emit(variables,
        "public Builder $repeated_add$($type$ value) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  $name$_.add" + (javaType == Helpers.JavaType.INT ? "Int" : (javaType == Helpers.JavaType.LONG ? "Long" : (javaType == Helpers.JavaType.FLOAT ? "Float" : (javaType == Helpers.JavaType.DOUBLE ? "Double" : "Boolean")))) + "(value);\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER, context.getOptions(), true);
    printer.emit(variables,
        "public Builder addAll$capitalized_name$(\n" +
        "    java.lang.Iterable<? extends " + boxed + "> values) {\n" +
        "  ensure$capitalized_name$IsMutable();\n" +
        "  com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
        "      values, $name$_);\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER, context.getOptions(), true);
    printer.emit(variables,
        "public Builder clear$capitalized_name$() {\n" +
        "  $name$_ = $empty_list$;\n" +
        "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
        "  onChanged();\n" +
        "  return this;\n" +
        "}\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    printer.emit(variables, "$name$_ = $empty_list$;\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
    printer.emit(variables,
        "$name$_ = $empty_list$;\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
    printer.emit(variables,
        "if (!other.$name$_.isEmpty()) {\n" +
        "  if ($name$_.isEmpty()) {\n" +
        "    $name$_ = other.$name$_;\n" +
        "    $name$_.makeImmutable();\n" +
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
    printer.emit(variables,
        "if (" + Helpers.generateGetBit("from_", builderBitIndex) + ") {\n" +
        "  $name$_.makeImmutable();\n" +
        "  result.$name$_ = $name$_;\n" +
        "}\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
    printer.emit(variables,
        "$type$ v = input.read$capitalized_type$();\n" +
        "ensure$capitalized_name$IsMutable();\n" +
        "$name$_.add" + getAddMethodSuffix() + "(v);\n");
  }

  @Override
  public void generateParsingCodeFromPacked(Printer printer) {
    int fixedSize = Helpers.getFixedSize(descriptor.getType());
    if (fixedSize != -1) {
        variables.put("fixed_size", fixedSize);
        printer.emit(variables,
            "int length = input.readRawVarint32();\n" +
            "int limit = input.pushLimit(length);\n" +
            "int alloc = length > 4096 ? 4096 : length;\n" +
            "ensure$capitalized_name$IsMutable(alloc / $fixed_size$);\n" +
            "while (input.getBytesUntilLimit() > 0) {\n" +
            "  $name$_.add" + getAddMethodSuffix() + "(input.read$capitalized_type$());\n" +
            "}\n" +
            "input.popLimit(limit);\n");
    } else {
        printer.emit(variables,
            "int length = input.readRawVarint32();\n" +
            "int limit = input.pushLimit(length);\n" +
            "ensure$capitalized_name$IsMutable();\n" +
            "while (input.getBytesUntilLimit() > 0) {\n" +
            "  $name$_.add" + getAddMethodSuffix() + "(input.read$capitalized_type$());\n" +
            "}\n" +
            "input.popLimit(limit);\n");
    }
  }

  @Override
  public void generateBuilderParsingCode(Printer printer) {
    int tag = (descriptor.getNumber() << 3) | Helpers.getWireTypeForFieldType(descriptor.getType());
    printer.print("case " + tag + ": {\n");
    printer.indent();
    generateParsingCode(printer);
    printer.print("break;\n");
    printer.outdent();
    printer.print("} // case " + tag + "\n");

    int packedTag = (descriptor.getNumber() << 3) | com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
    printer.print("case " + packedTag + ": {\n");
    printer.indent();
    generateParsingCodeFromPacked(printer);
    printer.print("break;\n");
    printer.outdent();
    printer.print("} // case " + packedTag + "\n");
  }

  private String getAddMethodSuffix() {
    Helpers.JavaType javaType = Helpers.getJavaType(descriptor);
    return javaType == Helpers.JavaType.INT ? "Int" : (javaType == Helpers.JavaType.LONG ? "Long" : (javaType == Helpers.JavaType.FLOAT ? "Float" : (javaType == Helpers.JavaType.DOUBLE ? "Double" : "Boolean")));
  }

  @Override
  public void generateParsingDoneCode(Printer printer) {
     printer.emit(variables,
         "if (" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
         "  $name$_.makeImmutable();\n" +
         "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "}\n");
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
            "  output.write$capitalized_type$NoTag($name$_.get" + getAddMethodSuffix() + "(i));\n" +
            "}\n");
    } else {
        printer.emit(variables,
            "for (int i = 0; i < $name$_.size(); i++) {\n" +
            "  output.write$capitalized_type$($number$, $name$_.get" + getAddMethodSuffix() + "(i));\n" +
            "}\n");
    }
  }

  @Override
  public void generateSerializedSizeCode(Printer printer) {
    printer.emit(variables,
        "{\n" +
        "  int dataSize = 0;\n");
    int fixedSize = Helpers.getFixedSize(descriptor.getType());
    if (fixedSize == -1) {
        printer.emit(variables,
            "  for (int i = 0; i < $name$_.size(); i++) {\n" +
            "    dataSize += com.google.protobuf.CodedOutputStream\n" +
            "      .compute$capitalized_type$SizeNoTag($name$_.get" + getAddMethodSuffix() + "(i));\n" +
            "  }\n");
    } else {
        variables.put("fixed_size", fixedSize);
        printer.emit(variables,
            "  dataSize = $fixed_size$ * get$capitalized_name$List().size();\n");
    }
    printer.emit(variables, "  size += dataSize;\n");

    if (descriptor.isPacked()) {
        printer.emit(variables,
            "  if (!get$capitalized_name$List().isEmpty()) {\n" +
            "    size += $tag_size$;\n" +
            "    size += com.google.protobuf.CodedOutputStream\n" +
            "        .computeInt32SizeNoTag(dataSize);\n" +
            "  }\n" +
            "  $name$MemoizedSerializedSize = dataSize;\n");
    } else {
        printer.emit(variables,
            "  size += $tag_size$ * get$capitalized_name$List().size();\n");
    }
    printer.emit(variables,
        "}\n");
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
