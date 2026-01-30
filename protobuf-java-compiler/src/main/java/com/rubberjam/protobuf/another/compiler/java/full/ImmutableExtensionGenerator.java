package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorFactory.ExtensionGenerator;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.Helpers.JavaType;
import com.rubberjam.protobuf.another.compiler.java.Names;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

public class ImmutableExtensionGenerator extends ExtensionGenerator {

  private final Context context;
  private final String simpleName;

  public ImmutableExtensionGenerator(FieldDescriptor descriptor, Context context) {
    super(descriptor);
    this.context = context;
    this.simpleName = Names.underscoresToCamelCase(descriptor);
  }

  @Override
  public void generate(Printer printer) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", simpleName);
    vars.put("containing_type", context.getNameResolver().getClassName(descriptor.getContainingType(), true));

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

    printer.emit(vars,
      "public static final\n" +
      "  com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
      "    $containing_type$,\n" +
      "    $type$> $name$;\n");
  }

  @Override
  public int generateNonNestedInitializationCode(Printer printer) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", simpleName);

    String fieldName;
    if (descriptor.getExtensionScope() != null) {
       fieldName = context.getNameResolver().getClassName(descriptor.getExtensionScope(), true) + "." + simpleName;
    } else {
       fieldName = simpleName;
    }
    vars.put("field_name", fieldName);
    vars.put("index", String.valueOf(descriptor.getIndex()));

    String typeClass;
    JavaType javaType = Helpers.getJavaType(descriptor);
    if (javaType == JavaType.MESSAGE) {
      typeClass = context.getNameResolver().getClassName(descriptor.getMessageType(), true) + ".class";
      vars.put("default_instance", context.getNameResolver().getClassName(descriptor.getMessageType(), true) + ".getDefaultInstance()");
    } else if (javaType == JavaType.ENUM) {
       typeClass = context.getNameResolver().getClassName(descriptor.getEnumType(), true) + ".class";
       vars.put("default_instance", "null");
    } else {
       typeClass = Helpers.getBoxedPrimitiveTypeName(javaType) + ".class";
       vars.put("default_instance", "null");
    }
    vars.put("type_class", typeClass);

    if (descriptor.getExtensionScope() != null) {
      vars.put("scope_default_instance",
          context.getNameResolver().getClassName(descriptor.getExtensionScope(), true) + ".getDefaultInstance()");
      printer.emit(vars,
          "$field_name$ = com.google.protobuf.GeneratedMessage.newMessageScopedGeneratedExtension(\n" +
          "  $scope_default_instance$,\n" +
          "  $index$,\n" +
          "  $type_class$,\n" +
          "  $default_instance$);\n");
    } else {
      printer.emit(vars,
          "$field_name$ = com.google.protobuf.GeneratedMessage.newFileScopedGeneratedExtension(\n" +
          "  $type_class$,\n" +
          "  $default_instance$);\n");
    }

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
