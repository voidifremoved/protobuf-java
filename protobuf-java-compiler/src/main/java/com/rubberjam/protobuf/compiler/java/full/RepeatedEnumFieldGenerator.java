
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
                        variables.put("unknown", variables.get("type") + ".UNRECOGNIZED");
                } else {
                        variables.put("unknown", variables.get("default"));
                }

                int wireType = descriptor.isPacked() ? com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED
                                : com.google.protobuf.WireFormat.WIRETYPE_VARINT;
                variables.put("tag", (descriptor.getNumber() << 3) | wireType);
                variables.put("tag_size", com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber()));

                variables.put("null_check", "if (value == null) { throw new NullPointerException(); }");
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
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER,
                                context.getOptions());
                printer.emit(variables, "java.util.List<$type$> get$capitalized_name$List();\n");

                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT,
                                context.getOptions());
                printer.emit(variables, "int get$capitalized_name$Count();\n");

                DocComment.writeFieldAccessorDocComment(printer, descriptor,
                                DocComment.AccessorType.LIST_INDEXED_GETTER,
                                context.getOptions());
                printer.emit(variables, "$type$ get$capitalized_name$(int index);\n");

                if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_GETTER,
                                        context.getOptions());
                        printer.emit(variables, "java.util.List<java.lang.Integer>\n" +
                                        "get$capitalized_name$ValueList();\n");

                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
                        printer.emit(variables, "int get$capitalized_name$Value(int index);\n");
                }
        }

        @Override
        public void generateMembers(Printer printer) {
                printer.emit(variables,
                                "@SuppressWarnings(\"serial\")\n" +
                                                "private com.google.protobuf.Internal.IntList $name$_ =\n" +
                                                "    emptyIntList();\n" +
                                                "private static final     com.google.protobuf.Internal.IntListAdapter.IntConverter<\n"
                                                +
                                                "    $type$> $name$_converter_ =\n" +
                                                "        new com.google.protobuf.Internal.IntListAdapter.IntConverter<\n"
                                                +
                                                "            $type$>() {\n" +
                                                "          public $type$ convert(int from) {\n" +
                                                "            $type$ result = $type$.forNumber(from);\n" +
                                                "            return result == null ? $unknown$ : result;\n" +
                                                "          }\n" +
                                                "        };\n");

                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER,
                                context.getOptions());
                printer.emit(variables,
                                "@java.lang.Override\n" +
                                                "public java.util.List<$type$> get$capitalized_name$List() {\n" +
                                                "  return new com.google.protobuf.Internal.IntListAdapter<\n" +
                                                "      $type$>($name$_, $name$_converter_);\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT,
                                context.getOptions());
                printer.emit(variables,
                                "@java.lang.Override\n" +
                                                "public int get$capitalized_name$Count() {\n" +
                                                "  return $name$_.size();\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor,
                                DocComment.AccessorType.LIST_INDEXED_GETTER,
                                context.getOptions());
                printer.emit(variables,
                                "@java.lang.Override\n" +
                                                "public $type$ get$capitalized_name$(int index) {\n" +
                                                "  return $name$_converter_.convert($name$_.getInt(index));\n" +
                                                "}\n");

                if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_GETTER,
                                        context.getOptions());
                        printer.emit(variables,
                                        "@java.lang.Override\n" +
                                                        "public java.util.List<java.lang.Integer>\n" +
                                                        "get$capitalized_name$ValueList() {\n" +
                                                        "  return $name$_;\n" +
                                                        "}\n");
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
                        printer.emit(variables,
                                        "@java.lang.Override\n" +
                                                        "public int get$capitalized_name$Value(int index) {\n" +
                                                        "  return $name$_.getInt(index);\n" +
                                                        "}\n");
                }

                if (descriptor.isPacked()) {
                        printer.emit(variables, "private int $name$MemoizedSerializedSize;\n");
                }
        }

        @Override
        public void generateBuilderMembers(Printer printer) {
                printer.emit(variables,
                                "private com.google.protobuf.Internal.IntList $name$_ = emptyIntList();\n" +
                                                "private void ensure$capitalized_name$IsMutable() {\n" +
                                                "  if (!$name$_.isModifiable()) {\n" +
                                                "    $name$_ = makeMutableCopy($name$_);\n" +
                                                "  }\n" +
                                                "  " + Helpers.generateSetBit(builderBitIndex) + ";\n" +
                                                "}\n");

                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_GETTER,
                                context.getOptions());
                printer.emit(variables,
                                "public java.util.List<$type$> " +
                                                "get$capitalized_name$List() {\n" +
                                                "  return new com.google.protobuf.Internal.IntListAdapter<\n" +
                                                "      $type$>($name$_, $name$_converter_);\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_COUNT,
                                context.getOptions());
                printer.emit(variables,
                                "public int get$capitalized_name$Count() {\n" +
                                                "  return $name$_.size();\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor,
                                DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
                printer.emit(variables,
                                "public $type$ get$capitalized_name$(int index) {\n" +
                                                "  return $name$_converter_.convert($name$_.getInt(index));\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor,
                                DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
                printer.emit(variables,
                                "public Builder set$capitalized_name$(\n" +
                                                "    int index, $type$ value) {\n" +
                                                "  $null_check$\n" +
                                                "  ensure$capitalized_name$IsMutable();\n" +
                                                "  $name$_.setInt(index, value.getNumber());\n" +
                                                "  $on_changed$\n" +
                                                "  return this;\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_ADDER,
                                context.getOptions(), true);
                printer.emit(variables,
                                "public Builder add$capitalized_name$($type$ value) {\n" +
                                                "  $null_check$\n" +
                                                "  ensure$capitalized_name$IsMutable();\n" +
                                                "  $name$_.addInt(value.getNumber());\n" +
                                                "  $on_changed$\n" +
                                                "  return this;\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.LIST_MULTI_ADDER,
                                context.getOptions(), true);
                printer.emit(variables,
                                "public Builder addAll$capitalized_name$(\n" +
                                                "    java.lang.Iterable<? extends $type$> values) {\n" +
                                                "  ensure$capitalized_name$IsMutable();\n" +
                                                "  for ($type$ value : values) {\n" +
                                                "    $name$_.addInt(value.getNumber());\n" +
                                                "  }\n" +
                                                "  $on_changed$\n" +
                                                "  return this;\n" +
                                                "}\n");
                DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
                                context.getOptions(), true);
                printer.emit(variables,
                                "public Builder clear$capitalized_name$() {\n" +
                                                "  $name$_ = emptyIntList();\n" +
                                                "  " + Helpers.generateClearBit(builderBitIndex) + ";\n" +
                                                "  $on_changed$\n" +
                                                "  return this;\n" +
                                                "}\n");

                if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_GETTER, context.getOptions());
                        printer.emit(variables,
                                        "public java.util.List<java.lang.Integer>\n" +
                                                        "get$capitalized_name$ValueList() {\n" +
                                                        "  $name$_.makeImmutable();\n" +
                                                        "  return $name$_;\n" +
                                                        "}\n");
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_INDEXED_GETTER, context.getOptions());
                        printer.emit(variables,
                                        "public int get$capitalized_name$Value(int index) {\n" +
                                                        "  return $name$_.getInt(index);\n" +
                                                        "}\n");
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_INDEXED_SETTER, context.getOptions(), true);
                        printer.emit(variables,
                                        "public Builder set$capitalized_name$Value(\n" +
                                                        "    int index, int value) {\n" +
                                                        "  ensure$capitalized_name$IsMutable();\n" +
                                                        "  $name$_.setInt(index, value);\n" +
                                                        "  $on_changed$\n" +
                                                        "  return this;\n" +
                                                        "}\n");
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_ADDER, context.getOptions(), true);
                        printer.emit(variables,
                                        "public Builder add$capitalized_name$Value(int value) {\n" +
                                                        "  ensure$capitalized_name$IsMutable();\n" +
                                                        "  $name$_.addInt(value);\n" +
                                                        "  $on_changed$\n" +
                                                        "  return this;\n" +
                                                        "}\n");
                        DocComment.writeFieldEnumValueAccessorDocComment(printer, descriptor,
                                        DocComment.AccessorType.LIST_MULTI_ADDER, context.getOptions(), true);
                        printer.emit(variables,
                                        "public Builder addAll$capitalized_name$Value(\n" +
                                                        "    java.lang.Iterable<java.lang.Integer> values) {\n" +
                                                        "  ensure$capitalized_name$IsMutable();\n" +
                                                        "  for (int value : values) {\n" +
                                                        "    $name$_.addInt(value);\n" +
                                                        "  }\n" +
                                                        "  $on_changed$\n" +
                                                        "  return this;\n" +
                                                        "}\n");
                }
        }

        @Override
        public void generateInitializationCode(Printer printer) {
                printer.emit(variables, "$name$_ = emptyIntList();\n");
        }

        @Override
        public void generateBuilderClearCode(Printer printer) {
                printer.emit(variables, "$name$_ = emptyIntList();\n");
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
                printer.emit(variables,
                                "if ($get_has_field_bit_from_local$) {\n" +
                                                "  $name$_.makeImmutable();\n" +
                                                "  $clear_has_field_bit_from_local$;\n" +
                                                "}\n" +
                                                "result.$name$_ = $name$_;\n");
        }

        @Override
        public void generateParsingCode(Printer printer) {
                if (InternalHelpers.supportUnknownEnumValue(descriptor)) {
                        printer.emit(variables,
                                        "int rawValue = input.readEnum();\n" +
                                                        "ensure$capitalized_name$IsMutable();\n" +
                                                        "$name$_.addInt(rawValue);\n");
                } else {
                        printer.emit(variables,
                                        "int rawValue = input.readEnum();\n" +
                                                        "$type$ value = $type$.forNumber(rawValue);\n" +
                                                        "if (value == null) {\n" +
                                                        "  unknownFields.mergeVarintField($number$, rawValue);\n" +
                                                        "} else {\n" +
                                                        "  ensure$capitalized_name$IsMutable();\n" +
                                                        "  $name$_.addInt(rawValue);\n" +
                                                        "}\n");
                }
        }

        @Override
        public void generateParsingCodeFromPacked(Printer printer) {
                printer.emit(variables,
                                "int length = input.readRawVarint32();\n" +
                                                "int oldLimit = input.pushLimit(length);\n" +
                                                "while (input.getBytesUntilLimit() > 0) {\n" +
                                                "  int rawValue = input.readEnum();\n" +
                                                "  if (InternalHelpers.supportUnknownEnumValue(descriptor)) {\n" +
                                                "    ensure$capitalized_name$IsMutable();\n" +
                                                "    $name$_.addInt(rawValue);\n" +
                                                "  } else {\n" +
                                                "    $type$ value = $type$.forNumber(rawValue);\n" +
                                                "    if (value == null) {\n" +
                                                "      unknownFields.mergeVarintField($number$, rawValue);\n" +
                                                "    } else {\n" +
                                                "      ensure$capitalized_name$IsMutable();\n" +
                                                "      $name$_.addInt(rawValue);\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "}\n" +
                                                "input.popLimit(oldLimit);\n");
        }

        @Override
        public void generateParsingDoneCode(Printer printer) {
                // No op
        }

        // We'll leave the serialization code mostly intact since IntList handles
        // the underlying integers the same way. But we need to use getInt if necessary.
        @Override
        public void generateSerializationCode(Printer printer) {
                if (descriptor.isPacked()) {
                        printer.emit(variables,
                                        "if (get$capitalized_name$List().size() > 0) {\n" +
                                                        "  output.writeUInt32NoTag($tag$);\n" +
                                                        "  output.writeUInt32NoTag($name$MemoizedSerializedSize);\n" +
                                                        "}\n" +
                                                        "for (int i = 0; i < $name$_.size(); i++) {\n" +
                                                        "  output.writeEnumNoTag($name$_.getInt(i));\n" +
                                                        "}\n");
                } else {
                        printer.emit(variables,
                                        "for (int i = 0; i < $name$_.size(); i++) {\n" +
                                                        "  output.writeEnum($number$, $name$_.getInt(i));\n" +
                                                        "}\n");
                }
        }

        @Override
        public void generateSerializedSizeCode(Printer printer) {
                printer.emit(variables,
                                "{\n" +
                                                "  int dataSize = 0;\n" +
                                                "  for (int i = 0; i < $name$_.size(); i++) {\n" +
                                                "    dataSize += com.google.protobuf.CodedOutputStream\n" +
                                                "      .computeEnumSizeNoTag($name$_.getInt(i));\n" +
                                                "  }\n" +
                                                "  size += dataSize;\n");
                if (descriptor.isPacked()) {
                        printer.emit(variables,
                                        "  if (!get$capitalized_name$List().isEmpty()) {" +
                                                        "  size += $tag_size$;\n" +
                                                        "    size += com.google.protobuf.CodedOutputStream\n" +
                                                        "      .computeUInt32SizeNoTag(dataSize);\n" +
                                                        "  }$name$MemoizedSerializedSize = dataSize;\n");
                } else {
                        printer.emit(variables,
                                        "  size += $tag_size$ * $name$_.size();\n");
                }
                printer.emit(variables, "}\n");
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
