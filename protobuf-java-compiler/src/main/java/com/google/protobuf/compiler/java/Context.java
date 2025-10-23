// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.DescriptorProtos.FileOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A context object holds the information that is shared among all code
 * generators.
 */
public final class Context {

  private final ClassNameResolver nameResolver;
  private final Options options;
  private final Map<FieldDescriptor, FieldGeneratorInfo> fieldGeneratorInfoMap = new HashMap<>();
  private final Map<OneofDescriptor, OneofGeneratorInfo> oneofGeneratorInfoMap = new HashMap<>();

  public Context(FileDescriptor file, Options options) {
    this.nameResolver = new ClassNameResolver();
    this.options = options;
    initializeFieldGeneratorInfo(file);
  }

  public ClassNameResolver getNameResolver() {
    return nameResolver;
  }

  public Options getOptions() {
    return options;
  }

  public FieldGeneratorInfo getFieldGeneratorInfo(FieldDescriptor field) {
    return fieldGeneratorInfoMap.get(field);
  }

  public OneofGeneratorInfo getOneofGeneratorInfo(OneofDescriptor oneof) {
    return oneofGeneratorInfoMap.get(oneof);
  }

  public boolean enforceLite() {
    return options.enforceLite;
  }

  public boolean hasGeneratedMethods(Descriptor descriptor) {
    return options.enforceLite
        || descriptor.getFile().getOptions().getOptimizeFor() != FileOptions.OptimizeMode.CODE_SIZE;
  }

  private void initializeFieldGeneratorInfo(FileDescriptor file) {
    for (Descriptor messageType : file.getMessageTypes()) {
      initializeFieldGeneratorInfoForMessage(messageType);
    }
  }

  private void initializeFieldGeneratorInfoForMessage(Descriptor message) {
    for (Descriptor nestedType : message.getNestedTypes()) {
      initializeFieldGeneratorInfoForMessage(nestedType);
    }

    List<FieldDescriptor> fields = new ArrayList<>(message.getFields());
    initializeFieldGeneratorInfoForFields(fields);

    for (OneofDescriptor oneof : message.getOneofs()) {
      OneofGeneratorInfo info = new OneofGeneratorInfo();
      info.name = StringUtils.underscoresToCamelCase(oneof.getName(), false);
      info.capitalizedName = StringUtils.underscoresToCamelCase(oneof.getName(), true);
      oneofGeneratorInfoMap.put(oneof, info);
    }
  }

  private static boolean isRepeatedFieldConflicting(
      FieldDescriptor field1, String name1, FieldDescriptor field2, String name2) {
    if (field1.isRepeated() && !field2.isRepeated()) {
      if (name2.equals(name1 + "Count")) {
        return true;
      }
      if (name2.equals(name1 + "List")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isEnumFieldConflicting(
      FieldDescriptor field1, String name1, FieldDescriptor field2, String name2) {
    if (field1.getType() == FieldDescriptor.Type.ENUM
        && field1.getFile().getSyntax() == FileDescriptor.Syntax.PROTO3) {
      if (name2.equals(name1 + "Value")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isConflictingOneWay(
      FieldDescriptor field1, String name1, FieldDescriptor field2, String name2) {
    return isRepeatedFieldConflicting(field1, name1, field2, name2)
        || isEnumFieldConflicting(field1, name1, field2, name2);
  }

  private static boolean isConflicting(
      FieldDescriptor field1, String name1, FieldDescriptor field2, String name2) {
    return isConflictingOneWay(field1, name1, field2, name2)
        || isConflictingOneWay(field2, name2, field1, name1);
  }

  private void initializeFieldGeneratorInfoForFields(List<FieldDescriptor> fields) {
    boolean[] isConflict = new boolean[fields.size()];
    for (int i = 0; i < fields.size(); i++) {
      FieldDescriptor field = fields.get(i);
      String name = StringUtils.capitalizedFieldName(field);
      for (int j = i + 1; j < fields.size(); j++) {
        FieldDescriptor other = fields.get(j);
        String otherName = StringUtils.capitalizedFieldName(other);
        if (name.equals(otherName) || isConflicting(field, name, other, otherName)) {
          isConflict[i] = isConflict[j] = true;
        }
      }
    }

    for (int i = 0; i < fields.size(); i++) {
      FieldDescriptor field = fields.get(i);
      FieldGeneratorInfo info = new FieldGeneratorInfo();
      info.name = StringUtils.camelCaseFieldName(field);
      info.capitalizedName = StringUtils.capitalizedFieldName(field);
      if (isConflict[i]) {
        info.name += field.getNumber();
        info.capitalizedName += field.getNumber();
      }
      info.options = options;
      fieldGeneratorInfoMap.put(field, info);
    }
  }
}
