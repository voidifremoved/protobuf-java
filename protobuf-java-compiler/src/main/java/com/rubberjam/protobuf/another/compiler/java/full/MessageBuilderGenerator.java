package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon.FieldGeneratorMap;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.Names;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates the Builder inner class for a message.
 * Ported from java/full/message_builder.cc.
 */
public class MessageBuilderGenerator {

  private final Descriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;
  private final Map<Integer, OneofDescriptor> oneofs = new TreeMap<>();

  public MessageBuilderGenerator(Descriptor descriptor, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.fieldGenerators = ImmutableFieldGeneratorFactory.createFieldGenerators(descriptor, context);

    for (OneofDescriptor oneof : descriptor.getOneofs()) {
        oneofs.put(oneof.getIndex(), oneof);
    }
  }

  public void generate(Printer printer) {
    writeMessageDocComment(printer, descriptor);
    String versionSuffix = Helpers.getGeneratedCodeVersionSuffix();

    String extraInterfaces = Helpers.getExtraBuilderInterfaces(descriptor);
    String className = nameResolver.getImmutableClassName(descriptor);

    if (descriptor.getExtensions().size() > 0) {
      printer.print(
          "public static final class Builder extends\n" +
          "    com.google.protobuf.GeneratedMessage" + versionSuffix + ".ExtendableBuilder<\n" +
          "      " + className + ", Builder> implements\n" +
          "    " + extraInterfaces + "\n" +
          "    " + className + "OrBuilder {\n");
    } else {
      printer.print(
          "public static final class Builder extends\n" +
          "    com.google.protobuf.GeneratedMessage" + versionSuffix + ".Builder<Builder> implements\n" +
          "    " + extraInterfaces + "\n" +
          "    " + className + "OrBuilder {\n");
    }
    printer.indent();

    generateDescriptorMethods(printer);
    generateCommonBuilderMethods(printer);

    if (context.hasGeneratedMethods(descriptor)) {
      generateIsInitialized(printer);
      generateBuilderParsingMethods(printer);
    }

    // oneof
    Map<String, Object> vars = new HashMap<>();
    for (OneofDescriptor oneof : oneofs.values()) {
      vars.put("oneof_name", context.getOneofGeneratorInfo(oneof).name);
      vars.put("oneof_capitalized_name", context.getOneofGeneratorInfo(oneof).capitalizedName);
      vars.put("oneof_index", oneof.getIndex());

      printer.emit(vars,
          "private int $oneof_name$Case_ = 0;\n" +
          "private java.lang.Object $oneof_name$_;\n");

      printer.emit(vars,
          "public $oneof_capitalized_name$Case\n" +
          "    get$oneof_capitalized_name$Case() {\n" +
          "  return $oneof_capitalized_name$Case.forNumber(\n" +
          "      $oneof_name$Case_);\n" +
          "}\n" +
          "\n" +
          "public Builder clear$oneof_capitalized_name$() {\n" +
          "  $oneof_name$Case_ = 0;\n" +
          "  $oneof_name$_ = null;\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n" +
          "\n");
    }

    // Integers for bit fields.
    int totalBits = 0;
    for (FieldDescriptor field : descriptor.getFields()) {
      totalBits += fieldGenerators.get(field).getNumBitsForBuilder();
    }
    int totalInts = (totalBits + 31) / 32;
    for (int i = 0; i < totalInts; i++) {
      printer.print("private int " + getBitFieldName(i) + ";\n");
    }

    for (FieldDescriptor field : descriptor.getFields()) {
      printer.print("\n");
      fieldGenerators.get(field).generateBuilderMembers(printer);
    }

    if (context.getOptions().isOpensourceRuntime()) {
      printer.print(
          "@java.lang.Override\n" +
          "public final Builder setUnknownFields(\n" +
          "    final com.google.protobuf.UnknownFieldSet unknownFields) {\n" +
          "  return super.setUnknownFields(unknownFields);\n" +
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public final Builder mergeUnknownFields(\n" +
          "    final com.google.protobuf.UnknownFieldSet unknownFields) {\n" +
          "  return super.mergeUnknownFields(unknownFields);\n" +
          "}\n" +
          "\n");
    }

    printer.print(
        "\n" +
        "// @@protoc_insertion_point(builder_scope:" + descriptor.getFullName() + ")\n");

    printer.outdent();
    printer.print("}\n");
  }

  private void generateDescriptorMethods(Printer printer) {
    String versionSuffix = Helpers.getGeneratedCodeVersionSuffix();
    if (!descriptor.getOptions().getNoStandardDescriptorAccessor()) {
      printer.print(
          "public static final com.google.protobuf.Descriptors.Descriptor\n" +
          "    getDescriptor() {\n" +
          "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
          ".internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_descriptor;\n" +
          "}\n" +
          "\n");
    }

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

      printer.print(
          "@SuppressWarnings({\"rawtypes\"})\n" +
          "protected com.google.protobuf.MapFieldReflectionAccessor internalGetMutableMapFieldReflection(\n" +
          "    int number) {\n" +
          "  switch (number) {\n");
      printer.indent();
      printer.indent();
      for (FieldDescriptor field : mapFields) {
        printer.print(
            "case " + field.getNumber() + ":\n" +
            "  return internalGetMutable" + context.getFieldGeneratorInfo(field).capitalizedName + "();\n");
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
        "protected com.google.protobuf.GeneratedMessage" + versionSuffix + ".FieldAccessorTable\n" +
        "    internalGetFieldAccessorTable() {\n" +
        "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
        ".internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_fieldAccessorTable\n" +
        "      .ensureFieldAccessorsInitialized(\n" +
        "          " + nameResolver.getImmutableClassName(descriptor) + ".class, " +
        nameResolver.getImmutableClassName(descriptor) + ".Builder.class);\n" +
        "}\n" +
        "\n");
  }

  private void generateCommonBuilderMethods(Printer printer) {
    String versionSuffix = Helpers.getGeneratedCodeVersionSuffix();
    boolean needMaybeForceBuilderInit = false;
    for (FieldDescriptor field : descriptor.getFields()) {
      if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE &&
          field.getContainingOneof() == null &&
          Helpers.hasHasbit(field)) {
        needMaybeForceBuilderInit = true;
        break;
      }
    }

    String forceBuilderInit = needMaybeForceBuilderInit
        ? "  maybeForceBuilderInitialization();"
        : "";

    printer.print(
        "// Construct using " + nameResolver.getImmutableClassName(descriptor) + ".newBuilder()\n" +
        "private Builder() {\n" +
        forceBuilderInit + "\n" +
        "}\n" +
        "\n");

    printer.print(
        "private Builder(\n" +
        "    com.google.protobuf.GeneratedMessage" + versionSuffix + ".BuilderParent parent) {\n" +
        "  super(parent);\n" +
        forceBuilderInit + "\n" +
        "}\n");

    if (needMaybeForceBuilderInit) {
      printer.print(
          "private void maybeForceBuilderInitialization() {\n" +
          "  if (com.google.protobuf.GeneratedMessage" + versionSuffix + "\n" +
          "          .alwaysUseFieldBuilders) {\n");
      printer.indent();
      printer.indent();
      for (FieldDescriptor field : descriptor.getFields()) {
        if (field.getContainingOneof() == null) {
          fieldGenerators.get(field).generateFieldBuilderInitializationCode(printer);
        }
      }
      printer.outdent();
      printer.outdent();
      printer.print(
          "  }\n" +
          "}\n");
    }

    printer.print(
        "@java.lang.Override\n" +
        "public Builder clear() {\n" +
        "  super.clear();\n");

    printer.indent();
    int totalBuilderInts = (descriptor.getFields().size() + 31) / 32;
    for (int i = 0; i < totalBuilderInts; i++) {
      printer.print(getBitFieldName(i) + " = 0;\n");
    }

    for (FieldDescriptor field : descriptor.getFields()) {
      fieldGenerators.get(field).generateBuilderClearCode(printer);
    }

    for (OneofDescriptor oneof : oneofs.values()) {
        printer.print(
            context.getOneofGeneratorInfo(oneof).name + "Case_ = 0;\n" +
            context.getOneofGeneratorInfo(oneof).name + "_ = null;\n");
    }

    printer.outdent();
    printer.print(
        "  return this;\n" +
        "}\n" +
        "\n");

    printer.print(
        "@java.lang.Override\n" +
        "public com.google.protobuf.Descriptors.Descriptor\n" +
        "    getDescriptorForType() {\n" +
        "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) +
        ".internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_descriptor;\n" +
        "}\n" +
        "\n");

    printer.print(
        "@java.lang.Override\n" +
        "public " + nameResolver.getImmutableClassName(descriptor) + " getDefaultInstanceForType() {\n" +
        "  return " + nameResolver.getImmutableClassName(descriptor) + ".getDefaultInstance();\n" +
        "}\n" +
        "\n");

    printer.print(
        "@java.lang.Override\n" +
        "public " + nameResolver.getImmutableClassName(descriptor) + " build() {\n" +
        "  " + nameResolver.getImmutableClassName(descriptor) + " result = buildPartial();\n" +
        "  if (!result.isInitialized()) {\n" +
        "    throw newUninitializedMessageException(result);\n" +
        "  }\n" +
        "  return result;\n" +
        "}\n" +
        "\n");

    generateBuildPartial(printer);

    if (context.getOptions().isOpensourceRuntime()) {
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
           String className = nameResolver.getImmutableClassName(descriptor);
           printer.print(
               "@java.lang.Override\n" +
               "public <Type> Builder setExtension(\n" +
               "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
               "        " + className + ", Type> extension,\n" +
               "    Type value) {\n" +
               "  return super.setExtension(extension, value);\n" +
               "}\n" +
               "@java.lang.Override\n" +
               "public <Type> Builder setExtension(\n" +
               "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
               "        " + className + ", java.util.List<Type>> extension,\n" +
               "    int index, Type value) {\n" +
               "  return super.setExtension(extension, index, value);\n" +
               "}\n" +
               "@java.lang.Override\n" +
               "public <Type> Builder addExtension(\n" +
               "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
               "        " + className + ", java.util.List<Type>> extension,\n" +
               "    Type value) {\n" +
               "  return super.addExtension(extension, value);\n" +
               "}\n" +
               "@java.lang.Override\n" +
               "public <T> Builder clearExtension(\n" +
               "    com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
               "        " + className + ", T> extension) {\n" +
               "  return super.clearExtension(extension);\n" +
               "}\n");
       }
    }

    if (context.hasGeneratedMethods(descriptor)) {
      String className = nameResolver.getImmutableClassName(descriptor);
      printer.print(
          "@java.lang.Override\n" +
          "public Builder mergeFrom(com.google.protobuf.Message other) {\n" +
          "  if (other instanceof " + className + ") {\n" +
          "    return mergeFrom((" + className + ")other);\n" +
          "  } else {\n" +
          "    super.mergeFrom(other);\n" +
          "    return this;\n" +
          "  }\n" +
          "}\n" +
          "\n");

      printer.print(
          "public Builder mergeFrom(" + className + " other) {\n" +
          "  if (other == " + className + ".getDefaultInstance()) return this;\n");
      printer.indent();

      for (FieldDescriptor field : descriptor.getFields()) {
        if (field.getContainingOneof() == null) {
          fieldGenerators.get(field).generateMergingCode(printer);
        }
      }

      for (OneofDescriptor oneof : oneofs.values()) {
        printer.print(
            "switch (other.get" + context.getOneofGeneratorInfo(oneof).capitalizedName + "Case()) {\n");
        printer.indent();
        for (FieldDescriptor field : oneof.getFields()) {
          printer.print(
              "case " + field.getName().toUpperCase() + ": {\n");
          printer.indent();
          fieldGenerators.get(field).generateMergingCode(printer);
          printer.print("break;\n");
          printer.outdent();
          printer.print("}\n");
        }
        printer.print(
            "case " + context.getOneofGeneratorInfo(oneof).name.toUpperCase() + "_NOT_SET: {\n" +
            "  break;\n" +
            "}\n");
        printer.outdent();
        printer.print("}\n");
      }

      printer.outdent();

      if (descriptor.getExtensions().size() > 0) {
        printer.print("  this.mergeExtensionFields(other);\n");
      }

      printer.print(
          "  this.mergeUnknownFields(other.getUnknownFields());\n" +
          "  onChanged();\n" +
          "  return this;\n" +
          "}\n" +
          "\n");
    }
  }

  private void generateBuildPartial(Printer printer) {
    String className = nameResolver.getImmutableClassName(descriptor);
    printer.print(
        "@java.lang.Override\n" +
        "public " + className + " buildPartial() {\n" +
        "  " + className + " result = new " + className + "(this);\n");

    printer.indent();

    boolean hasRepeatedFields = false;
    for (FieldDescriptor field : descriptor.getFields()) {
      if (bitfieldTracksMutability(field)) {
        hasRepeatedFields = true;
        printer.print("buildPartialRepeatedFields(result);\n");
        break;
      }
    }

    int totalBuilderInts = (descriptor.getFields().size() + 31) / 32;
    if (totalBuilderInts > 0) {
      for (int i = 0; i < totalBuilderInts; i++) {
        printer.print(
            "if (" + getBitFieldName(i) + " != 0) { buildPartial" + i + "(result); }\n");
      }
    }

    if (!oneofs.isEmpty()) {
      printer.print("buildPartialOneofs(result);\n");
    }

    printer.outdent();
    printer.print(
        "  onBuilt();\n" +
        "  return result;\n" +
        "}\n" +
        "\n");

    if (hasRepeatedFields) {
      printer.print(
          "private void buildPartialRepeatedFields(" + className + " result) {\n");
      printer.indent();
      for (FieldDescriptor field : descriptor.getFields()) {
        if (bitfieldTracksMutability(field)) {
          fieldGenerators.get(field).generateBuildingCode(printer);
        }
      }
      printer.outdent();
      printer.print("}\n\n");
    }

    int startField = 0;
    for (int i = 0; i < totalBuilderInts; i++) {
      startField = generateBuildPartialPiece(printer, i, startField);
    }

    if (!oneofs.isEmpty()) {
      printer.print(
          "private void buildPartialOneofs(" + className + " result) {\n");
      printer.indent();
      for (OneofDescriptor oneof : oneofs.values()) {
        String oneofName = context.getOneofGeneratorInfo(oneof).name;
        printer.print(
            "result." + oneofName + "Case_ = " + oneofName + "Case_;\n" +
            "result." + oneofName + "_ = this." + oneofName + "_;\n");
        for (FieldDescriptor field : oneof.getFields()) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
             fieldGenerators.get(field).generateBuildingCode(printer);
          }
        }
      }
      printer.outdent();
      printer.print("}\n\n");
    }
  }

  private int generateBuildPartialPiece(Printer printer, int piece, int firstField) {
      String className = nameResolver.getImmutableClassName(descriptor);
      printer.print(
          "private void buildPartial" + piece + "(" + className + " result) {\n" +
          "  int from_" + getBitFieldName(piece) + " = " + getBitFieldName(piece) + ";\n");
      printer.indent();

      // We need to track which bitfields in the message we've declared 'to_...' variables for.
      // But in Java we can just declare them as we go, assuming we don't redeclare.
      // Or we can pre-declare them.
      // C++ logic uses a set to track declared bitfields.
      List<Integer> declaredToBitfields = new ArrayList<>();

      int bit = 0;
      int next = firstField;
      for (; bit < 32 && next < descriptor.getFields().size(); ++next) {
          FieldDescriptor field = descriptor.getFields().get(next);
          ImmutableFieldGenerator generator = fieldGenerators.get(field);
          bit += generator.getNumBitsForBuilder();

          if (field.getContainingOneof() != null) continue;
          if (bitfieldTracksMutability(field)) continue;
          if (generator.getNumBitsForBuilder() == 0) continue;

          if (generator.getNumBitsForMessage() > 0) {
              int toBitfield = generator.getMessageBitIndex() / 32;
              if (!declaredToBitfields.contains(toBitfield)) {
                  printer.print("int to_" + getBitFieldName(toBitfield) + " = 0;\n");
                  declaredToBitfields.add(toBitfield);
              }
          }
          generator.generateBuildingCode(printer);
      }

      for (int toBitfield : declaredToBitfields) {
          printer.print("result." + getBitFieldName(toBitfield) + " |= to_" + getBitFieldName(toBitfield) + ";\n");
      }

      printer.outdent();
      printer.print("}\n\n");

      return next;
  }

  private void generateBuilderParsingMethods(Printer printer) {
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
      printer.indent();
      printer.indent();
      printer.indent();
      printer.indent();

      generateBuilderFieldParsingCases(printer);

      printer.outdent();
      printer.outdent();
      printer.outdent();
      printer.outdent();

      printer.print(
          "        default: {\n" +
          "          if (!super.parseUnknownField(input, extensionRegistry, tag)) {\n" +
          "            done = true; // was an endgroup tag\n" +
          "          }\n" +
          "          break;\n" +
          "        } // default:\n" +
          "      } // switch (tag)\n" +
          "    } // while (!done)\n" +
          "  } catch (com.google.protobuf.InvalidProtocolBufferException e) {\n" +
          "    throw e.unwrapIOException();\n" +
          "  } finally {\n" +
          "    onChanged();\n" +
          "  } // finally\n" +
          "  return this;\n" +
          "}\n");
  }

  private void generateBuilderFieldParsingCases(Printer printer) {
      List<FieldDescriptor> sortedFields = new ArrayList<>(descriptor.getFields());
      sortedFields.sort(Comparator.comparingInt(FieldDescriptor::getNumber));

      for (FieldDescriptor field : sortedFields) {
          generateBuilderFieldParsingCase(printer, field);
          if (field.isPackable()) {
              generateBuilderPackedFieldParsingCase(printer, field);
          }
      }
  }

  private void generateBuilderFieldParsingCase(Printer printer, FieldDescriptor field) {
      int tag = Helpers.makeTag(field.getNumber(), Helpers.getWireTypeForFieldType(field.getType()));
      printer.print("case " + tag + ": {\n");
      printer.indent();
      fieldGenerators.get(field).generateBuilderParsingCode(printer);
      printer.outdent();
      printer.print(
          "  break;\n" +
          "} // case " + tag + "\n");
  }

  private void generateBuilderPackedFieldParsingCase(Printer printer, FieldDescriptor field) {
      int tag = Helpers.makeTag(field.getNumber(), com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED);
      printer.print("case " + tag + ": {\n");
      printer.indent();
      fieldGenerators.get(field).generateBuilderParsingCodeFromPacked(printer);
      printer.outdent();
      printer.print(
          "  break;\n" +
          "} // case " + tag + "\n");
  }

  private void generateIsInitialized(Printer printer) {
      printer.print(
          "@java.lang.Override\n" +
          "public final boolean isInitialized() {\n");
      printer.indent();

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.isRequired()) {
              printer.print(
                  "if (!has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) {\n" +
                  "  return false;\n" +
                  "}\n");
          }
      }

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
                  if (field.isMapField()) {
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

      printer.outdent();
      printer.print(
          "  return true;\n" +
          "}\n" +
          "\n");
  }

  private void writeMessageDocComment(Printer printer, Descriptor descriptor) {
      com.rubberjam.protobuf.another.compiler.java.DocComment.writeMessageDocComment(
          printer, descriptor, new com.rubberjam.protobuf.another.compiler.java.DocComment.Options(), false);
  }

  // Helpers
  private String getBitFieldName(int index) {
      return "bitField" + index + "_";
  }

  private boolean bitfieldTracksMutability(FieldDescriptor field) {
      if (!field.isRepeated() || field.isMapField()) {
          return false;
      }
      return bitfieldTracksMutabilityExact(field);
  }

  private boolean bitfieldTracksMutabilityExact(FieldDescriptor field) {
      switch (field.getType()) {
          case GROUP:
          case MESSAGE:
          case ENUM:
              return true;
          default:
              return false;
      }
  }

  private String mapValueImmutableClassdName(Descriptor descriptor, ClassNameResolver nameResolver) {
      FieldDescriptor valueField = descriptor.findFieldByName("value");
      return nameResolver.getImmutableClassName(valueField.getMessageType());
  }
}
