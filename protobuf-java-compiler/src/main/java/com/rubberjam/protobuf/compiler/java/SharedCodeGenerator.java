package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FileDescriptor;
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

  public void generateDescriptors(Printer printer) {
    printer.print(
        "java.lang.String[] descriptorData = {\n");
    printer.indent();

    com.google.protobuf.DescriptorProtos.FileDescriptorProto fileProto = rebuildDescriptorProto(file);
    com.google.protobuf.ByteString bytes = fileProto.toByteString();
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
  }

  private com.google.protobuf.DescriptorProtos.FileDescriptorProto rebuildDescriptorProto(com.google.protobuf.Descriptors.FileDescriptor file) {
    com.google.protobuf.DescriptorProtos.FileDescriptorProto.Builder builder = file.toProto().toBuilder();
    builder.clearSourceCodeInfo();
    if ("proto2".equals(file.toProto().getSyntax())) {
      builder.clearSyntax();
    }

    for (int i = 0; i < file.getMessageTypes().size(); i++) {
        updateMessageProto(builder.getMessageTypeBuilder(i), file.getMessageTypes().get(i));
    }
    for (int i = 0; i < file.getExtensions().size(); i++) {
        updateFieldProto(builder.getExtensionBuilder(i), file.getExtensions().get(i));
    }

    return builder.build();
  }

  private void updateMessageProto(com.google.protobuf.DescriptorProtos.DescriptorProto.Builder builder, com.google.protobuf.Descriptors.Descriptor descriptor) {
      for (int i = 0; i < descriptor.getFields().size(); i++) {
          updateFieldProto(builder.getFieldBuilder(i), descriptor.getFields().get(i));
      }
      for (int i = 0; i < descriptor.getNestedTypes().size(); i++) {
          updateMessageProto(builder.getNestedTypeBuilder(i), descriptor.getNestedTypes().get(i));
      }
      for (int i = 0; i < descriptor.getExtensions().size(); i++) {
          updateFieldProto(builder.getExtensionBuilder(i), descriptor.getExtensions().get(i));
      }
  }

  private void updateFieldProto(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Builder builder, com.google.protobuf.Descriptors.FieldDescriptor field) {
      builder.setType(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.forNumber(field.getType().toProto().getNumber()));
      builder.setLabel(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.forNumber(field.toProto().getLabel().getNumber()));

      if (field.getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE ||
          field.getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP) {
          builder.setTypeName("." + field.getMessageType().getFullName());
      } else if (field.getType() == com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM) {
          builder.setTypeName("." + field.getEnumType().getFullName());
      }

      if (field.hasDefaultValue()) {
          builder.setDefaultValue(formatDefaultValue(field));
      }

      if (field.getContainingOneof() != null) {
          builder.setOneofIndex(field.getContainingOneof().getIndex());
      }
  }

  private String formatDefaultValue(com.google.protobuf.Descriptors.FieldDescriptor field) {
      Object val = field.getDefaultValue();
      switch (field.getType()) {
          case UINT32:
          case FIXED32:
              return Integer.toUnsignedString((Integer)val);
          case UINT64:
          case FIXED64:
              return Long.toUnsignedString((Long)val);
          case FLOAT: {
              float f = (Float) val;
              if (Float.isInfinite(f)) return f < 0 ? "-inf" : "inf";
              if (Float.isNaN(f)) return "nan";
              return Helpers.formatFloat(f);
          }
          case DOUBLE: {
              double d = (Double) val;
              if (Double.isInfinite(d)) return d < 0 ? "-inf" : "inf";
              if (Double.isNaN(d)) return "nan";
              return Helpers.formatDouble(d);
          }
          case ENUM:
              return ((com.google.protobuf.Descriptors.EnumValueDescriptor)val).getName();
          case BYTES:
              return escapeBytesInDescriptor((com.google.protobuf.ByteString)val);
          default:
              return val.toString();
      }
  }

  private String escapeBytesInDescriptor(com.google.protobuf.ByteString bytes) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.size(); i++) {
          byte b = bytes.byteAt(i);
          if (b == '\n') sb.append("\\n");
          else if (b == '\r') sb.append("\\r");
          else if (b == '\t') sb.append("\\t");
          else if (b == '\"') sb.append("\\\"");
          else if (b == '\'') sb.append("\\\'");
          else if (b == '\\') sb.append("\\\\");
          else if (b >= 0x20 && b <= 0x7E) sb.append((char)b);
          else sb.append(String.format("\\%03o", b & 0xFF));
      }
      return sb.toString();
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
