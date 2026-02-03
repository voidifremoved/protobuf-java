package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FileDescriptor;

/**
 * Internal naming helpers. Ported from names_internal.h.
 */
public final class NamesInternal {

  private NamesInternal() {}

  // Joins two package segments into a single package name with a dot separator,
  // unless either of the segments is empty, in which case no separator is added.
  public static String joinPackage(String a, String b) {
    if (a.isEmpty()) {
      return b;
    } else if (b.isEmpty()) {
      return a;
    } else {
      return a + "." + b;
    }
  }

  public static String defaultJavaPackage(FileDescriptor file) {
    if (file.getOptions().hasJavaPackage()) {
      return file.getOptions().getJavaPackage();
    } else {
      String packagePrefix = isOss() ? "" : "com.google.protos";
      return joinPackage(packagePrefix, file.getPackage());
    }
  }

  // The package name to use for a file that is being compiled as proto2-API.
  public static String proto2DefaultJavaPackage(FileDescriptor file) {
    return defaultJavaPackage(file);
  }

  // Converts a Java package name to a directory name.
  public static String packageToDir(String packageName) {
    String packageDir = packageName.replace('.', '/');
    if (!packageDir.isEmpty()) {
      packageDir += "/";
    }
    return packageDir;
  }

  // Helper to determine OSS mode. In C++ this is google::protobuf::internal::IsOss().
  private static boolean isOss() {
    return true; // Defaulting to OSS behavior for this port.
  }
}
