package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon.FieldGeneratorMap;
import com.rubberjam.protobuf.another.compiler.java.GeneratorFactory;
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
 * Generates a message class.
 * Ported from java/message.cc.
 */
public class ImmutableMessageGenerator extends GeneratorFactory.MessageGenerator {

  private final Context context;
  private final ClassNameResolver nameResolver;
  private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;
  private final Map<Integer, OneofDescriptor> oneofs = new TreeMap<>();

  public ImmutableMessageGenerator(Descriptor descriptor, Context context) {
    super(descriptor);
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.fieldGenerators = ImmutableFieldGeneratorFactory.createFieldGenerators(descriptor, context);

    for (OneofDescriptor oneof : descriptor.getOneofs()) {
        oneofs.put(oneof.getIndex(), oneof);
    }
  }

  @Override
  public void generateStaticVariables(Printer printer, int[] bytecodeEstimate) {
      Map<String, Object> vars = new HashMap<>();
      vars.put("identifier", Helpers.uniqueFileScopeIdentifier(descriptor));
      vars.put("index", descriptor.getIndex());
      vars.put("classname", nameResolver.getImmutableClassName(descriptor));
      if (descriptor.getContainingType() != null) {
          vars.put("parent", Helpers.uniqueFileScopeIdentifier(descriptor.getContainingType()));
      }

      boolean multipleFiles = descriptor.getFile().getOptions().getJavaMultipleFiles();
      vars.put("private", multipleFiles ? "" : "private ");
      vars.put("final", (bytecodeEstimate[0] <= GeneratorCommon.kMaxStaticSize) ? "final " : "");

      printer.print(vars,
          "$private$static $final$com.google.protobuf.Descriptors.Descriptor\n" +
          "  internal_$identifier$_descriptor;\n");
      bytecodeEstimate[0] += 30;

      generateFieldAccessorTable(printer, bytecodeEstimate);

      for (Descriptor nested : descriptor.getNestedTypes()) {
          new ImmutableMessageGenerator(nested, context).generateStaticVariables(printer, bytecodeEstimate);
      }
  }

  @Override
  public int generateStaticVariableInitializers(Printer printer) {
      int bytecodeEstimate = 0;
      Map<String, Object> vars = new HashMap<>();
      vars.put("identifier", Helpers.uniqueFileScopeIdentifier(descriptor));
      vars.put("index", descriptor.getIndex());
      vars.put("classname", nameResolver.getImmutableClassName(descriptor));
      if (descriptor.getContainingType() != null) {
          vars.put("parent", Helpers.uniqueFileScopeIdentifier(descriptor.getContainingType()));
      }

      if (descriptor.getContainingType() == null) {
          printer.print(vars,
              "internal_$identifier$_descriptor =\n" +
              "  getDescriptor().getMessageTypes().get($index$);\n");
          bytecodeEstimate += 30;
      } else {
          printer.print(vars,
              "internal_$identifier$_descriptor =\n" +
              "  internal_$parent$_descriptor.getNestedTypes().get($index$);\n");
          bytecodeEstimate += 30;
      }

      bytecodeEstimate += generateFieldAccessorTableInitializer(printer);

      for (Descriptor nested : descriptor.getNestedTypes()) {
          bytecodeEstimate += new ImmutableMessageGenerator(nested, context).generateStaticVariableInitializers(printer);
      }
      return bytecodeEstimate;
  }

  private void generateFieldAccessorTable(Printer printer, int[] bytecodeEstimate) {
      Map<String, Object> vars = new HashMap<>();
      vars.put("identifier", Helpers.uniqueFileScopeIdentifier(descriptor));
      boolean multipleFiles = descriptor.getFile().getOptions().getJavaMultipleFiles();
      vars.put("private", multipleFiles ? "" : "private ");
      vars.put("final", (bytecodeEstimate[0] <= GeneratorCommon.kMaxStaticSize) ? "final " : "");
      vars.put("ver", Helpers.getGeneratedCodeVersionSuffix());

      printer.print(vars,
          "$private$static $final$\n" +
          "  com.google.protobuf.GeneratedMessage$ver$.FieldAccessorTable\n" +
          "    internal_$identifier$_fieldAccessorTable;\n");

      bytecodeEstimate[0] += 10 + 6 * descriptor.getFields().size() + 6 * descriptor.getRealOneofs().size();
  }

  private int generateFieldAccessorTableInitializer(Printer printer) {
      int bytecodeEstimate = 10;
      printer.print(
          "internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_fieldAccessorTable = new\n" +
          "  com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".FieldAccessorTable(\n" +
          "    internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_descriptor,\n" +
          "    new java.lang.String[] { ");

      for (FieldDescriptor field : descriptor.getFields()) {
          bytecodeEstimate += 6;
          printer.print("\"" + context.getFieldGeneratorInfo(field).capitalizedName + "\", ");
      }
      for (OneofDescriptor oneof : descriptor.getRealOneofs()) {
          bytecodeEstimate += 6;
          printer.print("\"" + context.getOneofGeneratorInfo(oneof).capitalizedName + "\", ");
      }
      printer.print("});\n");
      return bytecodeEstimate;
  }

  @Override
  public void generateInterface(Printer printer) {
      Helpers.maybePrintGeneratedAnnotation(context, printer, descriptor, true, "OrBuilder");
      if (!context.getOptions().isOpensourceRuntime()) {
          printer.print("@com.google.protobuf.Internal.ProtoNonnullApi\n");
      }

      String deprecation = descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
      String extraInterfaces = Helpers.getExtraMessageOrBuilderInterfaces(descriptor);

      if (descriptor.getExtensions().size() > 0) {
          printer.print(
              deprecation + "public interface " + descriptor.getName() + "OrBuilder extends\n" +
              "    " + extraInterfaces + "\n" +
              "    com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".\n" +
              "        ExtendableMessageOrBuilder<" + descriptor.getName() + "> {\n");
      } else {
          printer.print(
              deprecation + "public interface " + descriptor.getName() + "OrBuilder extends\n" +
              "    " + extraInterfaces + "\n" +
              "    com.google.protobuf.MessageOrBuilder {\n");
      }

      printer.indent();
      for (FieldDescriptor field : descriptor.getFields()) {
          printer.print("\n");
          fieldGenerators.get(field).generateInterfaceMembers(printer);
      }
      for (OneofDescriptor oneof : oneofs.values()) {
          String capitalizedName = context.getOneofGeneratorInfo(oneof).capitalizedName;
          printer.print(
              "\n" +
              nameResolver.getImmutableClassName(descriptor) + "." + capitalizedName + "Case " +
              "get" + capitalizedName + "Case();\n");
      }
      printer.outdent();
      printer.print("}\n");
  }

  @Override
  public void generate(Printer printer) {
      boolean isOwnFile = Helpers.isOwnFile(descriptor, true);
      Map<String, Object> vars = new HashMap<>();
      vars.put("static", isOwnFile ? "" : "static ");
      vars.put("classname", descriptor.getName());
      vars.put("extra_interfaces", Helpers.getExtraMessageInterfaces(descriptor));
      vars.put("ver", Helpers.getGeneratedCodeVersionSuffix());
      vars.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");

      DocComment.writeMessageDocComment(printer, descriptor, new DocComment.Options(), false);
      Helpers.maybePrintGeneratedAnnotation(context, printer, descriptor, true, null);
      if (!context.getOptions().isOpensourceRuntime()) {
          printer.print("@com.google.protobuf.Internal.ProtoNonnullApi\n");
      }

      String builderType;
      if (descriptor.getExtensions().size() > 0) {
          printer.print(vars,
              "$deprecation$public $static$final class $classname$ extends\n" +
              "    com.google.protobuf.GeneratedMessage$ver$.ExtendableMessage<\n" +
              "      $classname$> implements\n" +
              "    $extra_interfaces$\n" +
              "    $classname$OrBuilder {\n");
          builderType = "com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".ExtendableBuilder<" +
              nameResolver.getImmutableClassName(descriptor) + ", ?>";
      } else {
          printer.print(vars,
              "$deprecation$public $static$final class $classname$ extends\n" +
              "    com.google.protobuf.GeneratedMessage$ver$ implements\n" +
              "    $extra_interfaces$\n" +
              "    $classname$OrBuilder {\n");
          builderType = "com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".Builder<?>";
      }
      printer.print("private static final long serialVersionUID = 0L;\n");

      printer.indent();
      vars.put("buildertype", builderType);
      printer.print(vars,
          "// Use $classname$.newBuilder() to construct.\n" +
          "private $classname$($buildertype$ builder) {\n" +
          "  super(builder);\n" +
          "}\n" +
          "private $classname$() {\n");

      printer.indent();
      generateInitializers(printer);
      printer.outdent();
      printer.print(
          "}\n" +
          "\n");

      printer.print(vars,
          "@java.lang.Override\n" +
          "@SuppressWarnings({\"unused\"})\n" +
          "protected java.lang.Object newInstance(\n" +
          "    UnusedPrivateParameter unused) {\n" +
          "  return new $classname$();\n" +
          "}\n" +
          "\n");

      generateDescriptorMethods(printer);

      for (EnumDescriptor enumDesc : descriptor.getEnumTypes()) {
          new EnumGenerator(enumDesc, true, context).generate(printer);
      }

      for (Descriptor nested : descriptor.getNestedTypes()) {
          if (nested.getOptions().getMapEntry()) continue;
          ImmutableMessageGenerator messageGenerator = new ImmutableMessageGenerator(nested, context);
          messageGenerator.generateInterface(printer);
          messageGenerator.generate(printer);
      }

      int totalBits = 0;
      for (FieldDescriptor field : descriptor.getFields()) {
          totalBits += fieldGenerators.get(field).getNumBitsForMessage();
      }
      int totalInts = (totalBits + 31) / 32;
      for (int i = 0; i < totalInts; i++) {
          printer.print("private int " + getBitFieldName(i) + ";\n");
      }

      for (OneofDescriptor oneof : oneofs.values()) {
          String oneofName = context.getOneofGeneratorInfo(oneof).name;
          String oneofCapitalizedName = context.getOneofGeneratorInfo(oneof).capitalizedName;
          vars.put("oneof_name", oneofName);
          vars.put("oneof_capitalized_name", oneofCapitalizedName);

          printer.print(vars,
              "private int $oneof_name$Case_ = 0;\n" +
              "@SuppressWarnings(\"serial\")\n" +
              "private java.lang.Object $oneof_name$_;\n" +
              "public enum $oneof_capitalized_name$Case\n" +
              "    implements com.google.protobuf.Internal.EnumLite,\n" +
              "        com.google.protobuf.AbstractMessage.InternalOneOfEnum {\n");

          printer.indent();
          for (FieldDescriptor field : oneof.getFields()) {
              printer.print(
                  (field.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "") +
                  field.getName().toUpperCase() + "(" + field.getNumber() + "),\n");
          }
          printer.print(oneofName.toUpperCase() + "_NOT_SET(0);\n");

          printer.print(vars,
              "private final int value;\n" +
              "private $oneof_capitalized_name$Case(int value) {\n" +
              "  this.value = value;\n" +
              "}\n");

          if (context.getOptions().isOpensourceRuntime()) {
             printer.print(vars,
                 "/**\n" +
                 " * @param value The number of the enum to look for.\n" +
                 " * @return The enum associated with the given number.\n" +
                 " * @deprecated Use {@link #forNumber(int)} instead.\n" +
                 " */\n" +
                 "@java.lang.Deprecated\n" +
                 "public static $oneof_capitalized_name$Case valueOf(int value) {\n" +
                 "  return forNumber(value);\n" +
                 "}\n" +
                 "\n");
          }

          printer.print(vars,
              "public static $oneof_capitalized_name$Case forNumber(int value) {\n" +
              "  switch (value) {\n");
          for (FieldDescriptor field : oneof.getFields()) {
              printer.print("    case " + field.getNumber() + ": return " + field.getName().toUpperCase() + ";\n");
          }
          printer.print(
              "    case 0: return " + oneofName.toUpperCase() + "_NOT_SET;\n" +
              "    default: return null;\n" +
              "  }\n" +
              "}\n" +
              "public int getNumber() {\n" +
              "  return this.value;\n" +
              "}\n");
          printer.outdent();
          printer.print("};\n\n");

          printer.print(vars,
              "public $oneof_capitalized_name$Case\n" +
              "    get$oneof_capitalized_name$Case() {\n" +
              "  return $oneof_capitalized_name$Case.forNumber(\n" +
              "      $oneof_name$Case_);\n" +
              "}\n" +
              "\n");
      }

      if (Helpers.isAnyMessage(descriptor)) {
          generateAnyMethods(printer);
      }

      for (FieldDescriptor field : descriptor.getFields()) {
          printer.print(
              "public static final int " + Helpers.fieldConstantName(field) + " = " + field.getNumber() + ";\n");
          fieldGenerators.get(field).generateMembers(printer);
          printer.print("\n");
      }

      if (context.hasGeneratedMethods(descriptor)) {
          generateIsInitialized(printer);
          generateMessageSerializationMethods(printer);
          generateEqualsAndHashCode(printer);
      }

      generateParseFromMethods(printer);
      generateBuilder(printer);

      printer.print(
          "\n" +
          "// @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")\n");

      String className = nameResolver.getImmutableClassName(descriptor);
      printer.print(
          "private static final " + className + " DEFAULT_INSTANCE;\n" +
          "static {\n" +
          "  DEFAULT_INSTANCE = new " + className + "();\n" +
          "}\n" +
          "\n" +
          "public static " + className + " getDefaultInstance() {\n" +
          "  return DEFAULT_INSTANCE;\n" +
          "}\n" +
          "\n");

      generateParser(printer);

      printer.print(
          "@java.lang.Override\n" +
          "public " + className + " getDefaultInstanceForType() {\n" +
          "  return DEFAULT_INSTANCE;\n" +
          "}\n" +
          "\n");

      for (FieldDescriptor ext : descriptor.getExtensions()) {
          new ImmutableExtensionGenerator(ext, context).generate(printer);
      }

      printer.outdent();
      printer.print("}\n\n");
  }

  @Override
  public void generateExtensionRegistrationCode(Printer printer) {
      for (FieldDescriptor ext : descriptor.getExtensions()) {
          new ImmutableExtensionGenerator(ext, context).generateRegistrationCode(printer);
      }
      for (Descriptor nested : descriptor.getNestedTypes()) {
          new ImmutableMessageGenerator(nested, context).generateExtensionRegistrationCode(printer);
      }
  }

  private void generateInitializers(Printer printer) {
      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              fieldGenerators.get(field).generateInitializationCode(printer);
          }
      }
  }

  private void generateDescriptorMethods(Printer printer) {
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
              "@java.lang.Override\n" +
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
          ".internal_" + Helpers.uniqueFileScopeIdentifier(descriptor) + "_fieldAccessorTable\n" +
          "      .ensureFieldAccessorsInitialized(\n" +
          "          " + nameResolver.getImmutableClassName(descriptor) + ".class, " +
          nameResolver.getImmutableClassName(descriptor) + ".Builder.class);\n" +
          "}\n" +
          "\n");
  }

  private void generateAnyMethods(Printer printer) {
      // Ported from message.cc
      // ... implementation omitted for brevity or I need to implement it.
      // Based on the C++ code I fetched, it is quite long. I will implement a placeholder or TODO,
      // but strict requirements say "exactly".
      // I will implement it.
      printer.print(
        "private static String getTypeUrl(\n" +
        "    java.lang.String typeUrlPrefix,\n" +
        "    com.google.protobuf.Descriptors.Descriptor descriptor) {\n" +
        "  return typeUrlPrefix.endsWith(\"/\")\n" +
        "      ? typeUrlPrefix + descriptor.getFullName()\n" +
        "      : typeUrlPrefix + \"/\" + descriptor.getFullName();\n" +
        "}\n" +
        "\n" +
        "private static String getTypeNameFromTypeUrl(\n" +
        "    java.lang.String typeUrl) {\n" +
        "  int pos = typeUrl.lastIndexOf('/');\n" +
        "  return pos == -1 ? \"\" : typeUrl.substring(pos + 1);\n" +
        "}\n" +
        "\n" +
        "public static <T extends com.google.protobuf.Message> Any pack(\n" +
        "    T message) {\n" +
        "  return Any.newBuilder()\n" +
        "      .setTypeUrl(getTypeUrl(\"type.googleapis.com\",\n" +
        "                             message.getDescriptorForType()))\n" +
        "      .setValue(message.toByteString())\n" +
        "      .build();\n" +
        "}\n" +
        "\n" +
        "public static <T extends com.google.protobuf.Message> Any pack(\n" +
        "    T message, java.lang.String typeUrlPrefix) {\n" +
        "  return Any.newBuilder()\n" +
        "      .setTypeUrl(getTypeUrl(typeUrlPrefix,\n" +
        "                             message.getDescriptorForType()))\n" +
        "      .setValue(message.toByteString())\n" +
        "      .build();\n" +
        "}\n" +
        "\n" +
        "public <T extends com.google.protobuf.Message> boolean is(\n" +
        "    java.lang.Class<T> clazz) {\n" +
        "  T defaultInstance =\n" +
        "      com.google.protobuf.Internal.getDefaultInstance(clazz);\n" +
        "  return getTypeNameFromTypeUrl(getTypeUrl()).equals(\n" +
        "      defaultInstance.getDescriptorForType().getFullName());\n" +
        "}\n" +
        "\n" +
        "public boolean isSameTypeAs(com.google.protobuf.Message message) {\n" +
        "  return getTypeNameFromTypeUrl(getTypeUrl()).equals(\n" +
        "      message.getDescriptorForType().getFullName());\n" +
        "}\n" +
        "\n" +
        "@SuppressWarnings(\"serial\")\n" +
        "private volatile com.google.protobuf.Message cachedUnpackValue;\n" +
        "\n" +
        "@java.lang.SuppressWarnings(\"unchecked\")\n" +
        "public <T extends com.google.protobuf.Message> T unpack(\n" +
        "    java.lang.Class<T> clazz)\n" +
        "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
        "  boolean invalidClazz = false;\n" +
        "  if (cachedUnpackValue != null) {\n" +
        "    if (cachedUnpackValue.getClass() == clazz) {\n" +
        "      return (T) cachedUnpackValue;\n" +
        "    }\n" +
        "    invalidClazz = true;\n" +
        "  }\n" +
        "  if (invalidClazz || !is(clazz)) {\n" +
        "    throw new com.google.protobuf.InvalidProtocolBufferException(\n" +
        "        \"Type of the Any message does not match the given class.\");\n" +
        "  }\n" +
        "  T defaultInstance =\n" +
        "      com.google.protobuf.Internal.getDefaultInstance(clazz);\n" +
        "  T result = (T) defaultInstance.getParserForType()\n" +
        "      .parseFrom(getValue());\n" +
        "  cachedUnpackValue = result;\n" +
        "  return result;\n" +
        "}\n" +
        "\n" +
        "@java.lang.SuppressWarnings(\"unchecked\")\n" +
        "public <T extends com.google.protobuf.Message> T unpackSameTypeAs(T message)\n" +
        "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
        "  boolean invalidValue = false;\n" +
        "  if (cachedUnpackValue != null) {\n" +
        "    if (cachedUnpackValue.getClass() == message.getClass()) {\n" +
        "      return (T) cachedUnpackValue;\n" +
        "    }\n" +
        "    invalidValue = true;\n" +
        "  }\n" +
        "  if (invalidValue || !isSameTypeAs(message)) {\n" +
        "    throw new com.google.protobuf.InvalidProtocolBufferException(\n" +
        "        \"Type of the Any message does not match the given exemplar.\");\n" +
        "  }\n" +
        "  T result = (T) message.getParserForType().parseFrom(getValue());\n" +
        "  cachedUnpackValue = result;\n" +
        "  return result;\n" +
        "}\n" +
        "\n");
  }

  private void generateIsInitialized(Printer printer) {
      // Same logic as MessageBuilderGenerator but using memoizedIsInitialized
      // Ported from message.cc GenerateIsInitialized

      printer.print("private byte memoizedIsInitialized = -1;\n");
      printer.print(
          "@java.lang.Override\n" +
          "public final boolean isInitialized() {\n");
      printer.indent();
      printer.print(
          "byte isInitialized = memoizedIsInitialized;\n" +
          "if (isInitialized == 1) return true;\n" +
          "if (isInitialized == 0) return false;\n" +
          "\n");

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.isRequired()) {
              printer.print(
                  "if (!has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) {\n" +
                  "  memoizedIsInitialized = 0;\n" +
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
                      "  memoizedIsInitialized = 0;\n" +
                      "  return false;\n" +
                      "}\n");
              } else if (field.isOptional()) {
                  printer.print(
                      "if (has" + name + "()) {\n" +
                      "  if (!get" + name + "().isInitialized()) {\n" +
                      "    memoizedIsInitialized = 0;\n" +
                      "    return false;\n" +
                      "  }\n" +
                      "}\n");
              } else if (field.isRepeated()) {
                   if (field.isMapField()) {
                        // ...
                        printer.print(
                            "for (" + mapValueImmutableClassdName(field.getMessageType(), nameResolver) + " item : get" + name + "Map().values()) {\n" +
                            "  if (!item.isInitialized()) {\n" +
                            "    memoizedIsInitialized = 0;\n" +
                            "    return false;\n" +
                            "  }\n" +
                            "}\n");
                   } else {
                        printer.print(
                            "for (int i = 0; i < get" + name + "Count(); i++) {\n" +
                            "  if (!get" + name + "(i).isInitialized()) {\n" +
                            "    memoizedIsInitialized = 0;\n" +
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
              "  memoizedIsInitialized = 0;\n" +
              "  return false;\n" +
              "}\n");
      }

      printer.outdent();
      printer.print(
          "  memoizedIsInitialized = 1;\n" +
          "  return true;\n" +
          "}\n" +
          "\n");
  }

  private void generateMessageSerializationMethods(Printer printer) {
      // Ported from message.cc GenerateMessageSerializationMethods
      List<FieldDescriptor> sortedFields = new ArrayList<>(descriptor.getFields());
      sortedFields.sort(Comparator.comparingInt(FieldDescriptor::getNumber));

      printer.print(
          "@java.lang.Override\n" +
          "public void writeTo(com.google.protobuf.CodedOutputStream output)\n" +
          "                    throws java.io.IOException {\n");
      printer.indent();

      if (Helpers.hasPackedFields(descriptor)) {
          printer.print("getSerializedSize();\n");
      }

      if (descriptor.getExtensions().size() > 0) {
          if (descriptor.getOptions().getMessageSetWireFormat()) {
              printer.print(
                  "com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + "\n" +
                  "  .ExtendableMessage<" + nameResolver.getImmutableClassName(descriptor) + ">.ExtensionWriter\n" +
                  "    extensionWriter = newMessageSetExtensionWriter();\n");
          } else {
              printer.print(
                  "com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + "\n" +
                  "  .ExtendableMessage<" + nameResolver.getImmutableClassName(descriptor) + ">.ExtensionWriter\n" +
                  "    extensionWriter = newExtensionWriter();\n");
          }
      }

      for (FieldDescriptor field : sortedFields) {
          fieldGenerators.get(field).generateSerializationCode(printer);
      }

      if (descriptor.getOptions().getMessageSetWireFormat()) {
          printer.print("getUnknownFields().writeAsMessageSetTo(output);\n");
      } else {
          printer.print("getUnknownFields().writeTo(output);\n");
      }

      printer.outdent();
      printer.print(
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public int getSerializedSize() {\n" +
          "  int size = memoizedSize;\n" +
          "  if (size != -1) return size;\n" +
          "\n");
      printer.indent();
      printer.print("size = 0;\n");

      for (FieldDescriptor field : descriptor.getFields()) {
          fieldGenerators.get(field).generateSerializedSizeCode(printer);
      }

      if (descriptor.getExtensions().size() > 0) {
          if (descriptor.getOptions().getMessageSetWireFormat()) {
              printer.print("size += extensionsSerializedSizeAsMessageSet();\n");
          } else {
              printer.print("size += extensionsSerializedSize();\n");
          }
      }

      if (descriptor.getOptions().getMessageSetWireFormat()) {
          printer.print("size += getUnknownFields().getSerializedSizeAsMessageSet();\n");
      } else {
          printer.print("size += getUnknownFields().getSerializedSize();\n");
      }

      printer.print(
          "memoizedSize = size;\n" +
          "return size;\n");
      printer.outdent();
      printer.print(
          "}\n" +
          "\n");
  }

  private void generateEqualsAndHashCode(Printer printer) {
      // Ported from message.cc GenerateEqualsAndHashCode
      // ...
      printer.print(
          "@java.lang.Override\n" +
          "public boolean equals(final java.lang.Object obj) {\n");
      printer.indent();
      printer.print(
          "if (obj == this) {\n" +
          " return true;\n" +
          "}\n" +
          "if (!(obj instanceof " + nameResolver.getImmutableClassName(descriptor) + ")) {\n" +
          "  return super.equals(obj);\n" +
          "}\n" +
          nameResolver.getImmutableClassName(descriptor) + " other = (" + nameResolver.getImmutableClassName(descriptor) + ") obj;\n" +
          "\n");

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              if (field.hasPresence()) {
                  printer.print(
                      "if (has" + context.getFieldGeneratorInfo(field).capitalizedName + "() != other.has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) return false;\n" +
                      "if (has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) {\n");
                  printer.indent();
              }
              fieldGenerators.get(field).generateEqualsCode(printer);
              if (field.hasPresence()) {
                  printer.outdent();
                  printer.print("}\n");
              }
          }
      }

      for (OneofDescriptor oneof : oneofs.values()) {
          String capName = context.getOneofGeneratorInfo(oneof).capitalizedName;
          String name = context.getOneofGeneratorInfo(oneof).name;
          printer.print(
              "if (!get" + capName + "Case().equals(\n" +
              "    other.get" + capName + "Case())) return false;\n" +
              "switch (" + name + "Case_) {\n");
          printer.indent();
          for (FieldDescriptor field : oneof.getFields()) {
              printer.print("case " + field.getNumber() + ":\n");
              printer.indent();
              fieldGenerators.get(field).generateEqualsCode(printer);
              printer.print("break;\n");
              printer.outdent();
          }
          printer.print(
              "case 0:\n" +
              "default:\n");
          printer.outdent();
          printer.print("}\n");
      }

      printer.print(
          "if (!getUnknownFields().equals(other.getUnknownFields())) return false;\n");
      if (descriptor.getExtensions().size() > 0) {
          printer.print(
              "if (!getExtensionFields().equals(other.getExtensionFields()))\n" +
              "  return false;\n");
      }
      printer.print("return true;\n");
      printer.outdent();
      printer.print("}\n\n");

      printer.print(
          "@java.lang.Override\n" +
          "public int hashCode() {\n");
      printer.indent();
      printer.print(
          "if (memoizedHashCode != 0) {\n" +
          "  return memoizedHashCode;\n" +
          "}\n" +
          "int hash = 41;\n" +
          "hash = (19 * hash) + getDescriptor().hashCode();\n");

      for (FieldDescriptor field : descriptor.getFields()) {
          if (field.getContainingOneof() == null) {
              if (field.hasPresence()) {
                  printer.print("if (has" + context.getFieldGeneratorInfo(field).capitalizedName + "()) {\n");
                  printer.indent();
              }
              fieldGenerators.get(field).generateHashCodeCode(printer);
              if (field.hasPresence()) {
                  printer.outdent();
                  printer.print("}\n");
              }
          }
      }

      for (OneofDescriptor oneof : oneofs.values()) {
          String name = context.getOneofGeneratorInfo(oneof).name;
          printer.print("switch (" + name + "Case_) {\n");
          printer.indent();
          for (FieldDescriptor field : oneof.getFields()) {
              printer.print("case " + field.getNumber() + ":\n");
              printer.indent();
              fieldGenerators.get(field).generateHashCodeCode(printer);
              printer.print("break;\n");
              printer.outdent();
          }
          printer.print(
              "case 0:\n" +
              "default:\n");
          printer.outdent();
          printer.print("}\n");
      }

      if (descriptor.getExtensions().size() > 0) {
          printer.print("hash = hashFields(hash, getExtensionFields());\n");
      }

      printer.print(
          "hash = (29 * hash) + getUnknownFields().hashCode();\n" +
          "memoizedHashCode = hash;\n" +
          "return hash;\n");
      printer.outdent();
      printer.print("}\n\n");
  }

  private void generateParseFromMethods(Printer printer) {
      String className = nameResolver.getImmutableClassName(descriptor);
      String ver = Helpers.getGeneratedCodeVersionSuffix();

      printer.print(
          "public static " + className + " parseFrom(\n" +
          "    java.nio.ByteBuffer data)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    java.nio.ByteBuffer data,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data, extensionRegistry);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    com.google.protobuf.ByteString data)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    com.google.protobuf.ByteString data,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data, extensionRegistry);\n" +
          "}\n" +
          "public static " + className + " parseFrom(byte[] data)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    byte[] data,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "  return PARSER.parseFrom(data, extensionRegistry);\n" +
          "}\n" +
          "public static " + className + " parseFrom(java.io.InputStream input)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseWithIOException(PARSER, input);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    java.io.InputStream input,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseWithIOException(PARSER, input, extensionRegistry);\n" +
          "}\n" +
          "public static " + className + " parseDelimitedFrom(java.io.InputStream input)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseDelimitedWithIOException(PARSER, input);\n" +
          "}\n" +
          "public static " + className + " parseDelimitedFrom(\n" +
          "    java.io.InputStream input,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseDelimitedWithIOException(PARSER, input, extensionRegistry);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    com.google.protobuf.CodedInputStream input)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseWithIOException(PARSER, input);\n" +
          "}\n" +
          "public static " + className + " parseFrom(\n" +
          "    com.google.protobuf.CodedInputStream input,\n" +
          "    com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "    throws java.io.IOException {\n" +
          "  return com.google.protobuf.GeneratedMessage" + ver + "\n" +
          "      .parseWithIOException(PARSER, input, extensionRegistry);\n" +
          "}\n" +
          "\n");
  }

  private void generateBuilder(Printer printer) {
      String className = nameResolver.getImmutableClassName(descriptor);
      printer.print(
          "@java.lang.Override\n" +
          "public Builder newBuilderForType() { return newBuilder(); }\n" +
          "public static Builder newBuilder() {\n" +
          "  return DEFAULT_INSTANCE.toBuilder();\n" +
          "}\n" +
          "public static Builder newBuilder(" + className + " prototype) {\n" +
          "  return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);\n" +
          "}\n" +
          "@java.lang.Override\n" +
          "public Builder toBuilder() {\n" +
          "  return this == DEFAULT_INSTANCE\n" +
          "      ? new Builder() : new Builder().mergeFrom(this);\n" +
          "}\n" +
          "\n");

      printer.print(
          "@java.lang.Override\n" +
          "protected Builder newBuilderForType(\n" +
          "    com.google.protobuf.GeneratedMessage" + Helpers.getGeneratedCodeVersionSuffix() + ".BuilderParent parent) {\n" +
          "  Builder builder = new Builder(parent);\n" +
          "  return builder;\n" +
          "}\n");

      new MessageBuilderGenerator(descriptor, context).generate(printer);
  }

  private void generateParser(Printer printer) {
      String className = nameResolver.getImmutableClassName(descriptor);
      printer.print(
          "private static final com.google.protobuf.Parser<" + className + ">\n" +
          "    PARSER = new com.google.protobuf.AbstractParser<" + className + ">() {\n" +
          "  @java.lang.Override\n" +
          "  public " + className + " parsePartialFrom(\n" +
          "      com.google.protobuf.CodedInputStream input,\n" +
          "      com.google.protobuf.ExtensionRegistryLite extensionRegistry)\n" +
          "      throws com.google.protobuf.InvalidProtocolBufferException {\n" +
          "    Builder builder = newBuilder();\n" +
          "    try {\n" +
          "      builder.mergeFrom(input, extensionRegistry);\n" +
          "    } catch (com.google.protobuf.InvalidProtocolBufferException e) {\n" +
          "      throw e.setUnfinishedMessage(builder.buildPartial());\n" +
          "    } catch (com.google.protobuf.UninitializedMessageException e) {\n" +
          "      throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());\n" +
          "    } catch (java.io.IOException e) {\n" +
          "      throw new com.google.protobuf.InvalidProtocolBufferException(e)\n" +
          "          .setUnfinishedMessage(builder.buildPartial());\n" +
          "    }\n" +
          "    return builder.buildPartial();\n" +
          "  }\n" +
          "};\n" +
          "\n" +
          "public static com.google.protobuf.Parser<" + className + "> parser() {\n" +
          "  return PARSER;\n" +
          "}\n" +
          "\n" +
          "@java.lang.Override\n" +
          "public com.google.protobuf.Parser<" + className + "> getParserForType() {\n" +
          "  return PARSER;\n" +
          "}\n" +
          "\n");
  }

  // Helpers
  private String getBitFieldName(int index) {
      return "bitField" + index + "_";
  }

  private String mapValueImmutableClassdName(Descriptor descriptor, ClassNameResolver nameResolver) {
      FieldDescriptor valueField = descriptor.findFieldByName("value");
      return nameResolver.getImmutableClassName(valueField.getMessageType());
  }
}
