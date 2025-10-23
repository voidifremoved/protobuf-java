package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import java.util.HashMap;
import java.util.Map;

class NameResolver {
  private final Map<String, String> classNames = new HashMap<>();

  String getFileClassName(FileDescriptor file, boolean immutable) {
    String key = file.getFullName() + (immutable ? ":immutable" : ":mutable");
    if (classNames.containsKey(key)) {
      return classNames.get(key);
    }
    String className;
    if (file.getOptions().hasJavaOuterClassname()) {
      className = file.getOptions().getJavaOuterClassname();
    } else {
      className = StringUtils.toUpperCamelCase(file.getName().substring(0, file.getName().lastIndexOf('.')));
    }
    classNames.put(key, className);
    return className;
  }

  boolean hasConflictingClassName(FileDescriptor file, String className, boolean exact) {
    for (Descriptor messageType : file.getMessageTypes()) {
      if (className.equals(messageType.getName())) {
        return true;
      }
    }
    for (EnumDescriptor enumType : file.getEnumTypes()) {
      if (className.equals(enumType.getName())) {
        return true;
      }
    }
    for (ServiceDescriptor service : file.getServices()) {
      if (className.equals(service.getName())) {
        return true;
      }
    }
    return false;
  }
}
