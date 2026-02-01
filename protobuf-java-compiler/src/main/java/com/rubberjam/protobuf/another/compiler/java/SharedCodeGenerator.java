package com.rubberjam.protobuf.another.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.another.compiler.java.full.ImmutableExtensionGenerator;
import com.rubberjam.protobuf.another.compiler.java.full.ImmutableMessageGenerator;
import com.rubberjam.protobuf.io.Printer;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the shared code for a .proto file.
 * Ported from java/shared_code_generator.h and java/shared_code_generator.cc.
 */
public class SharedCodeGenerator {

  private final FileDescriptor file;
  private final Options options;
  private final Context context;
  private final ClassNameResolver nameResolver;

  public SharedCodeGenerator(FileDescriptor file, Options options) {
    this.file = file;
    this.options = options;
    this.context = new Context(file, options);
    this.nameResolver = context.getNameResolver();
  }

  public void generate(Printer printer) {
    printer.print(
        "public static com.google.protobuf.Descriptors.FileDescriptor\n" +
        "    getDescriptor() {\n" +
        "  return descriptor;\n" +
        "}\n");

    boolean isImmutable = !context.enforceLite();
    if (isImmutable) {
        printer.print(
            "private static  com.google.protobuf.Descriptors.FileDescriptor\n" +
            "    descriptor;\n");
    } else {
        if (context.enforceLite()) {
            return;
        }
    }

    printer.print("static {\n");
    printer.indent();

    generateDescriptors(printer);

    printer.outdent();
    printer.print("}\n");
  }

  private void generateDescriptors(Printer printer) {
    printer.print(
        "java.lang.String[] descriptorData = {\n");
    printer.indent();

    com.google.protobuf.DescriptorProtos.FileDescriptorProto.Builder fileProtoBuilder = file.toProto().toBuilder();
    fileProtoBuilder.clearSourceCodeInfo();
    if ("proto2".equals(fileProtoBuilder.getSyntax())) {
        fileProtoBuilder.clearSyntax();
    }
    com.google.protobuf.ByteString bytes = fileProtoBuilder.build().toByteString();
    List<String> pieces = new ArrayList<>();
    int chunkSize = 40;
    for (int i = 0; i < bytes.size(); i += chunkSize) {
        int end = Math.min(i + chunkSize, bytes.size());
        pieces.add(escapeBytes(bytes.substring(i, end)));
    }

    for (int i = 0; i < pieces.size(); i++) {
        if (i < pieces.size() - 1) {
            printer.print("\"" + pieces.get(i) + "\" +\n");
        } else {
            printer.print("\"" + pieces.get(i) + "\"\n");
        }
    }

    printer.outdent();
    printer.print(
        "};\n" +
        "descriptor = com.google.protobuf.Descriptors.FileDescriptor\n" +
        "  .internalBuildGeneratedFileFrom(descriptorData,\n" +
        "    new com.google.protobuf.Descriptors.FileDescriptor[] {\n");

    printer.indent();
    for (FileDescriptor dependency : file.getDependencies()) {
        String dependencyClass = nameResolver.getImmutableClassName(dependency);
        printer.print(dependencyClass + ".getDescriptor(),\n");
    }
    printer.outdent();
    printer.print(
        "    });\n");

    generateStaticVariables(printer);
    printer.print(
        "descriptor.resolveAllFeaturesImmutable();\n");
  }

  private void generateStaticVariables(Printer printer) {
      for (Descriptor message : file.getMessageTypes()) {
          new ImmutableMessageGenerator(message, context).generateStaticVariableInitializers(printer);
      }

      for (FieldDescriptor extension : file.getExtensions()) {
          new ImmutableExtensionGenerator(extension, context).generateRegistrationCode(printer);
      }
  }

  private static String escapeBytes(com.google.protobuf.ByteString input) {
    StringBuilder builder = new StringBuilder(input.size() * 4);
    for (int i = 0; i < input.size(); i++) {
      byte b = input.byteAt(i);
      char c = (char) (b & 0xFF);
      if (c == '\\') {
        builder.append("\\\\");
      } else if (c == '"') {
        builder.append("\\\"");
      } else if (c == '\'') {
        builder.append("\\'");
      } else if (c == '\n') {
        builder.append("\\n");
      } else if (c == '\r') {
        builder.append("\\r");
      } else if (c == '\t') {
        builder.append("\\t");
      } else if (c == '$') {
        // Escape $ so Printer does not treat it as variable delimiter.
        builder.append("\\044");
      } else if (c >= 0x20 && c < 0x7F) {
        builder.append(c);
      } else {
        builder.append(String.format("\\%03o", (int) c & 0xFF));
      }
    }
    return builder.toString();
  }
}
