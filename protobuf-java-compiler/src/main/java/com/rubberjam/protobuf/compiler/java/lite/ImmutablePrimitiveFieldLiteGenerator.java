package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImmutablePrimitiveFieldLiteGenerator implements ImmutableFieldLiteGenerator {
  protected final FieldDescriptor descriptor;
  protected final Context context;
  protected final ClassNameResolver nameResolver;
  protected final Map<String, Object> variables;
  protected final int messageBitIndex;

  public ImmutablePrimitiveFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.messageBitIndex = messageBitIndex;
    this.variables = new HashMap<>();

    FieldCommon.setCommonFieldVariables(descriptor, context.getFieldGeneratorInfo(descriptor), variables);
    variables.put("type", Helpers.getPrimitiveTypeName(Helpers.getJavaType(descriptor)));
    variables.put("boxed_type", Helpers.getBoxedPrimitiveTypeName(Helpers.getJavaType(descriptor)));
    variables.put("field_type", Helpers.getPrimitiveTypeName(Helpers.getJavaType(descriptor)));
    variables.put("default", Helpers.defaultValue(descriptor, true, nameResolver, context.getOptions()));
    variables.put("default_init", Helpers.defaultValue(descriptor, true, nameResolver, context.getOptions()));
    variables.put("capitalized_type", Helpers.getCapitalizedType(descriptor, true));
    variables.put("tag", String.valueOf(Helpers.makeTag(descriptor.getNumber(), Helpers.getWireTypeForFieldType(descriptor.getType()))));
    variables.put("tag_size", String.valueOf(com.google.protobuf.CodedOutputStream.computeTagSize(descriptor.getNumber())));
    variables.put("null_check", "if (value == null) {\n  throw new NullPointerException();\n}\n");

    // For fixed size types
    if (Helpers.getJavaType(descriptor) == Helpers.JavaType.INT || Helpers.getJavaType(descriptor) == Helpers.JavaType.LONG || Helpers.getJavaType(descriptor) == Helpers.JavaType.BOOLEAN) {
       variables.put("is_primitive", "true");
    } else {
       variables.put("is_primitive", "false");
    }
  }

  @Override
  public int getNumBitsForMessage() {
    // Check descriptor.
    if (descriptor.hasPresence()) {
        return 1;
    }
    return 0;
  }

  @Override
  public void generateInterfaceMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "$type$ ${$get$capitalized_name$$}$();\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateMembers(Printer printer) {
    printer.emit(variables, "private $type$ $name$_;\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ ${$get$capitalized_name$$}$() {\n" +
        "  return $name$_;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$($type$ value) {\n" +
        // (descriptor.getType() == FieldDescriptor.Type.STRING) ? "$null_check$" : "" + // primitives don't need null check
        "  $set_has_bit_message$\n" +
        "  $name$_ = value;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void clear$capitalized_name$() {\n" +
        "  $clear_has_bit_message$\n" +
        "  $name$_ = $default$;\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "public $type$ ${$get$capitalized_name$$}$() {\n" +
        "  return instance.get$capitalized_name$();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder ${$set$capitalized_name$$}$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "public Builder ${$clear$capitalized_name$$}$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    if (descriptor.hasPresence()) {
         Helpers.writeIntToUtf16CharSequence(messageBitIndex, output);
    }
    printer.emit(variables, "\"$name$_\",\n");
  }

  @Override
  public void generateInitializationCode(Printer printer) {
    if (!Helpers.isDefaultValueJavaDefault(descriptor)) {
        printer.emit(variables, "$name$_ = $default_init$;\n");
    }
  }

  @Override
  public String getBoxedType() {
    return (String) variables.get("boxed_type");
  }
}
