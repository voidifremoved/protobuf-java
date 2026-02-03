package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.Names;
import com.rubberjam.protobuf.compiler.java.GeneratorFactory.ExtensionGenerator;
import com.rubberjam.protobuf.compiler.java.Helpers.JavaType;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

public class ImmutableExtensionLiteGenerator extends ExtensionGenerator {

  private final Context context;
  private final String simpleName;

  public ImmutableExtensionLiteGenerator(FieldDescriptor descriptor, Context context) {
    super(descriptor);
    this.context = context;
    this.simpleName = Names.underscoresToCamelCase(descriptor);
  }

  @Override
  public void generate(Printer printer) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", simpleName);
    vars.put("containing_type", context.getNameResolver().getClassName(descriptor.getContainingType(), true));
    vars.put("number", String.valueOf(descriptor.getNumber()));

    JavaType javaType = Helpers.getJavaType(descriptor);
    String typeParam;
    if (javaType == JavaType.MESSAGE) {
      typeParam = context.getNameResolver().getClassName(descriptor.getMessageType(), true);
    } else if (javaType == JavaType.ENUM) {
      typeParam = context.getNameResolver().getClassName(descriptor.getEnumType(), true);
    } else {
      typeParam = Helpers.getBoxedPrimitiveTypeName(javaType);
    }

    String type;
    if (descriptor.isRepeated()) {
      type = "java.util.List<" + typeParam + ">";
    } else {
      type = typeParam;
    }
    vars.put("type", type);
    vars.put("field_type", "com.google.protobuf.WireFormat.FieldType." + Helpers.getFieldTypeName(descriptor.getType()));

    // Default value logic
    if (descriptor.isRepeated()) {
      vars.put("default_value", "null");
      vars.put("message_default_instance", "null");
      vars.put("enum_map", "null"); // TODO: Handle enum maps for repeated enums if needed
    } else {
      vars.put("default_value", Helpers.defaultValue(descriptor, true, context.getNameResolver(), context.getOptions()));
       if (javaType == JavaType.MESSAGE) {
          vars.put("message_default_instance", context.getNameResolver().getClassName(descriptor.getMessageType(), true) + ".getDefaultInstance()");
       } else {
          vars.put("message_default_instance", "null");
       }

       if (javaType == JavaType.ENUM) {
          vars.put("enum_map", context.getNameResolver().getClassName(descriptor.getEnumType(), true) + ".internalGetValueMap()");
       } else {
          vars.put("enum_map", "null");
       }
    }

    // Class literal
    // For primitive types, we need boxed class?
    // GeneratedMessageLite expects boxed class for type?
    // Let's assume yes.
    vars.put("singular_type_class", typeParam + ".class");


    printer.emit(vars,
      "public static final int $name$_VALUE = $number$;\n");

    if (descriptor.isRepeated()) {
      printer.emit(vars,
        "public static final\n" +
        "  com.google.protobuf.GeneratedMessageLite.GeneratedExtension<\n" +
        "    $containing_type$,\n" +
        "    $type$> $name$ = com.google.protobuf.GeneratedMessageLite\n" +
        "        .newRepeatedGeneratedExtension(\n" +
        "      $containing_type$.getDefaultInstance(),\n" +
        "      $message_default_instance$,\n" +
        "      $enum_map$,\n" +
        "      $number$,\n" +
        "      $field_type$,\n" +
        "      $singular_type_class$);\n");
    } else {
      printer.emit(vars,
        "public static final\n" +
        "  com.google.protobuf.GeneratedMessageLite.GeneratedExtension<\n" +
        "    $containing_type$,\n" +
        "    $type$> $name$ = com.google.protobuf.GeneratedMessageLite\n" +
        "        .newSingularGeneratedExtension(\n" +
        "      $containing_type$.getDefaultInstance(),\n" +
        "      $default_value$,\n" +
        "      $message_default_instance$,\n" +
        "      $enum_map$,\n" +
        "      $number$,\n" +
        "      $field_type$,\n" +
        "      $singular_type_class$);\n");
    }
  }

  @Override
  public int generateNonNestedInitializationCode(Printer printer) {
     // Lite handles initialization inline in definition.
     return 0;
  }

  @Override
  public int generateRegistrationCode(Printer printer) {
     Map<String, Object> vars = new HashMap<>();
     String fieldName;
      if (descriptor.getExtensionScope() != null) {
         fieldName = context.getNameResolver().getClassName(descriptor.getExtensionScope(), true) + "." + simpleName;
      } else {
         fieldName = simpleName;
      }
      vars.put("field_name", fieldName);
      printer.emit(vars, "registry.add($field_name$);\n");
      return 0;
  }
}
