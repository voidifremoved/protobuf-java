package com.rubberjam.protobuf.another.compiler.java.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon.FieldGeneratorMap;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;
import java.util.HashMap;

public class MessageBuilderLiteGenerator {
  private final Descriptor descriptor;
  private final Context context;
  private final ClassNameResolver nameResolver;
  private final FieldGeneratorMap<ImmutableFieldLiteGenerator> fieldGenerators;

  public MessageBuilderLiteGenerator(Descriptor descriptor, Context context) {
    this.descriptor = descriptor;
    this.context = context;
    this.nameResolver = context.getNameResolver();
    this.fieldGenerators = ImmutableFieldLiteGeneratorFactory.createFieldGenerators(descriptor, context);
  }

  public void generate(Printer printer) {
    String className = nameResolver.getImmutableClassName(descriptor);
    Map<String, Object> vars = new HashMap<>();
    vars.put("classname", className);
    vars.put("visibility", Helpers.isOwnFile(descriptor, true) ? "public" : "private");
    vars.put("static", Helpers.isOwnFile(descriptor, true) ? " " : " static ");

    DocComment.writeMessageDocComment(printer, descriptor, context.getOptions(), false);
    printer.emit(vars,
        "$visibility$$static$final class Builder extends\n" +
        "    com.google.protobuf.GeneratedMessageLite.Builder<\n" +
        "      $classname$, Builder> implements\n" +
        "    // @@protoc_insertion_point(builder_implements:$classname$)\n" +
        "    $classname$OrBuilder {\n");
    printer.indent();

    printer.emit(vars,
        "// Construct using $classname$.newBuilder()\n" +
        "private Builder() {\n" +
        "  super(DEFAULT_INSTANCE);\n" +
        "}\n\n");

    // Generate field accessors
    for (FieldDescriptor field : descriptor.getFields()) {
      fieldGenerators.get(field).generateBuilderMembers(printer);
    }

    // Oneof clear methods if not handled by field generators?
    // Field generators (Oneof ones) handle setters/clearers.
    // But we might need clearOneofName().
    // In Lite, Oneof methods are usually per-field.

    printer.outdent();
    printer.emit("}\n");
  }
}
