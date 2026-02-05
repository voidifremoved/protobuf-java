package com.rubberjam.protobuf.compiler.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.GenericDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.io.Printer;

public final class DocComment
{

  private DocComment()
  {
  }

  public enum AccessorType
  {
    HAZZER,
    GETTER,
    SETTER,
        BYTES_SETTER,
    CLEARER,
        BYTES_GETTER,
    // Repeated
    LIST_COUNT,
    LIST_GETTER,
    LIST_INDEXED_GETTER,
    LIST_INDEXED_SETTER,
    LIST_ADDER,
    LIST_MULTI_ADDER,
    // Map
    MAP_ENTRY_ADDER,
    MAP_MULTI_ADDER,
    MAP_ENTRY_REMOVER,
    OR_BUILDER_GETTER,
    OR_BUILDER_LIST_GETTER,
    OR_BUILDER_INDEXED_GETTER
  }

  /**
   * Escapes the input string for inclusion in Javadoc.
   *
   */
  public static String escapeJavadoc(String input)
  {
    StringBuilder result = new StringBuilder(input.length() * 2);
    char prev = '*';

    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      switch (c)
      {
      case '*':
        // Avoid "/*".
        if (prev == '/')
        {
          result.append("&#42;");
        }
        else
        {
          result.append(c);
        }
        break;
      case '/':
        // Avoid "*/".
        if (prev == '*')
        {
          result.append("&#47;");
        }
        else
        {
          result.append(c);
        }
        break;
      case '@':
        // '@' starts javadoc tags.
        result.append("&#64;");
        break;
      case '<':
        // Avoid interpretation as HTML.
        result.append("&lt;");
        break;
      case '>':
        // Avoid interpretation as HTML.
        result.append("&gt;");
        break;
      case '&':
        // Avoid interpretation as HTML.
        result.append("&amp;");
        break;
      case '\\':
        // Java interprets Unicode escape sequences anywhere!
        result.append("&#92;");
        break;
      default:
        result.append(c);
        break;
      }
      prev = c;
    }

    return result.toString();
  }

  /**
   * Escapes the input string for inclusion in KDoc.
   *
   */
  public static String escapeKdoc(String input)
  {
    StringBuilder result = new StringBuilder(input.length() * 2);
    char prev = 'a'; // Initial dummy value

    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      switch (c)
      {
      case '*':
        // Avoid "/*".
        if (prev == '/')
        {
          result.append("&#42;");
        }
        else
        {
          result.append(c);
        }
        break;
      case '/':
        // Avoid "*/".
        if (prev == '*')
        {
          result.append("&#47;");
        }
        else
        {
          result.append(c);
        }
        break;
      default:
        result.append(c);
        break;
      }
      prev = c;
    }
    return result.toString();
  }

  private static void writeDocCommentBodyForLocation(
      Printer printer, SourceCodeInfo.Location location, Options options, boolean kdoc)
  {
    if (options.isStripNonfunctionalCodegen())
    {
      return;
    }

    String comments = location.getLeadingComments();
    if (comments.isEmpty())
    {
      comments = location.getTrailingComments();
    }

    if (!comments.isEmpty())
    {
      if (kdoc)
      {
        comments = escapeKdoc(comments);
      }
      else
      {
        comments = escapeJavadoc(comments);
      }

      String[] lines = comments.split("\n");

      if (kdoc)
      {
        printer.emit(" * ```\n");
      }
      else
      {
        printer.emit(" * <pre>\n");
      }

      for (String line : lines)
      {
        String trimmed = stripLeadingWhitespace(line);
        if (!trimmed.isEmpty())
        {
          printer.emit(Map.of("line", trimmed), " * $line$\n");
        }
        else
        {
          printer.emit(" *\n");
        }
      }

      if (kdoc)
      {
        printer.emit(" * ```\n");
      }
      else
      {
        printer.emit(" * </pre>\n");
      }
      printer.emit(" *\n");
    }
  }

  private static String stripLeadingWhitespace(String s)
  {
        // We only strip ' ' (space) not tabs or others, to match C++ behavior presumably,
        // but java Character.isWhitespace matches more.
        // Let's use simple logic.
    int i = 0;
    while (i < s.length() && s.charAt(i) == ' ')
    {
      i++;
    }
    return s.substring(i);
  }

  public static void writeDocCommentBody(
      Printer printer, GenericDescriptor descriptor, Options options, boolean kdoc)
  {
    SourceCodeInfo.Location location = getLocation(descriptor);
    if (location != null)
    {
      writeDocCommentBodyForLocation(printer, location, options, kdoc);
    }
  }

  private static SourceCodeInfo.Location getLocation(GenericDescriptor descriptor)
  {
        List<Integer> path = getPath(descriptor);
        if (path == null) return null;

        FileDescriptor file = descriptor.getFile();
        SourceCodeInfo sourceCodeInfo = file.toProto().getSourceCodeInfo();

        for (SourceCodeInfo.Location loc : sourceCodeInfo.getLocationList()) {
            if (loc.getPathList().equals(path)) {
                return loc;
            }
        }
    return null;
  }

    private static List<Integer> getPath(GenericDescriptor descriptor) {
        List<Integer> path = new ArrayList<>();
        getPathRecursive(descriptor, path);
        return path;
    }

    private static void getPathRecursive(GenericDescriptor descriptor, List<Integer> path) {
        if (descriptor instanceof FileDescriptor) {
            return; // Base case
        }

        GenericDescriptor parent = null;
        if (descriptor instanceof Descriptor) {
             parent = ((Descriptor) descriptor).getContainingType();
        } else if (descriptor instanceof FieldDescriptor) {
             parent = ((FieldDescriptor) descriptor).getContainingType();
        } else if (descriptor instanceof EnumDescriptor) {
             parent = ((EnumDescriptor) descriptor).getContainingType();
        } else if (descriptor instanceof EnumValueDescriptor) {
             parent = ((EnumValueDescriptor) descriptor).getType();
        } else if (descriptor instanceof ServiceDescriptor) {
             // Top level usually
             parent = null;
        } else if (descriptor instanceof MethodDescriptor) {
             parent = ((MethodDescriptor) descriptor).getService();
        }

        if (parent != null) {
            getPathRecursive(parent, path);
        }

        // Append current descriptor's path component
        if (descriptor instanceof Descriptor) {
            if (parent == null) {
                path.add(com.google.protobuf.DescriptorProtos.FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER);
            } else {
                path.add(com.google.protobuf.DescriptorProtos.DescriptorProto.NESTED_TYPE_FIELD_NUMBER);
            }
            path.add(((Descriptor)descriptor).getIndex());
        } else if (descriptor instanceof FieldDescriptor) {
            FieldDescriptor fd = (FieldDescriptor) descriptor;
            if (fd.isExtension()) {
                 if (parent == null) {
                      path.add(com.google.protobuf.DescriptorProtos.FileDescriptorProto.EXTENSION_FIELD_NUMBER);
                 } else {
                      path.add(com.google.protobuf.DescriptorProtos.DescriptorProto.EXTENSION_FIELD_NUMBER);
                 }
            } else {
                 path.add(com.google.protobuf.DescriptorProtos.DescriptorProto.FIELD_FIELD_NUMBER);
            }
            path.add(fd.getIndex());
        } else if (descriptor instanceof EnumDescriptor) {
            if (parent == null) {
                path.add(com.google.protobuf.DescriptorProtos.FileDescriptorProto.ENUM_TYPE_FIELD_NUMBER);
            } else {
                path.add(com.google.protobuf.DescriptorProtos.DescriptorProto.ENUM_TYPE_FIELD_NUMBER);
            }
            path.add(((EnumDescriptor)descriptor).getIndex());
        } else if (descriptor instanceof EnumValueDescriptor) {
            path.add(com.google.protobuf.DescriptorProtos.EnumDescriptorProto.VALUE_FIELD_NUMBER);
            path.add(((EnumValueDescriptor)descriptor).getIndex());
        } else if (descriptor instanceof ServiceDescriptor) {
            path.add(com.google.protobuf.DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER);
            path.add(((ServiceDescriptor)descriptor).getIndex());
        } else if (descriptor instanceof MethodDescriptor) {
            path.add(com.google.protobuf.DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER);
            path.add(((MethodDescriptor)descriptor).getIndex());
        }
    }

  private static void writeDebugString(
      Printer printer, FieldDescriptor field, Options options, boolean kdoc)
  {
        String fieldComment = getFieldDefinition(field);

    if (kdoc)
    {
      printer.emit(Map.of("def", escapeKdoc(fieldComment)), " * `$def$`\n");
    }
    else
    {
      printer.emit(Map.of("def", escapeJavadoc(fieldComment)), " * <code>$def$</code>\n");
    }
  }

    private static String getFieldDefinition(FieldDescriptor field) {
        StringBuilder sb = new StringBuilder();

        if (field.isRepeated()) {
            sb.append("repeated ");
        } else if (field.isRequired()) {
            sb.append("required ");
        } else {
            // Check syntax via toProto() string
            String syntax = field.getFile().toProto().getSyntax();
            if (syntax.isEmpty() || "proto2".equals(syntax)) {
                sb.append("optional ");
            } else if (field.toProto().hasProto3Optional()) {
                 if (field.toProto().getLabel() == com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) {
                     if ("proto3".equals(syntax)) {
                         if (field.toProto().getProto3Optional()) {
                             sb.append("optional ");
                         }
                     } else {
                         sb.append("optional ");
                     }
                 }
            } else {
                 if (syntax.isEmpty() || "proto2".equals(syntax)) {
                     sb.append("optional ");
                 }
            }
        }

        if (field.getType() == FieldDescriptor.Type.GROUP) {
             sb.append("group ").append(field.getMessageType().getName());
        } else if (field.getType() == FieldDescriptor.Type.MESSAGE) {
             sb.append(".").append(field.getMessageType().getFullName()).append(" ").append(field.getName());
        } else if (field.getType() == FieldDescriptor.Type.ENUM) {
             sb.append(".").append(field.getEnumType().getFullName()).append(" ").append(field.getName());
        } else {
             sb.append(field.getType().name().toLowerCase()).append(" ").append(field.getName());
        }

        sb.append(" = ").append(field.getNumber());

        if (field.getType() == FieldDescriptor.Type.GROUP) {
            sb.append(" { ... }");
        } else {
            if (shouldPrintDefault(field)) {
                 sb.append(" [default = ");
                 sb.append(formatDefaultValue(field));
                 sb.append("]");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    private static boolean shouldPrintDefault(FieldDescriptor field) {
        if (field.isRepeated()) return false;
        if (field.getType() == FieldDescriptor.Type.MESSAGE) return false;
        if (field.getType() == FieldDescriptor.Type.GROUP) return false;

        String syntax = field.getFile().toProto().getSyntax();
        if ("proto3".equals(syntax)) {
            return false;
        }

        return field.hasDefaultValue();
    }

    private static String formatDefaultValue(FieldDescriptor field) {
        Object val = field.getDefaultValue();
        if (field.getType() == FieldDescriptor.Type.STRING) {
            return "\"" + escapeString((String)val) + "\"";
        } else if (field.getType() == FieldDescriptor.Type.BYTES) {
            return "\"" + escapeString(((com.google.protobuf.ByteString)val).toStringUtf8()) + "\"";
        } else if (field.getType() == FieldDescriptor.Type.ENUM) {
            return ((EnumValueDescriptor)val).getName();
        } else {
            return val.toString();
        }
    }

    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r");
    }

  public static void writeMessageDocComment(
      Printer printer, Descriptor message, Options options, boolean kdoc)
  {
    printer.emit("/**\n");
    writeDocCommentBody(printer, message, options, kdoc);
    if (kdoc)
    {
      printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
          " * Protobuf type `$fullname$`\n" + " */\n");
    }
    else
    {
      printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
          " * Protobuf type {@code $fullname$}\n" + " */\n");
    }
  }

  public static void writeMessageDocComment(
      Printer printer, EnumDescriptor message, Options options, boolean kdoc)
  {
    printer.emit("/**\n");
    writeDocCommentBody(printer, message, options, kdoc);
    if (kdoc)
    {
      printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
          " * Protobuf enum `$fullname$`\n" + " */\n");
    }
    else
    {
      printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
          " * Protobuf enum {@code $fullname$}\n" + " */\n");
    }
  }

  public static void writeMessageDocComment(
      Printer printer, ServiceDescriptor message, Options options, boolean kdoc)
  {
    printer.emit("/**\n");
    writeDocCommentBody(printer, message, options, kdoc);
    if (kdoc)
    {
      printer.emit(Map.of("fullname", escapeKdoc(message.getFullName())),
          " * Protobuf service `$fullname$`\n" + " */\n");
    }
    else
    {
      printer.emit(Map.of("fullname", escapeJavadoc(message.getFullName())),
          " * Protobuf service {@code $fullname$}\n" + " */\n");
    }
  }

  public static void writeFieldDocComment(
      Printer printer, FieldDescriptor field, Options options, boolean kdoc)
  {
    printer.emit("/**\n");
    writeDocCommentBody(printer, field, options, kdoc);
    writeDebugString(printer, field, options, kdoc);
    printer.emit(" */\n");
  }

  public static void writeDeprecatedJavadoc(
      Printer printer, FieldDescriptor field, Options options)
  {
    if (!field.getOptions().getDeprecated())
    {
      return;
    }
    printer.emit(Map.of("name", field.getFullName()), " * @deprecated $name$ is deprecated.\n");
  }

  public static void writeFieldAccessorDocComment(
      Printer printer,
      FieldDescriptor field,
      AccessorType type,
      Options options,
      boolean builder,
      boolean kdoc,
      boolean isPrivate)
  {
    printer.emit("/**\n");
    writeDocCommentBody(printer, field, options, kdoc);
    writeDebugString(printer, field, options, kdoc);
    if (!kdoc && !isPrivate)
    {
      writeDeprecatedJavadoc(printer, field, options);
    }

    String name = underscoresToCamelCase(field.getName(), false);

    switch (type)
    {
    case OR_BUILDER_GETTER:
    case OR_BUILDER_LIST_GETTER:
    case OR_BUILDER_INDEXED_GETTER:
      break;
    case HAZZER:
      printer.emit(Map.of("name", name), " * @return Whether the $name$ field is set.\n");
      break;
    case GETTER:
      printer.emit(Map.of("name", name), " * @return The $name$.\n");
      break;
    case BYTES_GETTER:
      printer.emit(Map.of("name", name), " * @return The bytes for $name$.\n");
      break;
    case SETTER:
            if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) break;
      printer.emit(Map.of("name", name), " * @param value The $name$ to set.\n");
      break;
    case BYTES_SETTER:
            if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) break;
      printer.emit(Map.of("name", name), " * @param value The bytes for $name$ to set.\n");
      break;
    case CLEARER:
      break;
    case LIST_COUNT:
      if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) break;
      printer.emit(Map.of("name", name), " * @return The count of $name$.\n");
      break;
    case LIST_GETTER:
      if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) break;
      printer.emit(Map.of("name", name), " * @return A list containing the $name$.\n");
      break;
    case LIST_INDEXED_GETTER:
      if (field.getType() == FieldDescriptor.Type.MESSAGE || field.getType() == FieldDescriptor.Type.GROUP) break;
      printer.emit(" * @param index The index of the element to return.\n");
      printer.emit(Map.of("name", name), " * @return The $name$ at the given index.\n");
      break;
    case LIST_INDEXED_SETTER:
      printer.emit(" * @param index The index to set the value at.\n");
      printer.emit(Map.of("name", name), " * @param value The $name$ to set.\n");
      break;
    case LIST_ADDER:
      printer.emit(Map.of("name", name), " * @param value The $name$ to add.\n");
      break;
    case LIST_MULTI_ADDER:
      printer.emit(Map.of("name", name), " * @param values The $name$ to add.\n");
      break;
    case MAP_ENTRY_ADDER:
      printer.emit(Map.of("name", name), " * @param key The key of the $name$ to add.\n");
      printer.emit(Map.of("name", name), " * @param value The value of the $name$ to add.\n");
      break;
    case MAP_MULTI_ADDER:
      printer.emit(Map.of("name", name), " * @param values The $name$ to add.\n");
      break;
    case MAP_ENTRY_REMOVER:
      printer.emit(Map.of("name", name), " * @param key The key of the $name$ to remove.\n");
      break;
    }
    if (builder)
    {
            switch (type) {
                case SETTER:
                case BYTES_SETTER:
                case CLEARER:
                case LIST_INDEXED_SETTER:
                case LIST_ADDER:
                case LIST_MULTI_ADDER:
                case MAP_ENTRY_ADDER:
                case MAP_MULTI_ADDER:
                case MAP_ENTRY_REMOVER:
                    if (field.getType() != FieldDescriptor.Type.MESSAGE && field.getType() != FieldDescriptor.Type.GROUP) {
                  printer.emit(" * @return This builder for chaining.\n");
                    }
                    break;
                default:
                    break;
            }
    }
    printer.emit(" */\n");
  }

  public static void writeFieldAccessorDocComment(
      Printer printer,
      FieldDescriptor field,
      AccessorType type,
      Options options,
      boolean builder)
  {
    writeFieldAccessorDocComment(printer, field, type, options, builder, false, false);
  }

  public static void writeFieldAccessorDocComment(
      Printer printer,
      FieldDescriptor field,
      AccessorType type,
      Options options)
  {
    writeFieldAccessorDocComment(printer, field, type, options, false, false, false);
  }

    public static void writeFieldEnumValueAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options) {
        writeFieldEnumValueAccessorDocComment(printer, field, type, options, false);
    }

    public static void writeFieldEnumValueAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options,
            boolean builder)
    {
        writeFieldEnumValueAccessorDocComment(printer, field, type, options, builder, false);
    }

    public static void writeFieldEnumValueAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options,
            boolean builder,
            boolean isPrivate)
    {
        printer.emit("/**\n");
        writeDocCommentBody(printer, field, options, false);
        writeDebugString(printer, field, options, false);
        if (!isPrivate) {
            writeDeprecatedJavadoc(printer, field, options);
        }

        String name = underscoresToCamelCase(field.getName(), false);

        switch (type)
        {
        case GETTER:
            printer.emit(Map.of("name", name),
                    " * @return The enum numeric value on the wire for $name$.\n");
            break;
        case SETTER:
            printer.emit(Map.of("name", name),
                    " * @param value The enum numeric value on the wire for $name$ to set.\n");
            break;
        case LIST_GETTER:
            printer.emit(Map.of("name", name),
                    " * @return A list containing the enum numeric values on the wire for $name$.\n");
            break;
        case LIST_INDEXED_GETTER:
            printer.emit(" * @param index The index of the value to return.\n");
            printer.emit(Map.of("name", name),
                    " * @return The enum numeric value on the wire of $name$ at the given index.\n");
            break;
        case LIST_INDEXED_SETTER:
            printer.emit(" * @param index The index to set the value at.\n");
            printer.emit(Map.of("name", name),
                    " * @param value The enum numeric value on the wire for $name$ to set.\n");
            break;
        case LIST_ADDER:
            printer.emit(Map.of("name", name),
                    " * @param value The enum numeric value on the wire for $name$ to add.\n");
            break;
        case LIST_MULTI_ADDER:
            printer.emit(Map.of("name", name),
                    " * @param values The enum numeric values on the wire for $name$ to add.\n");
            break;
        default:
            break;
        }


        if (builder)
        {
            switch (type) {
                case SETTER:
                case LIST_INDEXED_SETTER:
                case LIST_ADDER:
                case LIST_MULTI_ADDER:
                    printer.emit(" * @return This builder for chaining.\n");
                    break;
                default:
                    break;
            }
        }

        printer.emit(" */\n");
    }

    public static void writeFieldStringBytesAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options) {
        writeFieldStringBytesAccessorDocComment(printer, field, type, options, false);
    }

    public static void writeFieldStringBytesAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options,
            boolean builder)
    {
        writeFieldStringBytesAccessorDocComment(printer, field, type, options, builder, false);
    }

    public static void writeFieldStringBytesAccessorDocComment(
            Printer printer,
            FieldDescriptor field,
            AccessorType type,
            Options options,
            boolean builder,
            boolean isPrivate)
    {
        printer.emit("/**\n");
        writeDocCommentBody(printer, field, options, false);
        writeDebugString(printer, field, options, false);
        if (!isPrivate) {
            writeDeprecatedJavadoc(printer, field, options);
        }

        String name = underscoresToCamelCase(field.getName(), false);

        switch (type)
        {
        case GETTER:
            printer.emit(Map.of("name", name), " * @return The bytes for $name$.\n");
            break;
        case SETTER:
            printer.emit(Map.of("name", name), " * @param value The bytes for $name$ to set.\n");
            break;
        case LIST_GETTER:
            printer.emit(Map.of("name", name), " * @return A list containing the bytes for $name$.\n");
            break;
        case LIST_INDEXED_GETTER:
            printer.emit(" * @param index The index of the value to return.\n");
            printer.emit(Map.of("name", name), " * @return The bytes of the $name$ at the given index.\n");
            break;
        case LIST_INDEXED_SETTER:
            printer.emit(" * @param index The index to set the value at.\n");
            printer.emit(Map.of("name", name), " * @param value The bytes of the $name$ to set.\n");
            break;
        case LIST_ADDER:
            printer.emit(Map.of("name", name), " * @param value The bytes of the $name$ to add.\n");
            break;
        case LIST_MULTI_ADDER:
            printer.emit(Map.of("name", name), " * @param values The bytes of the $name$ to add.\n");
            break;
        default:
            break;
        }


        if (builder)
        {
            switch (type) {
                case SETTER:
                case LIST_INDEXED_SETTER:
                case LIST_ADDER:
                case LIST_MULTI_ADDER:
                    printer.emit(" * @return This builder for chaining.\n");
                    break;
                default:
                    break;
            }
        }

        printer.emit(" */\n");
    }

  // Minimal implementation helper for CamelCase
  private static String underscoresToCamelCase(String input, boolean capNextLetter)
  {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);
      if (c == '_')
      {
        capNextLetter = true;
      }
      else if (capNextLetter)
      {
        result.append(Character.toUpperCase(c));
        capNextLetter = false;
      }
      else
      {
        result.append(c);
      }
    }
    return result.toString();
  }

  public static void writeEnumValueDocComment(Printer printer, EnumValueDescriptor value, Context context)
  {
        printer.emit("/**\n");
    writeDocCommentBody(printer, value, context.getOptions(), false); // Assuming no KDoc support in enum values for now
        printer.emit(Map.of("def", escapeJavadoc(value.getName())), " * <code>$def$ = " + value.getNumber() + ";</code>\n");
    printer.emit(" */\n");
  }
}
