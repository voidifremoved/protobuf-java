// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

/** A utility class for resolving Java class names for protobuf descriptors. */
public final class ClassNameResolver {

  public String getFileClassName(FileDescriptor file, boolean immutable) {
    if (file.getOptions().hasJavaOuterClassname()) {
      return file.getOptions().getJavaOuterClassname();
    }
    String basename;
    int lastSlash = file.getName().lastIndexOf('/');
    if (lastSlash == -1) {
      basename = file.getName();
    } else {
      basename = file.getName().substring(lastSlash + 1);
    }
    return StringUtils.underscoresToCamelCase(basename.replace(".proto", ""), true) + "Proto";
  }

  public String getFileJavaPackage(FileDescriptor file) {
    if (file.getOptions().hasJavaPackage()) {
      return file.getOptions().getJavaPackage();
    }
    return file.getPackage();
  }

  private String getClassName(String nameWithoutPackage, FileDescriptor file, boolean immutable) {
    String result = getFileJavaPackage(file);
    if (!result.isEmpty()) {
      result += ".";
    }
    if (file.getOptions().getJavaMultipleFiles()) {
      result += nameWithoutPackage;
    } else {
      result += getFileClassName(file, immutable);
      result += ".";
      result += nameWithoutPackage.replace(".", "$");
    }
    return result;
  }

  private String classNameWithoutPackage(Descriptor descriptor) {
    String result;
    if (descriptor.getContainingType() != null) {
      result = classNameWithoutPackage(descriptor.getContainingType()) + ".";
    } else {
      result = "";
    }
    return result + descriptor.getName();
  }

  public String getClassName(Descriptor descriptor, boolean immutable) {
    return getClassName(
        classNameWithoutPackage(descriptor), descriptor.getFile(), immutable);
  }

  public String getClassName(FileDescriptor descriptor, boolean immutable) {
    String result = getFileJavaPackage(descriptor);
    if (!result.isEmpty()) {
      result += ".";
    }
    result += getFileClassName(descriptor, immutable);
    return result;
  }

  public boolean hasConflictingClassName(FileDescriptor file, String classname) {
    for (EnumDescriptor enumType : file.getEnumTypes()) {
      if (enumType.getName().equals(classname)) {
        return true;
      }
    }
    for (ServiceDescriptor service : file.getServices()) {
      if (service.getName().equals(classname)) {
        return true;
      }
    }
    for (Descriptor messageType : file.getMessageTypes()) {
      if (messageHasConflictingClassName(messageType, classname)) {
        return true;
      }
    }
    return false;
  }

  private boolean messageHasConflictingClassName(Descriptor message, String classname) {
    if (message.getName().equals(classname)) {
      return true;
    }
    for (Descriptor nestedType : message.getNestedTypes()) {
      if (messageHasConflictingClassName(nestedType, classname)) {
        return true;
      }
    }
    for (EnumDescriptor enumType : message.getEnumTypes()) {
      if (enumType.getName().equals(classname)) {
        return true;
      }
    }
    return false;
  }
}
