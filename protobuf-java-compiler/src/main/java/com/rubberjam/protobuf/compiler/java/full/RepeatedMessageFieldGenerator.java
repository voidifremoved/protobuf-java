package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;

/**
 * For generating repeated message fields.
 * Ported from java/full/message_field.cc.
 */
public class RepeatedMessageFieldGenerator extends ImmutableFieldGenerator {
  public RepeatedMessageFieldGenerator(
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

    variables.put("is_group", descriptor.getType() == FieldDescriptor.Type.GROUP);
    variables.put("on_changed", "onChanged();");
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
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables, "java.util.List<$type$> \n" +
         "    get$capitalized_name$List();\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables, "$type$ get$capitalized_name$(int index);\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables, "int get$capitalized_name$Count();\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables, "java.util.List<? extends $type$OrBuilder> \n" +
         "    get$capitalized_name$OrBuilderList();\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables, "$type$OrBuilder get$capitalized_name$OrBuilder(\n" +
         "    int index);\n");
  }

  @Override
  public void generateMembers(Printer printer) {
     printer.emit(variables,
         "@SuppressWarnings(\"serial\")\n" +
         "private java.util.List<$type$> $name$_;\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.List<$type$> get$capitalized_name$List() {\n" +
         "  return $name$_;\n" +
         "}\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public java.util.List<? extends $type$OrBuilder> \n" +
         "    get$capitalized_name$OrBuilderList() {\n" +
         "  return $name$_;\n" +
         "}\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public int get$capitalized_name$Count() {\n" +
         "  return $name$_.size();\n" +
         "}\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $type$ get$capitalized_name$(int index) {\n" +
         "  return $name$_.get(index);\n" +
         "}\n");

     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "@java.lang.Override\n" +
         "public $type$OrBuilder get$capitalized_name$OrBuilder(\n" +
         "    int index) {\n" +
         "  return $name$_.get(index);\n" +
         "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
     variables.put("ver", Helpers.getGeneratedCodeVersionSuffix());
     // RepeatedFieldBuilder
     printer.emit(variables,
         "private java.util.List<$type$> $name$_ =\n" +
         "  java.util.Collections.emptyList();\n" +
         "private void ensure$capitalized_name$IsMutable() {\n" +
         "  if (!" + Helpers.generateGetBit(builderBitIndex) + ") {\n" +
         "    $name$_ = new java.util.ArrayList<$type$>($name$_);\n" +
         "    " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
         "   }\n" +
         "}\n" +
         "\n" +
         "private com.google.protobuf.RepeatedFieldBuilder$ver$<\n" +
         "    $type$, $type$.Builder, $type$OrBuilder> $name$Builder_;\n" +
         "\n");

     // getList
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public java.util.List<$type$> \n" +
         "    get$capitalized_name$List() {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    return java.util.Collections.unmodifiableList($name$_);\n" +
         "  } else {\n" +
         "    return $name$Builder_.getMessageList();\n" +
         "  }\n" +
         "}\n");

     // getCount
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public int get$capitalized_name$Count() {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    return $name$_.size();\n" +
         "  } else {\n" +
         "    return $name$Builder_.getCount();\n" +
         "  }\n" +
         "}\n");

     // get(index)
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public $type$ get$capitalized_name$(int index) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    return $name$_.get(index);\n" +
         "  } else {\n" +
         "    return $name$Builder_.getMessage(index);\n" +
         "  }\n" +
         "}\n");

     // set
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder set$capitalized_name$(\n" +
         "    int index, $type$ value) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    if (value == null) {\n" +
         "      throw new NullPointerException();\n" +
         "    }\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.set(index, value);\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.setMessage(index, value);\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // set builder
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder set$capitalized_name$(\n" +
         "    int index, $type$.Builder builderForValue) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.set(index, builderForValue.build());\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.setMessage(index, builderForValue.build());\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // add
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder add$capitalized_name$($type$ value) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    if (value == null) {\n" +
         "      throw new NullPointerException();\n" +
         "    }\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.add(value);\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.addMessage(value);\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // add(index)
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder add$capitalized_name$(\n" +
         "    int index, $type$ value) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    if (value == null) {\n" +
         "      throw new NullPointerException();\n" +
         "    }\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.add(index, value);\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.addMessage(index, value);\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // add builder
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder add$capitalized_name$(\n" +
         "    $type$.Builder builderForValue) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.add(builderForValue.build());\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.addMessage(builderForValue.build());\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // add builder (index)
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder add$capitalized_name$(\n" +
         "    int index, $type$.Builder builderForValue) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.add(index, builderForValue.build());\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.addMessage(index, builderForValue.build());\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // addAll
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder addAll$capitalized_name$(\n" +
         "    java.lang.Iterable<? extends $type$> values) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    com.google.protobuf.AbstractMessageLite.Builder.addAll(\n" +
         "        values, $name$_);\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.addAllMessages(values);\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // clear
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder clear$capitalized_name$() {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    $name$_ = java.util.Collections.emptyList();\n" +
         "    " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.clear();\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // remove
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public Builder remove$capitalized_name$(int index) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    ensure$capitalized_name$IsMutable();\n" +
         "    $name$_.remove(index);\n" +
         "    $on_changed$\n" +
         "  } else {\n" +
         "    $name$Builder_.remove(index);\n" +
         "  }\n" +
         "  return this;\n" +
         "}\n");

     // getBuilder
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public $type$.Builder get$capitalized_name$Builder(\n" +
         "    int index) {\n" +
         "  return internalGet$capitalized_name$FieldBuilder().getBuilder(index);\n" +
         "}\n");

     // getOrBuilder
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public $type$OrBuilder get$capitalized_name$OrBuilder(\n" +
         "    int index) {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    return $name$_.get(index);  } else {\n" +
         "    return $name$Builder_.getMessageOrBuilder(index);\n" +
         "  }\n" +
         "}\n");

     // getOrBuilderList
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public java.util.List<? extends $type$OrBuilder> \n" +
         "     get$capitalized_name$OrBuilderList() {\n" +
         "  if ($name$Builder_ != null) {\n" +
         "    return $name$Builder_.getMessageOrBuilderList();\n" +
         "  } else {\n" +
         "    return java.util.Collections.unmodifiableList($name$_);\n" +
         "  }\n" +
         "}\n");

     // addBuilder
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public $type$.Builder add$capitalized_name$Builder() {\n" +
         "  return internalGet$capitalized_name$FieldBuilder().addBuilder(\n" +
         "      $type$.getDefaultInstance());\n" +
         "}\n");

     // addBuilder (index)
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public $type$.Builder add$capitalized_name$Builder(\n" +
         "    int index) {\n" +
         "  return internalGet$capitalized_name$FieldBuilder().addBuilder(\n" +
         "      index, $type$.getDefaultInstance());\n" +
         "}\n");

     // getBuilderList
     DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);
     printer.emit(variables,
         "public java.util.List<$type$.Builder> \n" +
         "     get$capitalized_name$BuilderList() {\n" +
         "  return internalGet$capitalized_name$FieldBuilder().getBuilderList();\n" +
         "}\n");

     // getFieldBuilder (private)
     printer.emit(variables,
         "private com.google.protobuf.RepeatedFieldBuilder$ver$<\n" +
         "    $type$, $type$.Builder, $type$OrBuilder> \n" +
         "    internalGet$capitalized_name$FieldBuilder() {\n" +
         "  if ($name$Builder_ == null) {\n" +
         "    $name$Builder_ = new com.google.protobuf.RepeatedFieldBuilder$ver$<\n" +
         "        $type$, $type$.Builder, $type$OrBuilder>(\n" +
         "            $name$_,\n" +
         "            ((" + Helpers.getBitFieldName(builderBitIndex / 32) + " & " + Helpers.getBitMask(builderBitIndex) + ") != 0),\n" +
         "            getParentForChildren(),\n" +
         "            isClean());\n" +
         "    $name$_ = null;\n" +
         "  }\n" +
         "  return $name$Builder_;\n" +
         "}\n");
  }

  @Override
  public void generateFieldBuilderInitializationCode(Printer printer) {
    printer.emit(variables, "internalGet$capitalized_name$FieldBuilder();\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
     printer.emit(variables, "$name$_ = java.util.Collections.emptyList();\n");
  }

  @Override
  public void generateBuilderClearCode(Printer printer) {
     printer.emit(variables,
         "if ($name$Builder_ == null) {\n" +
         "  $name$_ = java.util.Collections.emptyList();\n" +
         "} else {\n" +
         "  $name$_ = null;\n" +
         "  $name$Builder_.clear();\n" +
         "}\n" +
         Helpers.generateClearBit(builderBitIndex) + ";\n");
  }

  @Override
  public void generateMergingCode(Printer printer) {
     printer.emit(variables,
         "if ($name$Builder_ == null) {\n" +
         "  if (!other.$name$_.isEmpty()) {\n" +
         "    if ($name$_.isEmpty()) {\n" +
         "      $name$_ = other.$name$_;\n" +
         "      " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "    } else {\n" +
         "      ensure$capitalized_name$IsMutable();\n" +
         "      $name$_.addAll(other.$name$_);\n" +
         "    }\n" +
         "    $on_changed$\n" +
         "  }\n" +
         "} else {\n" +
         "  if (!other.$name$_.isEmpty()) {\n" +
         "    if ($name$Builder_.isEmpty()) {\n" +
         "      $name$Builder_.dispose();\n" +
         "      $name$Builder_ = null;\n" +
         "      $name$_ = other.$name$_;\n" +
         "      " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "      $name$Builder_ = \n" +
         "        com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".alwaysUseFieldBuilders ?\n" +
         "           internalGet$capitalized_name$FieldBuilder() : null;\n" +
         "    } else {\n" +
         "      $name$Builder_.addAllMessages(other.$name$_);\n" +
         "    }\n" +
         "  }\n" +
         "}\n");
  }

  @Override
  public void generateBuildingCode(Printer printer) {
     printer.emit(variables,
         "if ($name$Builder_ == null) {\n" +
         "  if ($get_has_field_bit_from_local$) {\n" +
         "    $name$_ = java.util.Collections.unmodifiableList($name$_);\n" +
         "    " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
         "  }\n" +
         "  result.$name$_ = $name$_;\n" +
         "} else {\n" +
         "  result.$name$_ = $name$Builder_.build();\n" +
         "}\n");
  }

  @Override
  public void generateParsingCode(Printer printer) {
     if (descriptor.getType() == FieldDescriptor.Type.GROUP) {
         printer.emit(variables,
             "$type$ m =\n" +
             "    input.readGroup($number$,\n" +
             "        $type$.parser(),\n" +
             "        extensionRegistry);\n" +
             "if ($name$Builder_ == null) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(m);\n" +
             "} else {\n" +
             "  $name$Builder_.addMessage(m);\n" +
             "}\n");
     } else {
         printer.emit(variables,
             "$type$ m =\n" +
             "    input.readMessage(\n" +
             "        $type$.parser(),\n" +
             "        extensionRegistry);\n" +
             "if ($name$Builder_ == null) {\n" +
             "  ensure$capitalized_name$IsMutable();\n" +
             "  $name$_.add(m);\n" +
             "} else {\n" +
             "  $name$Builder_.addMessage(m);\n" +
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
    printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
    if (descriptor.getType() == FieldDescriptor.Type.GROUP) {
        printer.emit(variables,
            "  size += com.google.protobuf.CodedOutputStream\n" +
            "    .computeGroupSize($number$, $name$_.get(i));\n");
    } else {
        printer.emit(variables,
            "  size += com.google.protobuf.CodedOutputStream\n" +
            "    .computeMessageSize($number$, $name$_.get(i));\n");
    }
    printer.emit(variables, "}\n");
  }

  @Override
  public void generateSerializationCode(Printer printer) {
    printer.emit(variables, "for (int i = 0; i < $name$_.size(); i++) {\n");
    if (descriptor.getType() == FieldDescriptor.Type.GROUP) {
        printer.emit(variables,
            "  output.writeGroup($number$, $name$_.get(i));\n");
    } else {
        printer.emit(variables,
            "  output.writeMessage($number$, $name$_.get(i));\n");
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
