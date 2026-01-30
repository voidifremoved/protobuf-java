package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.GeneratorFactory;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates an enum class.
 * Ported from java/enum.cc.
 */
public class EnumGenerator extends GeneratorFactory.EnumGenerator {

  private final boolean immutableApi;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final List<EnumValueDescriptor> canonicalValues = new ArrayList<>();
  private final List<Alias> aliases = new ArrayList<>();

  private static class Alias {
    EnumValueDescriptor value;
    EnumValueDescriptor canonicalValue;
  }

  public EnumGenerator(EnumDescriptor descriptor, boolean immutableApi, Context context) {
    super(descriptor);
    this.immutableApi = immutableApi;
    this.context = context;
    this.nameResolver = context.getNameResolver();

    for (EnumValueDescriptor value : descriptor.getValues()) {
      EnumValueDescriptor canonicalValue = descriptor.findValueByNumber(value.getNumber());

      if (value == canonicalValue) {
        canonicalValues.add(value);
      } else {
        Alias alias = new Alias();
        alias.value = value;
        alias.canonicalValue = canonicalValue;
        aliases.add(alias);
      }
    }
  }

  @Override
  public void generate(Printer printer) {
    DocComment.writeMessageDocComment(printer, descriptor, new DocComment.Options(), false);

    if (!context.getOptions().isOpensourceRuntime()) {
      printer.print("@com.google.protobuf.Internal.ProtoNonnullApi\n");
    }

    Map<String, Object> vars = new HashMap<>();
    vars.put("classname", descriptor.getName());
    vars.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");

    printer.print(vars,
        "$deprecation$public enum $classname$\n" +
        "    implements com.google.protobuf.ProtocolMessageEnum {\n");

    printer.indent();

    boolean ordinalIsIndex = true;
    String indexText = "ordinal()";
    for (int i = 0; i < canonicalValues.size(); i++) {
      if (canonicalValues.get(i).getIndex() != i) {
        ordinalIsIndex = false;
        indexText = "index";
        break;
      }
    }

    for (EnumValueDescriptor value : canonicalValues) {
      vars.put("name", value.getName());
      vars.put("index", value.getIndex());
      vars.put("number", value.getNumber());

      if (value.getOptions().getDeprecated()) {
        printer.print("@java.lang.Deprecated\n");
      }
      if (ordinalIsIndex) {
        printer.print(vars, "$name$($number$),\n");
      } else {
        printer.print(vars, "$name$($index$, $number$),\n");
      }
    }

    // Check for open enum (proto3)
    if ("proto3".equals(descriptor.getFile().toProto().getSyntax())) {
        if (ordinalIsIndex) {
            printer.print("UNRECOGNIZED(-1),\n");
        } else {
            printer.print("UNRECOGNIZED(-1, -1),\n");
        }
    }

    printer.print(
        ";\n" +
        "\n");

    for (Alias alias : aliases) {
      vars.put("classname", descriptor.getName());
      vars.put("name", alias.value.getName());
      vars.put("canonical_name", alias.canonicalValue.getName());
      printer.print(vars,
          "public static final $classname$ $name$ = $canonical_name$;\n");
    }

    for (EnumValueDescriptor value : descriptor.getValues()) {
      vars.put("name", value.getName());
      vars.put("number", String.valueOf(value.getNumber()));
      vars.put("deprecation", value.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
      printer.print(vars,
          "$deprecation$public static final int $name$_VALUE = $number$;\n");
    }
    printer.print("\n");

    printer.print(
        "\n" +
        "public final int getNumber() {\n");

    if ("proto3".equals(descriptor.getFile().toProto().getSyntax())) {
      if (ordinalIsIndex) {
        printer.print(
            "  if (this == UNRECOGNIZED) {\n" +
            "    throw new java.lang.IllegalArgumentException(\n" +
            "        \"Can't get the number of an unknown enum value.\");\n" +
            "  }\n");
      } else {
        printer.print(
            "  if (index == -1) {\n" +
            "    throw new java.lang.IllegalArgumentException(\n" +
            "        \"Can't get the number of an unknown enum value.\");\n" +
            "  }\n");
      }
    }

    printer.print(
        "  return value;\n" +
        "}\n" +
        "\n");

    if (context.getOptions().isOpensourceRuntime()) {
      printer.print(vars,
          "/**\n" +
          " * @param value The numeric wire value of the corresponding enum entry.\n" +
          " * @return The enum associated with the given numeric wire value.\n" +
          " * @deprecated Use {@link #forNumber(int)} instead.\n" +
          " */\n" +
          "@java.lang.Deprecated\n" +
          "public static $classname$ valueOf(int value) {\n" +
          "  return forNumber(value);\n" +
          "}\n" +
          "\n");
    }

    printer.print(
        "/**\n" +
        " * @param value The numeric wire value of the corresponding enum entry.\n" +
        " * @return The enum associated with the given numeric wire value.\n" +
        " */\n");
    if (!context.getOptions().isOpensourceRuntime()) {
      printer.print("@com.google.protobuf.Internal.ProtoMethodMayReturnNull\n");
    }
    printer.print(vars,
        "public static $classname$ forNumber(int value) {\n" +
        "  switch (value) {\n");
    printer.indent();
    printer.indent();

    for (EnumValueDescriptor value : canonicalValues) {
      printer.print("case " + value.getNumber() + ": return " + value.getName() + ";\n");
    }

    printer.outdent();
    printer.outdent();
    printer.print(
        "    default: return null;\n" +
        "  }\n" +
        "}\n" +
        "\n" +
        "public static com.google.protobuf.Internal.EnumLiteMap<$classname$>\n" +
        "    internalGetValueMap() {\n" +
        "  return internalValueMap;\n" +
        "}\n" +
        "private static final com.google.protobuf.Internal.EnumLiteMap<\n" +
        "    $classname$> internalValueMap =\n" +
        "      new com.google.protobuf.Internal.EnumLiteMap<$classname$>() {\n" +
        "        public $classname$ findValueByNumber(int number) {\n" +
        "          return $classname$.forNumber(number);\n" +
        "        }\n" +
        "      };\n" +
        "\n");

    if (context.hasGeneratedMethods(descriptor)) {
      printer.print(
          "public final com.google.protobuf.Descriptors.EnumValueDescriptor\n" +
          "    getValueDescriptor() {\n");

      if ("proto3".equals(descriptor.getFile().toProto().getSyntax())) {
        if (ordinalIsIndex) {
          printer.print(
              "  if (this == UNRECOGNIZED) {\n" +
              "    throw new java.lang.IllegalStateException(\n" +
              "        \"Can't get the descriptor of an unrecognized enum value.\");\n" +
              "  }\n");
        } else {
          printer.print(
              "  if (index == -1) {\n" +
              "    throw new java.lang.IllegalStateException(\n" +
              "        \"Can't get the descriptor of an unrecognized enum value.\");\n" +
              "  }\n");
        }
      }

      printer.print(
          "  return getDescriptor().getValues().get(" + indexText + ");\n" +
          "}\n" +
          "public final com.google.protobuf.Descriptors.EnumDescriptor\n" +
          "    getDescriptorForType() {\n" +
          "  return getDescriptor();\n" +
          "}\n" +
          "public static final com.google.protobuf.Descriptors.EnumDescriptor\n" +
          "    getDescriptor() {\n");

      if (descriptor.getContainingType() == null) {
          printer.print(
              "  return " + nameResolver.getClassName(descriptor.getFile(), immutableApi) +
              ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");\n");
      } else {
          String parent = nameResolver.getClassName(descriptor.getContainingType(), immutableApi);
          String descriptorAccessor = descriptor.getContainingType().getOptions().getNoStandardDescriptorAccessor()
              ? "getDefaultInstance().getDescriptorForType()"
              : "getDescriptor()";

          printer.print(
              "  return " + parent + "." + descriptorAccessor + ".getEnumTypes().get(" + descriptor.getIndex() + ");\n");
      }

      printer.print(
          "}\n" +
          "\n" +
          "private static final $classname$[] VALUES = ");

      if (canUseEnumValues()) {
          printer.print("values();\n");
      } else {
          printer.print("getStaticValuesArray();\n");
          printer.print("private static $classname$[] getStaticValuesArray() {\n");
          printer.indent();
          printer.print("return new $classname$[] {\n" +
                        "  ");
          for (EnumValueDescriptor value : descriptor.getValues()) {
              printer.print(value.getName() + ", ");
          }
          printer.print("\n" +
                        "};\n");
          printer.outdent();
          printer.print("}\n");
      }

      printer.print(vars,
          "\n" +
          "public static $classname$ valueOf(\n" +
          "    com.google.protobuf.Descriptors.EnumValueDescriptor desc) {\n" +
          "  if (desc.getType() != getDescriptor()) {\n" +
          "    throw new java.lang.IllegalArgumentException(\n" +
          "      \"EnumValueDescriptor is not for this type.\");\n" +
          "  }\n");

      if ("proto3".equals(descriptor.getFile().toProto().getSyntax())) {
          printer.print(
              "  if (desc.getIndex() == -1) {\n" +
              "    return UNRECOGNIZED;\n" +
              "  }\n");
      }

      printer.print(
          "  return VALUES[desc.getIndex()];\n" +
          "}\n" +
          "\n");

      if (!ordinalIsIndex) {
        printer.print("private final int index;\n");
      }
    }

    printer.print("private final int value;\n\n");

    if (ordinalIsIndex) {
      printer.print(vars, "private $classname$(int value) {\n");
    } else {
      printer.print(vars, "private $classname$(int index, int value) {\n");
    }
    if (context.hasGeneratedMethods(descriptor) && !ordinalIsIndex) {
        printer.print("  this.index = index;\n");
    }
    printer.print(
        "  this.value = value;\n" +
        "}\n" +
        "\n");

    printer.print(
        "// @@protoc_insertion_point(enum_scope:" + descriptor.getFullName() + ")\n");

    printer.outdent();
    printer.print("}\n\n");
  }

  private boolean canUseEnumValues() {
    if (canonicalValues.size() != descriptor.getValues().size()) {
      return false;
    }
    for (int i = 0; i < descriptor.getValues().size(); i++) {
      if (!descriptor.getValues().get(i).getName().equals(canonicalValues.get(i).getName())) {
        return false;
      }
    }
    return true;
  }
}
