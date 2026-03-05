package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.Names;
import com.rubberjam.protobuf.compiler.java.GeneratorFactory.ExtensionGenerator;
import com.rubberjam.protobuf.compiler.java.Helpers.JavaType;
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
    vars.put("constant_name", Helpers.fieldConstantName(descriptor));
    vars.put("number", String.valueOf(descriptor.getNumber()));

    JavaType javaType = Helpers.getJavaType(descriptor);
    String singularType;
    if (javaType == JavaType.MESSAGE) {
      singularType = context.getNameResolver().getClassName(descriptor.getMessageType(), true);
    } else if (javaType == JavaType.ENUM) {
      singularType = context.getNameResolver().getClassName(descriptor.getEnumType(), true);
    } else {
      singularType = Helpers.getBoxedPrimitiveTypeName(javaType);
    }
    vars.put("singular_type", singularType);

    String type;
    if (descriptor.isRepeated()) {
      type = "java.util.List<" + singularType + ">";
    } else {
      type = singularType;
    }
    vars.put("type", type);
    vars.put("prototype", javaType == JavaType.MESSAGE
        ? context.getNameResolver().getClassName(descriptor.getMessageType(), true) + ".getDefaultInstance()"
        : "null");

    printer.emit(vars, "public static final int $constant_name$ = $number$;\n");

    com.rubberjam.protobuf.compiler.java.DocComment.writeFieldDocComment(printer, descriptor, context.getOptions(), false);

    if (descriptor.getExtensionScope() == null) {
      printer.emit(vars,
          "public static final\n" +
          "  com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
          "    $containing_type$,\n" +
          "    $type$> $name$ = com.google.protobuf.GeneratedMessage\n" +
          "        .newFileScopedGeneratedExtension(\n" +
          "      $singular_type$.class,\n" +
          "      $prototype$);\n");
    } else {
      vars.put("scope", context.getNameResolver().getClassName(descriptor.getExtensionScope(), true));
      vars.put("index", String.valueOf(descriptor.getIndex()));
      printer.emit(vars,
          "public static final\n" +
          "  com.google.protobuf.GeneratedMessage.GeneratedExtension<\n" +
          "    $containing_type$,\n" +
          "    $type$> $name$ = com.google.protobuf.GeneratedMessage\n" +
          "        .newMessageScopedGeneratedExtension(\n" +
          "      $scope$.getDefaultInstance(),\n" +
          "      $index$,\n" +
          "      $singular_type$.class,\n" +
          "      $prototype$);\n");
    }
  }

  @Override
  public int generateNonNestedInitializationCode(Printer printer) {
    return 0;
  }

  @Override
  public int generateRegistrationCode(Printer printer) {
     Map<String, Object> vars = new HashMap<>();
     String fieldName;
      if (descriptor.getExtensionScope() != null) {
         fieldName = context.getNameResolver().getClassName(descriptor.getExtensionScope(), true) + "." + simpleName;
      } else {
         fieldName = context.getNameResolver().getImmutableClassName(descriptor.getFile()) + "." + simpleName;
      }
      vars.put("field_name", fieldName);
      printer.emit(vars, "registry.add($field_name$);\n");
      return 0;
  }
}
