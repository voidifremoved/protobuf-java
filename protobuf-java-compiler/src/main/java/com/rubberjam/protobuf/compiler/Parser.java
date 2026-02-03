package com.rubberjam.protobuf.compiler;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import com.google.protobuf.DescriptorProtos.UninterpretedOption;
import com.rubberjam.protobuf.io.Tokenizer;
import com.rubberjam.protobuf.io.Tokenizer.Token;

/**
 * Implements parsing of .proto files to FileDescriptorProtos.
 * Ported from parser.cc.
 */
public class Parser {

  private Tokenizer input;
  private SourceCodeInfo.Builder sourceCodeInfo;
  private String syntaxIdentifier = "";
  private boolean hadErrors = false;
  private int recursionDepth = 0;
  
  // Accumulated comments to be attached to the next declaration
  private String upcomingDocComments = "";
  private List<String> upcomingDetachedComments = new ArrayList<>();

  // Helpers for source location tracking
  private static class LocationRecorder {
    private final Parser parser;
    private final SourceCodeInfo.Location.Builder location;

    LocationRecorder(Parser parser) {
      this.parser = parser;
      this.location = parser.sourceCodeInfo.addLocationBuilder();
      recordStart();
    }

    LocationRecorder(LocationRecorder parent, int path1) {
      this.parser = parent.parser;
      this.location = parser.sourceCodeInfo.addLocationBuilder();
      this.location.addAllPath(parent.location.getPathList());
      this.location.addPath(path1);
      recordStart();
    }

    LocationRecorder(LocationRecorder parent, int path1, int path2) {
      this.parser = parent.parser;
      this.location = parser.sourceCodeInfo.addLocationBuilder();
      this.location.addAllPath(parent.location.getPathList());
      this.location.addPath(path1);
      this.location.addPath(path2);
      recordStart();
    }

    private void recordStart() {
      Token current = parser.input.current();
      location.addSpan(current.line);
      location.addSpan(current.column);
    }

    void endAt(Token token) {
      if (token.line != location.getSpan(0)) {
        location.addSpan(token.line);
      }
      location.addSpan(token.endColumn);
    }
    
    // Attaches comments consumed by the parser to this location
    void attachComments(String leading, String trailing, List<String> detached) {
      if (!leading.isEmpty()) location.setLeadingComments(leading);
      if (!trailing.isEmpty()) location.setTrailingComments(trailing);
      if (!detached.isEmpty()) location.addAllLeadingDetachedComments(detached);
    }
  }

  public boolean parse(Tokenizer input, FileDescriptorProto.Builder file) {
    this.input = input;
    this.sourceCodeInfo = SourceCodeInfo.newBuilder();
    this.hadErrors = false;
    this.syntaxIdentifier = "";
    this.recursionDepth = 0;

    // Prime the pump
    input.next(); 

    LocationRecorder rootLocation = new LocationRecorder(this);

    if (lookingAt("syntax")) {
      if (!parseSyntaxIdentifier(rootLocation)) {
        return false;
      }
      if (file != null) file.setSyntax(syntaxIdentifier);
    } else {
      syntaxIdentifier = "proto2";
    }

    while (!atEnd()) {
      if (!parseTopLevelStatement(file, rootLocation)) {
        // Skip statement logic would go here
        input.next(); // Simple recovery
      }
    }

    if (file != null) {
      file.setSourceCodeInfo(sourceCodeInfo.build());
    }
    return !hadErrors;
  }

  // --- Parsing Primitives ---

  private boolean lookingAt(String text) {
    return input.current().text.equals(text);
  }

  private boolean lookingAtType(Tokenizer.TokenType type) {
    return input.current().type == type;
  }

  private boolean atEnd() {
    return input.current().type == Tokenizer.TokenType.END;
  }

  private boolean consume(String text) {
    if (lookingAt(text)) {
      input.next();
      return true;
    }
    recordError("Expected \"" + text + "\".");
    return false;
  }

  private boolean tryConsume(String text) {
    if (lookingAt(text)) {
      input.next();
      return true;
    }
    return false;
  }

  private boolean consumeIdentifier(StringBuilder output) {
    if (lookingAtType(Tokenizer.TokenType.IDENTIFIER)) {
      output.append(input.current().text);
      input.next();
      return true;
    }
    recordError("Expected identifier.");
    return false;
  }

  private boolean consumeInteger(int[] output) {
    if (lookingAtType(Tokenizer.TokenType.INTEGER)) {
      try {
        long val = parseInteger(input.current().text);
        output[0] = (int) val;
        input.next();
        return true;
      } catch (NumberFormatException e) {
        recordError("Invalid integer: " + input.current().text);
        return false;
      }
    }
    recordError("Expected integer.");
    return false;
  }

  private boolean consumeSignedInteger(int[] output) {
      boolean negative = tryConsume("-");
      if (consumeInteger(output)) {
          if (negative) {
              output[0] = -output[0];
          }
          return true;
      }
      return false;
  }

  private boolean consumeString(StringBuilder output) {
    if (lookingAtType(Tokenizer.TokenType.STRING)) {
      while (lookingAtType(Tokenizer.TokenType.STRING)) {
        output.append(input.current().text);
        input.next();
      }
      return true;
    }
    recordError("Expected string.");
    return false;
  }
  
  private boolean consumeNumber(StringBuilder output) {
      boolean negative = tryConsume("-");
      if (lookingAtType(Tokenizer.TokenType.INTEGER) || lookingAtType(Tokenizer.TokenType.FLOAT)) {
          if (negative) output.append("-");
          output.append(input.current().text);
          input.next();
          return true;
      } else if (lookingAt("inf")) {
          if (negative) output.append("-");
          output.append("inf");
          input.next();
          return true;
      } else if (lookingAt("nan")) {
          if (negative) output.append("-");
          output.append("nan");
          input.next();
          return true;
      }
      recordError("Expected number.");
      return false;
  }

  private boolean consumeEndOfDeclaration(String text, LocationRecorder location) {
    if (tryConsume(text)) {
        return true;
    }
    recordError("Expected \"" + text + "\".");
    return false;
  }

  private void recordError(String message) {
    System.err.println(input.current().line + ":" + input.current().column + ": " + message);
    hadErrors = true;
  }

  // --- Grammar Rules ---

  private boolean parseSyntaxIdentifier(LocationRecorder parent) {
    consume("syntax");
    consume("=");
    StringBuilder syntax = new StringBuilder();
    if (!consumeString(syntax)) return false;
    
    // Clean quotes if Tokenizer leaves them
    String cleanSyntax = syntax.toString().replace("\"", "").replace("'", "");
    
    if (!cleanSyntax.equals("proto2") && !cleanSyntax.equals("proto3")) {
      recordError("Unrecognized syntax identifier \"" + cleanSyntax + "\".");
      return false;
    }
    syntaxIdentifier = cleanSyntax;
    consume(";");
    return true;
  }

  private boolean parseTopLevelStatement(FileDescriptorProto.Builder file, LocationRecorder rootLocation) {
    if (lookingAt("message")) {
      LocationRecorder location = new LocationRecorder(rootLocation, FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER, file.getMessageTypeCount());
      return parseMessageDefinition(file.addMessageTypeBuilder(), location);
    } else if (lookingAt("enum")) {
      LocationRecorder location = new LocationRecorder(rootLocation, FileDescriptorProto.ENUM_TYPE_FIELD_NUMBER, file.getEnumTypeCount());
      return parseEnumDefinition(file.addEnumTypeBuilder(), location);
    } else if (lookingAt("service")) {
        LocationRecorder location = new LocationRecorder(rootLocation, FileDescriptorProto.SERVICE_FIELD_NUMBER, file.getServiceCount());
        return parseServiceDefinition(file.addServiceBuilder(), location);
    } else if (lookingAt("package")) {
      return parsePackage(file, rootLocation);
    } else if (lookingAt("import")) {
      return parseImport(file, rootLocation);
    } else if (lookingAt("extend")) {
        return parseFileExtend(file, rootLocation);
    } else if (lookingAt("message")) {
        input.next();
        return false;
    } else if (lookingAt("option")) {
      return parseOption(file.getOptionsBuilder(), rootLocation);
    } else if (lookingAt(";")) {
        input.next();
        return true;
    } else {
      recordError("Unexpected token: " + input.current().text);
      return false;
    }
  }

  private boolean parseImport(FileDescriptorProto.Builder file, LocationRecorder root) {
      LocationRecorder location = new LocationRecorder(root, FileDescriptorProto.DEPENDENCY_FIELD_NUMBER, file.getDependencyCount());
      consume("import");

      if (tryConsume("public")) {
          int index = file.getDependencyCount();
          file.addPublicDependency(index);
      } else if (tryConsume("weak")) {
          int index = file.getDependencyCount();
          file.addWeakDependency(index);
      }

      StringBuilder importPath = new StringBuilder();
      if (!consumeString(importPath)) return false;
      file.addDependency(importPath.toString());

      consumeEndOfDeclaration(";", location);
      return true;
  }

  private boolean parseFileExtend(FileDescriptorProto.Builder file, LocationRecorder root) {
      consume("extend");
      StringBuilder extendee = new StringBuilder();
      if (!parseType(extendee)) return false;

      consume("{");
      while (!lookingAt("}")) {
          if (atEnd()) return false;
          LocationRecorder location = new LocationRecorder(root, FileDescriptorProto.EXTENSION_FIELD_NUMBER, file.getExtensionCount());
          FieldDescriptorProto.Builder extension = file.addExtensionBuilder();
          extension.setExtendee(extendee.toString());
          if (!parseMessageField(extension, location, null, file, -1)) {
              input.next();
          }
      }
      consume("}");
      return true;
  }

  private boolean parsePackage(FileDescriptorProto.Builder file, LocationRecorder root) {
    LocationRecorder location = new LocationRecorder(root, FileDescriptorProto.PACKAGE_FIELD_NUMBER);
    consume("package");
    StringBuilder pkg = new StringBuilder();
    while (true) {
      if (!consumeIdentifier(pkg)) return false;
      if (!tryConsume(".")) break;
      pkg.append(".");
    }
    file.setPackage(pkg.toString());
    consumeEndOfDeclaration(";", location);
    return true;
  }

  private boolean parseMessageDefinition(DescriptorProto.Builder message, LocationRecorder messageLocation) {
    consume("message");
    StringBuilder name = new StringBuilder();
    if (!consumeIdentifier(name)) return false;
    message.setName(name.toString());

    consume("{");
    while (!lookingAt("}")) {
      if (atEnd()) {
        recordError("Unexpected end of file in message definition.");
        return false;
      }
      if (!parseMessageStatement(message, messageLocation)) {
          input.next();
      }
    }
    consume("}");
    return true;
  }

  private boolean parseMessageStatement(DescriptorProto.Builder message, LocationRecorder messageLocation) {
    if (lookingAt("message")) {
      LocationRecorder location = new LocationRecorder(messageLocation, DescriptorProto.NESTED_TYPE_FIELD_NUMBER, message.getNestedTypeCount());
      return parseMessageDefinition(message.addNestedTypeBuilder(), location);
    } else if (lookingAt("enum")) {
      LocationRecorder location = new LocationRecorder(messageLocation, DescriptorProto.ENUM_TYPE_FIELD_NUMBER, message.getEnumTypeCount());
      return parseEnumDefinition(message.addEnumTypeBuilder(), location);
    } else if (lookingAt("extensions")) {
      return parseExtensions(message, messageLocation);
    } else if (lookingAt("reserved")) {
      return parseReserved(message, messageLocation);
    } else if (lookingAt("extend")) {
      return parseNestedExtend(message, messageLocation);
    } else if (lookingAt("oneof")) {
      return parseOneof(message, messageLocation);
    } else if (lookingAt("option")) {
        return parseOption(message.getOptionsBuilder(), messageLocation);
    } else if (lookingAt("map")) {
        LocationRecorder location = new LocationRecorder(messageLocation, DescriptorProto.FIELD_FIELD_NUMBER, message.getFieldCount());
        return parseMapField(message, location);
    } else {
      LocationRecorder location = new LocationRecorder(messageLocation, DescriptorProto.FIELD_FIELD_NUMBER, message.getFieldCount());
      return parseMessageField(message.addFieldBuilder(), location, message, null, -1);
    }
  }

  private boolean parseMapField(DescriptorProto.Builder message, LocationRecorder location) {
      consume("map");
      consume("<");
      StringBuilder keyType = new StringBuilder();
      if (!parseType(keyType)) return false;
      consume(",");
      StringBuilder valueType = new StringBuilder();
      if (!parseType(valueType)) return false;
      consume(">");

      StringBuilder name = new StringBuilder();
      consumeIdentifier(name);

      consume("=");
      int[] number = new int[1];
      consumeInteger(number);

      // Create synthetic entry message
      String entryName = capitalized(name.toString()) + "Entry";
      DescriptorProto.Builder entry = message.addNestedTypeBuilder();
      entry.setName(entryName);
      entry.getOptionsBuilder().setMapEntry(true);

      FieldDescriptorProto.Builder keyField = entry.addFieldBuilder();
      keyField.setName("key");
      keyField.setNumber(1);
      keyField.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
      setFieldType(keyField, keyType.toString());

      FieldDescriptorProto.Builder valueField = entry.addFieldBuilder();
      valueField.setName("value");
      valueField.setNumber(2);
      valueField.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
      setFieldType(valueField, valueType.toString());

      // Add field to message
      FieldDescriptorProto.Builder field = message.addFieldBuilder();
      field.setName(name.toString());
      field.setNumber(number[0]);
      field.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
      field.setType(FieldDescriptorProto.Type.TYPE_MESSAGE);
      field.setTypeName(entryName);

      if (lookingAt("[")) {
          parseFieldOptions(field);
      }

      consume(";");
      return true;
  }

  private void setFieldType(FieldDescriptorProto.Builder field, String type) {
      if (isPrimitiveType(type)) {
          field.setType(FieldDescriptorProto.Type.valueOf("TYPE_" + type.toUpperCase()));
      } else {
          field.setTypeName(type);
          field.setType(FieldDescriptorProto.Type.TYPE_MESSAGE);
      }
  }

  private String capitalized(String s) {
      if (s.isEmpty()) return s;
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private boolean parseOneof(DescriptorProto.Builder message, LocationRecorder messageLocation) {
      consume("oneof");
      StringBuilder name = new StringBuilder();
      consumeIdentifier(name);

      OneofDescriptorProto.Builder oneof = message.addOneofDeclBuilder();
      oneof.setName(name.toString());
      int oneofIndex = message.getOneofDeclCount() - 1;

      consume("{");
      while (!lookingAt("}")) {
          if (lookingAt("option")) {
              parseOption(oneof.getOptionsBuilder(), messageLocation);
          } else {
              LocationRecorder location = new LocationRecorder(messageLocation, DescriptorProto.FIELD_FIELD_NUMBER, message.getFieldCount());
              FieldDescriptorProto.Builder field = message.addFieldBuilder();
              if (!parseMessageField(field, location, message, null, oneofIndex)) {
                  input.next();
              }
          }
      }
      consume("}");
      return true;
  }

  private boolean parseExtensions(DescriptorProto.Builder message, LocationRecorder parent) {
      consume("extensions");
      do {
          DescriptorProto.ExtensionRange.Builder range = message.addExtensionRangeBuilder();
          int[] start = new int[1];
          if (!consumeInteger(start)) return false;
          range.setStart(start[0]);

          if (tryConsume("to")) {
              if (tryConsume("max")) {
                  range.setEnd(536870912); // 2^29
              } else {
                  int[] end = new int[1];
                  if (!consumeInteger(end)) return false;
                  range.setEnd(end[0] + 1); // exclusive
              }
          } else {
              range.setEnd(start[0] + 1);
          }
      } while (tryConsume(","));
      consume(";");
      return true;
  }

  private boolean parseReserved(DescriptorProto.Builder message, LocationRecorder parent) {
      consume("reserved");
      if (lookingAtType(Tokenizer.TokenType.STRING)) {
          // Reserved names
           do {
              StringBuilder name = new StringBuilder();
              consumeString(name);
              message.addReservedName(name.toString());
           } while (tryConsume(","));
      } else {
          // Reserved ranges
          do {
              DescriptorProto.ReservedRange.Builder range = message.addReservedRangeBuilder();
              int[] start = new int[1];
              if (!consumeInteger(start)) return false;
              range.setStart(start[0]);

              if (tryConsume("to")) {
                  if (tryConsume("max")) {
                      range.setEnd(536870912);
                  } else {
                      int[] end = new int[1];
                      if (!consumeInteger(end)) return false;
                      range.setEnd(end[0] + 1);
                  }
              } else {
                  range.setEnd(start[0] + 1);
              }
          } while (tryConsume(","));
      }
      consume(";");
      return true;
  }

  private boolean parseNestedExtend(DescriptorProto.Builder message, LocationRecorder root) {
      consume("extend");
      StringBuilder extendee = new StringBuilder();
      if (!parseType(extendee)) return false;

      consume("{");
      while (!lookingAt("}")) {
          if (atEnd()) return false;
          LocationRecorder location = new LocationRecorder(root, DescriptorProto.EXTENSION_FIELD_NUMBER, message.getExtensionCount());
          FieldDescriptorProto.Builder extension = message.addExtensionBuilder();
          extension.setExtendee(extendee.toString());
          if (!parseMessageField(extension, location, message, null, -1)) {
              input.next();
          }
      }
      consume("}");
      return true;
  }

  private boolean parseType(StringBuilder type) {
      if (lookingAtType(Tokenizer.TokenType.IDENTIFIER)) {
          type.append(input.current().text);
          input.next();
          while (tryConsume(".")) {
              type.append(".");
              StringBuilder part = new StringBuilder();
              if(consumeIdentifier(part)) type.append(part);
          }
          return true;
      }
      return false;
  }

  private boolean parseMessageField(FieldDescriptorProto.Builder field, LocationRecorder fieldLocation, DescriptorProto.Builder containingMessage, FileDescriptorProto.Builder containingFile, int oneofIndex) {
    if (tryConsume("optional")) {
      field.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
    } else if (tryConsume("required")) {
      field.setLabel(FieldDescriptorProto.Label.LABEL_REQUIRED);
    } else if (tryConsume("repeated")) {
      field.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
    } else {
      if (syntaxIdentifier.equals("proto3") && oneofIndex == -1) {
          field.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
      }
    }

    if (tryConsume("group")) {
        StringBuilder name = new StringBuilder();
        consumeIdentifier(name);
        field.setType(FieldDescriptorProto.Type.TYPE_GROUP);
        field.setTypeName(name.toString());
        field.setName(name.toString().toLowerCase());

        consume("=");
        int[] number = new int[1];
        consumeInteger(number);
        field.setNumber(number[0]);

        if (lookingAt("[")) {
            parseFieldOptions(field);
        }

        if (oneofIndex != -1) {
            field.setOneofIndex(oneofIndex);
        }

        consume("{");

        DescriptorProto.Builder nestedType;
        if (containingMessage != null) {
            nestedType = containingMessage.addNestedTypeBuilder();
        } else if (containingFile != null) {
            nestedType = containingFile.addMessageTypeBuilder();
        } else {
            recordError("Group not allowed here.");
            return false;
        }
        nestedType.setName(name.toString());

        while (!lookingAt("}")) {
             if (atEnd()) {
                 recordError("Unexpected end of file in group definition.");
                 return false;
             }
             if (!parseMessageStatement(nestedType, fieldLocation)) {
                 input.next();
             }
        }
        consume("}");
        return true;
    }

    StringBuilder type = new StringBuilder();
    if (lookingAtType(Tokenizer.TokenType.IDENTIFIER)) {
        type.append(input.current().text);
        input.next();
        while (tryConsume(".")) {
            type.append(".");
            StringBuilder part = new StringBuilder();
            if(consumeIdentifier(part)) type.append(part);
        }
    }
    
    if (isPrimitiveType(type.toString())) {
        field.setType(FieldDescriptorProto.Type.valueOf("TYPE_" + type.toString().toUpperCase()));
    } else {
        field.setTypeName(type.toString());
    }

    StringBuilder name = new StringBuilder();
    consumeIdentifier(name);
    field.setName(name.toString());

    consume("=");

    int[] number = new int[1];
    consumeInteger(number);
    field.setNumber(number[0]);

    if (lookingAt("[")) {
        parseFieldOptions(field);
    }

    if (oneofIndex != -1) {
        field.setOneofIndex(oneofIndex);
    }

    consume(";");
    return true;
  }

  private boolean parseFieldOptions(FieldDescriptorProto.Builder field) {
      consume("[");
      do {
          if (lookingAt("default")) {
              consume("default");
              consume("=");
              if (lookingAtType(Tokenizer.TokenType.STRING)) {
                  StringBuilder sb = new StringBuilder();
                  consumeString(sb);
                  field.setDefaultValue(escape(sb.toString()));
              } else if (lookingAt("true") || lookingAt("false")) {
                  field.setDefaultValue(input.current().text);
                  input.next();
              } else {
                  StringBuilder sb = new StringBuilder();
                  if (consumeNumber(sb)) {
                      field.setDefaultValue(sb.toString());
                  } else if (lookingAtType(Tokenizer.TokenType.IDENTIFIER)) {
                      field.setDefaultValue(input.current().text);
                      input.next();
                  } else {
                      recordError("Expected default value.");
                  }
              }
          } else {
              parseOptionEntry(field.getOptionsBuilder());
          }
      } while (tryConsume(","));
      consume("]");
      return true;
  }

  private boolean parseEnumDefinition(EnumDescriptorProto.Builder enumType, LocationRecorder enumLocation) {
    consume("enum");
    StringBuilder name = new StringBuilder();
    consumeIdentifier(name);
    enumType.setName(name.toString());

    consume("{");
    while (!lookingAt("}")) {
      if (!parseEnumStatement(enumType, enumLocation)) {
          input.next();
      }
    }
    consume("}");
    return true;
  }

  private boolean parseEnumStatement(EnumDescriptorProto.Builder enumType, LocationRecorder enumLocation) {
      if (lookingAt("option")) return parseOption(enumType.getOptionsBuilder(), enumLocation);
      if (lookingAt(";")) { input.next(); return true; }
      
      LocationRecorder location = new LocationRecorder(enumLocation, EnumDescriptorProto.VALUE_FIELD_NUMBER, enumType.getValueCount());
      EnumValueDescriptorProto.Builder val = enumType.addValueBuilder();
      
      StringBuilder name = new StringBuilder();
      consumeIdentifier(name);
      val.setName(name.toString());
      
      consume("=");
      
      int[] number = new int[1];
      consumeSignedInteger(number);
      val.setNumber(number[0]);
      
      if (lookingAt("[")) {
          consume("[");
          do {
             parseOptionEntry(val.getOptionsBuilder());
          } while (tryConsume(","));
          consume("]");
      }

      consume(";");
      return true;
  }
  
  private boolean parseServiceDefinition(ServiceDescriptorProto.Builder service, LocationRecorder location) {
      consume("service");
      StringBuilder name = new StringBuilder();
      consumeIdentifier(name);
      service.setName(name.toString());
      
      consume("{");
      while(!lookingAt("}")) {
          if (lookingAt("rpc")) {
              parseServiceMethod(service.addMethodBuilder(), location);
          } else if (lookingAt("option")) {
              parseOption(service.getOptionsBuilder(), location);
          } else {
              input.next();
          }
      }
      consume("}");
      return true;
  }
  
  private boolean parseServiceMethod(MethodDescriptorProto.Builder method, LocationRecorder parentLocation) {
      consume("rpc");
      StringBuilder name = new StringBuilder();
      consumeIdentifier(name);
      method.setName(name.toString());
      
      consume("(");
      if (tryConsume("stream")) {
          method.setClientStreaming(true);
      }
      StringBuilder inputType = new StringBuilder();
      parseType(inputType);
      method.setInputType(inputType.toString());
      consume(")");
      
      consume("returns");
      
      consume("(");
      if (tryConsume("stream")) {
          method.setServerStreaming(true);
      }
      StringBuilder outputType = new StringBuilder();
      parseType(outputType);
      method.setOutputType(outputType.toString());
      consume(")");
      
      if (tryConsume("{")) {
          while (!lookingAt("}")) {
              if (lookingAt("option")) {
                  parseOption(method.getOptionsBuilder(), parentLocation);
              } else {
                  input.next();
              }
          }
          consume("}");
      } else {
          consume(";");
      }
      return true;
  }

  private boolean parseOption(com.google.protobuf.GeneratedMessage.Builder<?> optionsBuilder, LocationRecorder location) {
      consume("option");
      if (!parseOptionEntry(optionsBuilder)) return false;
      consume(";");
      return true;
  }

  private boolean parseOptionEntry(com.google.protobuf.GeneratedMessage.Builder<?> optionsBuilder) {
      UninterpretedOption.Builder uninterpreted = UninterpretedOption.newBuilder();

      if (!parseOptionName(uninterpreted)) return false;

      consume("=");

      if (!parseOptionValue(uninterpreted)) return false;

      try {
          java.lang.reflect.Method m = optionsBuilder.getClass().getMethod("addUninterpretedOption", UninterpretedOption.class);
          m.invoke(optionsBuilder, uninterpreted.build());
      } catch (Exception e) {
          recordError("Could not add option: " + e.getMessage());
          return false;
      }
      return true;
  }

  private boolean parseOptionName(UninterpretedOption.Builder uninterpreted) {
      do {
          UninterpretedOption.NamePart.Builder part = uninterpreted.addNameBuilder();
          if (tryConsume("(")) {
              part.setIsExtension(true);
              StringBuilder name = new StringBuilder();
              if (!parseType(name)) return false;
              part.setNamePart(name.toString());
              consume(")");
          } else {
              part.setIsExtension(false);
              StringBuilder name = new StringBuilder();
              consumeIdentifier(name);
              part.setNamePart(name.toString());
          }
      } while (tryConsume("."));
      return true;
  }

  private boolean parseOptionValue(UninterpretedOption.Builder uninterpreted) {
      if (lookingAtType(Tokenizer.TokenType.IDENTIFIER)) {
          uninterpreted.setIdentifierValue(input.current().text);
          input.next();
      } else if (lookingAtType(Tokenizer.TokenType.INTEGER)) {
          try {
             long val = parseInteger(input.current().text);
             uninterpreted.setPositiveIntValue(val);
          } catch (NumberFormatException e) {
             recordError("Invalid integer value: " + input.current().text);
             return false;
          }
          input.next();
      } else if (lookingAt("-")) {
          consume("-");
          if (lookingAtType(Tokenizer.TokenType.INTEGER)) {
              try {
                  long val = parseInteger(input.current().text);
                  uninterpreted.setNegativeIntValue(-val);
              } catch (NumberFormatException e) {
                  recordError("Invalid integer value: -" + input.current().text);
                  return false;
              }
              input.next();
          } else if (lookingAtType(Tokenizer.TokenType.FLOAT)) {
              double val = Double.parseDouble(input.current().text);
              uninterpreted.setDoubleValue(-val);
              input.next();
          } else if (lookingAt("inf")) {
              uninterpreted.setDoubleValue(Double.NEGATIVE_INFINITY);
              input.next();
          } else if (lookingAt("nan")) {
              uninterpreted.setDoubleValue(Double.NaN);
              input.next();
          }
      } else if (lookingAtType(Tokenizer.TokenType.FLOAT)) {
           double val = Double.parseDouble(input.current().text);
           uninterpreted.setDoubleValue(val);
           input.next();
      } else if (lookingAtType(Tokenizer.TokenType.STRING)) {
          StringBuilder sb = new StringBuilder();
          consumeString(sb);
          uninterpreted.setStringValue(com.google.protobuf.ByteString.copyFromUtf8(sb.toString()));
      } else if (lookingAt("{")) {
          consume("{");
          int depth = 1;
          StringBuilder aggregate = new StringBuilder();
          while (depth > 0 && !atEnd()) {
              if (lookingAt("}")) {
                  depth--;
                  if (depth == 0) break;
              } else if (lookingAt("{")) {
                  depth++;
              }
              aggregate.append(input.current().text).append(" ");
              input.next();
          }
          uninterpreted.setAggregateValue(aggregate.toString().trim());
          consume("}");
      } else {
          recordError("Expected option value.");
          return false;
      }
      return true;
  }

  private String escape(String raw) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < raw.length(); i++) {
          char c = raw.charAt(i);
          switch (c) {
              case '\n': sb.append("\\n"); break;
              case '\r': sb.append("\\r"); break;
              case '\t': sb.append("\\t"); break;
              case '\"': sb.append("\\\""); break;
              case '\'': sb.append("\\\'"); break;
              case '\\': sb.append("\\\\"); break;
              default:
                  if (c >= 0x20 && c < 0x7F) {
                      sb.append(c);
                  } else {
                      sb.append(String.format("\\%03o", (int)c));
                  }
          }
      }
      return sb.toString();
  }

  private long parseInteger(String text) throws NumberFormatException {
      if (text.startsWith("0x") || text.startsWith("0X")) {
          return Long.parseUnsignedLong(text.substring(2), 16);
      } else if (text.startsWith("0") && text.length() > 1) {
          return Long.parseUnsignedLong(text.substring(1), 8);
      } else {
          return Long.parseUnsignedLong(text, 10);
      }
  }

  private boolean isPrimitiveType(String type) {
      return type.equals("double") || type.equals("float") || type.equals("int32") || type.equals("int64") ||
             type.equals("uint32") || type.equals("uint64") || type.equals("sint32") || type.equals("sint64") ||
             type.equals("fixed32") || type.equals("fixed64") || type.equals("sfixed32") || type.equals("sfixed64") ||
             type.equals("bool") || type.equals("string") || type.equals("bytes");
  }
}
