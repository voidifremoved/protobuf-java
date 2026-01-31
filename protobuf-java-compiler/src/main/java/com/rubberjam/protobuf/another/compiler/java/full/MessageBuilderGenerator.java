package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon.FieldGeneratorMap;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.Names;
import com.rubberjam.protobuf.io.Printer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the Builder class for a message.
 */
public class MessageBuilderGenerator {

  private final Descriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;

  public MessageBuilderGenerator(Descriptor descriptor, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.fieldGenerators = ImmutableFieldGeneratorFactory.createFieldGenerators(descriptor, context);
  }

  public void generate(Printer printer) {
      Map<String, Object> vars = new HashMap<>();
      vars.put("classname", nameResolver.getImmutableClassName(descriptor));
      vars.put("parent", nameResolver.getImmutableClassName(descriptor.getFile()));
      vars.put("identifier", Helpers.uniqueFileScopeIdentifier(descriptor));

      String deprecation = descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
      vars.put("deprecation", deprecation);

      DocComment.writeMessageDocComment(printer, descriptor, new com.rubberjam.protobuf.another.compiler.java.Options(), true);

      printer.print(vars,
          "$deprecation$public static final class Builder extends\n" +
          "    com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".Builder<Builder> implements\n" +
          "    // @@protoc_insertion_point(builder_implements:" + descriptor.getFullName() + ")\n" +
          "    " + nameResolver.getImmutableClassName(descriptor) + "OrBuilder {\n");

      printer.indent();

      int totalBits = 0;
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              totalBits += fieldGenerators.get(field).getNumBitsForBuilder();
          }
      }
      int totalInts = (totalBits + 31) / 32;
      for (int i = 0; i < totalInts; i++) {
          printer.print("private int " + Helpers.getBitFieldName(i) + ";\n");
      }

      generateDescriptorMethods(printer);
      generateCommonBuilderMethods(printer);

      if (Helpers.isAnyMessage(descriptor)) {
          generateAnyMethods(printer);
      }

      // Generate fields
      for (FieldDescriptor field : descriptor.getFields()) {
          fieldGenerators.get(field).generateBuilderMembers(printer);
      }

      // Generate oneofs
      for (OneofDescriptor oneof : descriptor.getRealOneofs()) {
           generateOneofBuilderMembers(printer, oneof);
      }

      generateBuilderExtensionMethods(printer);

      printer.outdent();
      printer.print("}\n");
  }

  private void generateDescriptorMethods(Printer printer) {
      printer.print(
          "public static final com.google.protobuf.Descriptors.Descriptor\n" +
          "    getDescriptor() {\n" +
          "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
          ".internal_static_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_descriptor;\n" +
          "}\n" +
          "\n");

      List<FieldDescriptor> mapFields = new ArrayList<>();
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE &&
              field.getMessageType().getOptions().getMapEntry()) {
              mapFields.add(field);
          }
      }

      if (!mapFields.isEmpty()) {
          printer.print(
              "@SuppressWarnings({\"rawtypes\"})\n" +
              "protected com.google.protobuf.MapFieldReflectionAccessor internalGetMapFieldReflection(\n" +
              "    int number) {\n" +
              "  switch (number) {\n");
          printer.indent();
          printer.indent();
          for (FieldDescriptor field : mapFields) {
              printer.print(
                  "case " + field.getNumber() + ":\n" +
                  "  return internalGet" + context.getFieldGeneratorInfo(field).capitalizedName + "();\n");
          }
          printer.print(
              "default:\n" +
              "  throw new RuntimeException(\n" +
              "      \"Invalid map field number: \" + number);\n");
          printer.outdent();
          printer.outdent();
          printer.print(
              "  }\n" +
              "}\n");
      }

      printer.print(
          "@java.lang.Override\n" +
          "protected com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".FieldAccessorTable\n" +
          "    internalGetFieldAccessorTable() {\n" +
          "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
          ".internal_static_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_fieldAccessorTable\n" +
          "      .ensureFieldAccessorsInitialized(\n" +
          "          " + nameResolver.getImmutableClassName(descriptor) + ".class, " +
          nameResolver.getImmutableClassName(descriptor) + ".Builder.class);\n" +
          "}\n" +
          "\n");
  }

  private void generateCommonBuilderMethods(Printer printer) {
      String className = nameResolver.getImmutableClassName(descriptor);
      Map<String, Object> vars = new HashMap<>();
      vars.put("classname", className);

      printer.print(vars,
          "// Construct using " + className + ".newBuilder()\n" +
          "private Builder() {\n" +
          "  maybeForceBuilderInitialization();\n" +
          "}\n" +
          "\n" +
          "private Builder(\n" +
          "    com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".BuilderParent parent) {\n" +
          "  super(parent);\n" +
          "  maybeForceBuilderInitialization();\n" +
          "}\n" +
          "private void maybeForceBuilderInitialization() {\n" +
          "  if (com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + "\n" +
          "          .alwaysUseFieldBuilders) {\n");

      printer.indent();
      printer.indent();
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE &&
              field.isRepeated() &&
              !field.isMapField()) {
              fieldGenerators.get(field).generateBuilderClearCode(printer); // Actually just getBuilder calls usually?
              // The C++ code calls GenerateBuilderClearCode which is confusingly named but for initialization it usually checks hasFieldBuilder.
              // Actually for initialization `maybeForceBuilderInitialization` usually calls `get*FieldBuilder()` for repeated message fields to force eager init if allowed.
              // Let's check `RepeatedMessageFieldGenerator::GenerateBuilderClearCode`.
              // It prints `get${name}FieldBuilder();`
              printer.print("    get" + context.getFieldGeneratorInfo(field).capitalizedName + "FieldBuilder();\n");
          }
      }
      printer.outdent();
      printer.outdent();
      printer.print(
          "  }\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder clear() {\n" +
          "  super.clear();\n");

      printer.indent();
      vars.put("bitfield0", "bitField0_"); // We might need to handle bitField0_ being a member variable of Builder.
      // The builder has bitField0_ int.

      // In C++, GenerateBuilderClearCode is called.
      // For primitives, it clears the bit in bitField0_.
      // For repeateds, it clears list and bit.

      // To match C++ output structure perfectly (grouping clears), we iterate fields.
      // But we need to update bitField0_ intelligently.
      // The C++ implementation creates a `BitField` helper class to track bits.

      // For now, I will iterate fields and let them generate clear code.
      // However, C++ optimized by doing `bitField0_ = 0;` at the start/end if possible?
      // No, C++ `MessageBuilderGenerator::GenerateClear` calls `fieldGenerators_.get(field).GenerateBuilderClearCode(printer)`.

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              fieldGenerators.get(field).generateBuilderClearCode(printer);
          }
      }

      for (OneofDescriptor oneof : descriptor.getRealOneofs()) {
          String name = context.getOneofGeneratorInfo(oneof).name;
          printer.print(
              name + "Case_ = 0;\n" +
              name + "_ = null;\n");
      }

      printer.print(
          "  return this;\n" +
          "}\n" +
          "\n");
      printer.outdent();

      printer.print(vars,
          "@java.lang.Override\n" +
          "public com.google.protobuf.Descriptors.Descriptor\n" +
          "    getDescriptorForType() {\n" +
          "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
          ".internal_static_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_descriptor;\n" +
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public $classname$ getDefaultInstanceForType() {\n" +
          "  return $classname$.getDefaultInstance();\n" +
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public $classname$ build() {\n" +
          "  $classname$ result = buildPartial();\n" +
          "  if (!result.isInitialized()) {\n" +
          "    throw newUninitializedMessageException(result);\n" +
          "  }\n" +
          "  return result;\n" +
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public $classname$ buildPartial() {\n" +
          "  $classname$ result = new $classname$(this);\n");

      printer.indent();

      // Bitfield handling for buildPartial
      int totalBits = 0;
      for (FieldDescriptor field : descriptor.getFields()) {
           if (field.getContainingOneof() == null) {
               totalBits += fieldGenerators.get(field).getNumBitsForMessage(); // Use logic from FieldGenerator
           }
      }
      int totalInts = (totalBits + 31) / 32;

      // Declare from_bitField vars
      for (int i = 0; i < totalInts; i++) {
          printer.print("int from_bitField" + i + "_ = bitField" + i + "_;\n");
      }

      // Declare to_bitField vars (accumulators)
      for (int i = 0; i < totalInts; i++) {
          printer.print("int to_bitField" + i + "_ = 0;\n");
      }

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              fieldGenerators.get(field).generateBuildingCode(printer);
          }
      }

      // Assign to_bitField to result.bitField
      for (int i = 0; i < totalInts; i++) {
          printer.print("result.bitField" + i + "_ = to_bitField" + i + "_;\n");
      }

      for (OneofDescriptor oneof : descriptor.getRealOneofs()) {
          String name = context.getOneofGeneratorInfo(oneof).name;
          printer.print(
              "result." + name + "Case_ = " + name + "Case_;\n" +
              "result." + name + "_ = " + name + "_;\n");
      }

      printer.print("onBuilt();\n");
      printer.print("return result;\n");
      printer.outdent();
      printer.print("}\n\n");

      generateClone(printer);
      generateMergeFrom(printer);
      generateIsInitialized(printer);
  }

  private void generateClone(Printer printer) {
      printer.print(
          "@java.lang.Override\n" +
          "public Builder clone() {\n" +
          "  return super.clone();\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder setField(\n" +
          "    com.google.protobuf.Descriptors.FieldDescriptor field,\n" +
          "    java.lang.Object value) {\n" +
          "  return super.setField(field, value);\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder clearField(\n" +
          "    com.google.protobuf.Descriptors.FieldDescriptor field) {\n" +
          "  return super.clearField(field);\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder clearOneof(\n" +
          "    com.google.protobuf.Descriptors.OneofDescriptor oneof) {\n" +
          "  return super.clearOneof(oneof);\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder setRepeatedField(\n" +
          "    com.google.protobuf.Descriptors.FieldDescriptor field,\n" +
          "    int index, java.lang.Object value) {\n" +
          "  return super.setRepeatedField(field, index, value);\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder addRepeatedField(\n" +
          "    com.google.protobuf.Descriptors.FieldDescriptor field,\n" +
          "    java.lang.Object value) {\n" +
          "  return super.addRepeatedField(field, value);\n" +
          "}\n");

      if (descriptor.getExtensions().size() > 0) {
          printer.print(
              "@java.lang.Override\n" +
              "public <Type> Builder setExtension(\n" +
              "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
              "        " + nameResolver.getImmutableClassName(descriptor) + ", Type> extension,\n" +
              "    Type value) {\n" +
              "  return super.setExtension(extension, value);\n" +
              "}\n" +
              "@java.lang.Override\n" +
              "public <Type> Builder setExtension(\n" +
              "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
              "        " + nameResolver.getImmutableClassName(descriptor) + ", Type> extension,\n" +
              "    int index, Type value) {\n" +
              "  return super.setExtension(extension, index, value);\n" +
              "}\n" +
              "@java.lang.Override\n" +
              "public <Type> Builder addExtension(\n" +
              "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
              "        " + nameResolver.getImmutableClassName(descriptor) + ", Type> extension,\n" +
              "    Type value) {\n" +
              "  return super.addExtension(extension, value);\n" +
              "}\n" +
              "@java.lang.Override\n" +
              "public <Type> Builder clearExtension(\n" +
              "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
              "        " + nameResolver.getImmutableClassName(descriptor) + ", Type> extension) {\n" +
              "  return super.clearExtension(extension);\n" +
              "}\n");
      }

      printer.print(
          "@java.lang.Override\n" +
          "public Builder mergeFrom(com.google.protobuf.Message other) {\n" +
          "  if (other instanceof " + nameResolver.getImmutableClassName(descriptor) + ") {\n" +
          "    return mergeFrom((" + nameResolver.getImmutableClassName(descriptor) + ")other);\n" +
          "  } else {\n" +
          "    super.mergeFrom(other);\n" +
          "    return this;\n" +
          "  }\n" +
          "}\n" +
          "\n");
  }

  private void generateMergeFrom(Printer printer) {
      printer.print(
          "public Builder mergeFrom(" + nameResolver.getImmutableClassName(descriptor) + " other) {\n");
      printer.indent();
      printer.print(
          "if (other == " + nameResolver.getImmutableClassName(descriptor) + ".getDefaultInstance()) return this;\n");

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              fieldGenerators.get(field).generateMergingCode(printer);
          }
      }

      for (OneofDescriptor oneof : descriptor.getRealOneofs()) {
          String name = context.getOneofGeneratorInfo(oneof).name;
          String capName = context.getOneofGeneratorInfo(oneof).capitalizedName;
          printer.print(
              "switch (other.get" + capName + "Case()) {\n");
          printer.indent();
          for (FieldDescriptor field : oneof.getFields()) {
              printer.print("case " + field.getName().toUpperCase() + ": {\n");
              printer.indent();
              fieldGenerators.get(field).generateMergingCode(printer);
              printer.print("break;\n");
              printer.outdent();
              printer.print("}\n");
          }
          printer.print("case " + name.toUpperCase() + "_NOT_SET: {\n" +
                        "  break;\n" +
                        "}\n");
          printer.outdent();
          printer.print("}\n");
      }

      printer.print(
          "this.mergeUnknownFields(other.getUnknownFields());\n");
      if (descriptor.getExtensions().size() > 0) {
           printer.print("this.mergeExtensionFields(other);\n");
      }
      printer.print("onChanged();\n" +
                    "return this;\n");
      printer.outdent();
      printer.print("}\n\n");

      printer.print(
          "@java.lang.Override\n" +
          "public final boolean isInitialized() {\n");
      printer.indent();

      // IsInitialized logic for Builder
      // Iterate required fields
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.isRequired()) {
              printer.print(
                  "if (!has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) {\n" +
                  "  return false;\n" +
                  "}\n");
          }
      }

      // Iterate embedded messages
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE &&
              Helpers.hasRequiredFields(field.getMessageType())) {
              String name = context.getFieldGeneratorInfo(field).capitalizedName;
              if (field.isRequired()) {
                  printer.print(
                      "if (!get" + name + "().isInitialized()) {\n" +
                      "  return false;\n" +
                      "}\n");
              } else if (field.isOptional()) {
                  printer.print(
                      "if (has" + name + "()) {\n" +
                      "  if (!get" + name + "().isInitialized()) {\n" +
                      "    return false;\n" +
                      "  }\n" +
                      "}\n");
              } else if (field.isRepeated()) {
                  // ... logic for repeated messages
                   if (field.isMapField()) {
                       // ...
                       printer.print(
                           "for (" + mapValueImmutableClassdName(field.getMessageType(), nameResolver) + " item : get" + name + "Map().values()) {\n" +
                           "  if (!item.isInitialized()) {\n" +
                           "    return false;\n" +
                           "  }\n" +
                           "}\n");
                   } else {
                       printer.print(
                           "for (int i = 0; i < get" + name + "Count(); i++) {\n" +
                           "  if (!get" + name + "(i).isInitialized()) {\n" +
                           "    return false;\n" +
                           "  }\n" +
                           "}\n");
                   }
              }
          }
      }

      if (descriptor.getExtensions().size() > 0) {
          printer.print(
              "if (!extensionsAreInitialized()) {\n" +
              "  return false;\n" +
              "}\n");
      }

      printer.print("return true;\n");
      printer.outdent();
      printer.print("}\n\n");

      printer.print(
          "@java.lang.Override\n" +
          "public Builder mergeFrom(\n" +
          "    com.google.protobuf.CodedInputStream input,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws java.io.IOException {\n" +
          "  if (extensionRegistry == null) {\n" +
          "    throw new java.lang.NullPointerException();\n" +
          "  }\n" +
          "  try {\n" +
          "    boolean done = false;\n" +
          "    while (!done) {\n" +
          "      int tag = input.readTag();\n" +
          "      switch (tag) {\n" +
          "        case 0:\n" +
          "          done = true;\n" +
          "          break;\n");

      // Generate parsing logic
      printer.indent();
      printer.indent();
      printer.indent();
      printer.indent();

      // We need to sort fields by number or handle them in order.
      // And handle unknown fields.
      // This is a complex part often delegated to FieldGenerators but usually the switch is here.

      // For now, standard implementation:
      for (FieldDescriptor field : descriptor.getFields()) {
          printer.print("case " + Helpers.getWireFormatForField(field) + ": {\n");
          printer.indent();
          fieldGenerators.get(field).generateParsingCode(printer);
          printer.print("break;\n");
          printer.outdent();
          printer.print("}\n"); // End case block
      }

      printer.print(
          "default: {\n" +
          "  if (!super.parseUnknownField(input, extensionRegistry, tag)) {\n" +
          "    done = true;\n" + // was "return this;" in old versions? No, "done = true" in newer.
          "  }\n" +
          "  break;\n" +
          "}\n");

      printer.outdent();
      printer.outdent();
      printer.outdent();
      printer.outdent();

      printer.print(
          "      }\n" +
          "    }\n" +
          "  } catch (com.google.protobuf.InvalidProtocolBufferException e) {\n" +
          "    throw e.unwrapIOException();\n" +
          "  } finally {\n" +
          "    onChanged();\n" +
          "  }\n" +
          "  return this;\n" +
          "}\n");
  }

  private void generateIsInitialized(Printer printer) {
      // Done in generateMergeFrom above
  }

  private void generateAnyMethods(Printer printer) {
      // Implement if Any.
  }

  private void generateOneofBuilderMembers(Printer printer, OneofDescriptor oneof) {
      String name = context.getOneofGeneratorInfo(oneof).name;
      String capitalizedName = context.getOneofGeneratorInfo(oneof).capitalizedName;

      printer.print(
          "public " + nameResolver.getImmutableClassName(descriptor) + "." + capitalizedName + "Case\n" +
          "    get" + capitalizedName + "Case() {\n" +
          "  return " + nameResolver.getImmutableClassName(descriptor) + "." + capitalizedName + "Case.forNumber(\n" +
          "      " + name + "Case_);\n" +
          "}\n" +
          "\n" +
          "public Builder clear" + capitalizedName + "() {\n" +
          "  " + name + "Case_ = 0;\n" +
          "  " + name + "_ = null;\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n" +
          "\n");
  }

  private void generateBuilderExtensionMethods(Printer printer) {
      // Extensions in builders are handled by extendable message builder parent usually.
      // But if we have specific typed extensions accessors (not common in Java generated code for builder unless generated extensions).
  }

  private String mapValueImmutableClassdName(Descriptor descriptor, ClassNameResolver nameResolver) {
      FieldDescriptor valueField = descriptor.findFieldByName("value");
      return nameResolver.getImmutableClassName(valueField.getMessageType());
  }
}
