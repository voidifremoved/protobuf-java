package com.rubberjam.protobuf.compiler.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.rubberjam.protobuf.compiler.java.FieldCommon.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.FieldCommon.OneofGeneratorInfo;

/**
 * A context object holds the information that is shared among all code
 * generators. Ported from context.h/context.cc.
 */
public class Context {

  private final ClassNameResolver nameResolver;
  private final Map<FieldDescriptor, FieldGeneratorInfo> fieldGeneratorInfoMap = new HashMap<>();
  private final Map<OneofDescriptor, OneofGeneratorInfo> oneofGeneratorInfoMap = new HashMap<>();
  private final Options options;

  public Context(FileDescriptor file, Options options) {
    this.nameResolver = new ClassNameResolver();
    this.options = options;
    initializeFieldGeneratorInfo(file);
  }

  public ClassNameResolver getNameResolver() {
    return nameResolver;
  }

  public FieldGeneratorInfo getFieldGeneratorInfo(FieldDescriptor field) {
    FieldGeneratorInfo info = fieldGeneratorInfoMap.get(field);
    if (info == null) {
      throw new IllegalArgumentException("Can not find FieldGeneratorInfo for field: " + field.getFullName());
    }
    return info;
  }

  public OneofGeneratorInfo getOneofGeneratorInfo(OneofDescriptor oneof) {
    OneofGeneratorInfo info = oneofGeneratorInfoMap.get(oneof);
    if (info == null) {
      throw new IllegalArgumentException("Can not find OneofGeneratorInfo for oneof: " + oneof.getName());
    }
    return info;
  }

  public Options getOptions() {
    return options;
  }

  public boolean enforceLite() {
    return options.isEnforceLite();
  }

  public boolean hasGeneratedMethods(Descriptor descriptor) {
    return hasGeneratedMethods(descriptor.getFile());
  }

  public boolean hasGeneratedMethods(FileDescriptor file) {
    return !options.isEnforceLite();
  }

  public boolean hasGeneratedMethods(com.google.protobuf.Descriptors.EnumDescriptor descriptor) {
    return hasGeneratedMethods(descriptor.getFile());
  }

  public boolean hasGeneratedMethods(com.google.protobuf.Descriptors.ServiceDescriptor descriptor) {
    return hasGeneratedMethods(descriptor.getFile());
  }

  private void initializeFieldGeneratorInfo(FileDescriptor file) {
    for (Descriptor message : file.getMessageTypes()) {
      initializeFieldGeneratorInfoForMessage(message);
    }
  }

  private void initializeFieldGeneratorInfoForMessage(Descriptor message) {
    for (Descriptor nested : message.getNestedTypes()) {
      initializeFieldGeneratorInfoForMessage(nested);
    }

    initializeFieldGeneratorInfoForFields(message.getFields());

    for (OneofDescriptor oneof : message.getOneofs()) {
      OneofGeneratorInfo info = new OneofGeneratorInfo(
          Names.underscoresToCamelCase(oneof.getName(), false),
          Names.underscoresToCamelCase(oneof.getName(), true)
      );
      oneofGeneratorInfoMap.put(oneof, info);
    }
  }

  private void initializeFieldGeneratorInfoForFields(List<FieldDescriptor> fields) {
    boolean[] isConflict = new boolean[fields.size()];
    String[] conflictReason = new String[fields.size()];

    for (int i = 0; i < fields.size(); ++i) {
      FieldDescriptor field = fields.get(i);
      String name = Names.capitalizedFieldName(field);
      for (int j = i + 1; j < fields.size(); ++j) {
        FieldDescriptor other = fields.get(j);
        String otherName = Names.capitalizedFieldName(other);
        if (name.equals(otherName)) {
          isConflict[i] = isConflict[j] = true;
          conflictReason[i] = conflictReason[j] =
              "capitalized name of field \"" + field.getName() +
              "\" conflicts with field \"" + other.getName() + "\"";
        } else if (isConflicting(field, name, other, otherName, conflictReason, j)) {
          isConflict[i] = isConflict[j] = true;
          // conflictReason[j] is set by isConflicting
          conflictReason[i] = conflictReason[j];
        }
      }
      if (isConflict[i]) {
        System.err.println("WARNING: field \"" + field.getFullName() +
                           "\" is conflicting with another field: " + conflictReason[i]);
      }
    }

    for (int i = 0; i < fields.size(); ++i) {
      FieldDescriptor field = fields.get(i);
      String name = Helpers.camelCaseFieldName(field);
      String capitalizedName = Names.capitalizedFieldName(field);

      String disambiguatedReason = null;
      if (isConflict[i]) {
        name += field.getNumber();
        capitalizedName += field.getNumber();
        disambiguatedReason = conflictReason[i];
      }

      FieldGeneratorInfo info = new FieldGeneratorInfo(name, capitalizedName, disambiguatedReason, options);
      fieldGeneratorInfoMap.put(field, info);
    }
  }

  // Helper for checking suffix equality
  private boolean equalWithSuffix(String name1, String suffix, String name2) {
    if (name2.endsWith(suffix)) {
      String prefix = name2.substring(0, name2.length() - suffix.length());
      return name1.equals(prefix);
    }
    return false;
  }

  private boolean isRepeatedFieldConflicting(FieldDescriptor field1, String name1,
                                             FieldDescriptor field2, String name2,
                                             String[] info, int index) {
    if (field1.isRepeated() && !field2.isRepeated()) {
      if (equalWithSuffix(name1, "Count", name2)) {
        info[index] = "both repeated field \"" + field1.getName() +
                      "\" and singular field \"" + field2.getName() +
                      "\" generate the method \"get" + name1 + "Count()\"";
        return true;
      }
      if (equalWithSuffix(name1, "List", name2)) {
        info[index] = "both repeated field \"" + field1.getName() +
                      "\" and singular field \"" + field2.getName() +
                      "\" generate the method \"get" + name1 + "List()\"";
        return true;
      }
    }
    return false;
  }

  private boolean isEnumFieldConflicting(FieldDescriptor field1, String name1,
                                         FieldDescriptor field2, String name2,
                                         String[] info, int index) {
    if (field1.getType() == FieldDescriptor.Type.ENUM &&
        InternalHelpers.supportUnknownEnumValue(field1) &&
        equalWithSuffix(name1, "Value", name2)) {
      info[index] = "both enum field \"" + field1.getName() +
                    "\" and regular field \"" + field2.getName() +
                    "\" generate the method \"get" + name1 + "Value()\"";
      return true;
    }
    return false;
  }

  private boolean isConflictingOneWay(FieldDescriptor field1, String name1,
                                      FieldDescriptor field2, String name2,
                                      String[] info, int index) {
    return isRepeatedFieldConflicting(field1, name1, field2, name2, info, index) ||
           isEnumFieldConflicting(field1, name1, field2, name2, info, index);
  }

  private boolean isConflicting(FieldDescriptor field1, String name1,
                                FieldDescriptor field2, String name2,
                                String[] info, int index) {
    return isConflictingOneWay(field1, name1, field2, name2, info, index) ||
           isConflictingOneWay(field2, name2, field1, name1, info, index);
  }
}
