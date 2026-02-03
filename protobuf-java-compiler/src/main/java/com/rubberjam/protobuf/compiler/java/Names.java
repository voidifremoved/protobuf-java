package com.rubberjam.protobuf.compiler.java;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

/**
 * Utilities for mapping descriptors to their Java names.
 * Ported from names.cc/names.h.
 */
public final class Names {

  private Names() {}

  // Set of Java reserved words.
  //
  private static final Set<String> RESERVED_NAMES = new HashSet<>(Arrays.asList(
      "abstract", "assert", "boolean", "break", "byte",
      "case", "catch", "char", "class", "const",
      "continue", "default", "do", "double", "else",
      "enum", "extends", "false", "final", "finally",
      "float", "for", "goto", "if", "implements",
      "import", "instanceof", "int", "interface", "java",
      "long", "native", "new", "null", "package",
      "private", "protected", "public", "return", "short",
      "static", "strictfp", "super", "switch", "synchronized",
      "this", "throw", "throws", "transient", "true",
      "try", "void", "volatile", "while"
  ));

  // Names forbidden for field generation to avoid collision with base class methods.
  //
  private static final Set<String> FORBIDDEN_NAMES = new HashSet<>(Arrays.asList(
      "Class",
      "DefaultInstanceForType",
      "ParserForType",
      "SerializedSize",
      "AllFields",
      "DescriptorForType",
      "InitializationErrorString",
      "UnknownFields",
      "CachedSize"
  ));

  /**
   * Checks if a string is a Java reserved word.
   */
  public static boolean isReservedName(String name) {
    return RESERVED_NAMES.contains(name);
  }

  /**
   * Checks if a field name (in UpperCamelCase) is forbidden.
   */
  public static boolean isForbidden(String fieldName) {
    return FORBIDDEN_NAMES.contains(underscoresToCamelCase(fieldName, true));
  }

  // --- Class Name Resolution ---

  public static String qualifiedClassName(Descriptor descriptor) {
    return new ClassNameResolver().getClassName(descriptor, true);
  }

  public static String qualifiedClassName(EnumDescriptor descriptor) {
    return new ClassNameResolver().getClassName(descriptor, true);
  }

  public static String qualifiedClassName(ServiceDescriptor descriptor) {
    return new ClassNameResolver().getClassName(descriptor, true);
  }

  public static String qualifiedClassName(FileDescriptor descriptor) {
    return new ClassNameResolver().getClassName(descriptor, true);
  }

  // --- File/Package Naming ---

  /**
   * Gets the Java package name for the file.
   *
   */
  public static String fileJavaPackage(FileDescriptor file) {
    // Logic from DefaultJavaPackage in names_internal.h
    if (file.getOptions().hasJavaPackage()) {
      return file.getOptions().getJavaPackage();
    } else {
      String packagePrefix = isOss() ? "" : "com.google.protos";
      if (packagePrefix.isEmpty()) {
        return file.getPackage();
      } else if (file.getPackage().isEmpty()) {
        return packagePrefix;
      } else {
        return packagePrefix + "." + file.getPackage();
      }
    }
  }

  /**
   * Converts a Java package name to a directory structure.
   */
  public static String javaPackageDirectory(FileDescriptor file) {
    String packageName = fileJavaPackage(file);
    String dir = packageName.replace('.', '/');
    if (!dir.isEmpty()) {
      dir += "/";
    }
    return dir;
  }

  /**
   * Gets the outer class name for the file.
   */
  public static String fileClassName(FileDescriptor file) {
    return new ClassNameResolver().getFileClassName(file, true);
  }

  // --- Field Naming ---

  /**
   * Gets the raw field name logic (handling groups and forbidden names).
   *
   */
  private static String fieldName(FieldDescriptor field) {
    String name;
    if (field.getType() == FieldDescriptor.Type.GROUP) {
      name = field.getMessageType().getName();
    } else {
      name = field.getName();
    }
    
    if (isForbidden(name)) {
      return name + "#";
    }
    return name;
  }

  public static String capitalizedFieldName(FieldDescriptor field) {
    return underscoresToCamelCase(fieldName(field), true);
  }

  public static String capitalizedOneofName(OneofDescriptor oneof) {
    return underscoresToCamelCase(oneof.getName(), true);
  }

  public static String underscoresToCamelCase(FieldDescriptor field) {
    return underscoresToCamelCase(fieldName(field), false);
  }

  public static String underscoresToCapitalizedCamelCase(FieldDescriptor field) {
    return underscoresToCamelCase(fieldName(field), true);
  }

  public static String underscoresToCamelCase(MethodDescriptor method) {
    return underscoresToCamelCase(method.getName(), false);
  }

  public static String underscoresToCamelCaseCheckReserved(FieldDescriptor field) {
    String name = underscoresToCamelCase(field);
    if (isReservedName(name)) {
      return name + "_";
    }
    return name;
  }

  /**
   * Converts a name to CamelCase.
   *
   */
  public static String underscoresToCamelCase(String input, boolean capNextLetter) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if ('a' <= c && c <= 'z') {
        if (capNextLetter) {
          result.append((char) (c + ('A' - 'a')));
        } else {
          result.append(c);
        }
        capNextLetter = false;
      } else if ('A' <= c && c <= 'Z') {
        if (i == 0 && !capNextLetter) {
          // Force first letter to lower case unless explicitly capitalized
          result.append((char) (c + ('a' - 'A')));
        } else {
          result.append(c);
        }
        capNextLetter = false;
      } else if ('0' <= c && c <= '9') {
        result.append(c);
        capNextLetter = true;
      } else {
        capNextLetter = true;
      }
    }
    
    // Append trailing # if it exists (forbidden name marker)
    if (input.endsWith("#")) {
      result.append("#");
    }
    
    return result.toString();
  }

  // --- Kotlin Support ---

  public static String kotlinFactoryName(Descriptor descriptor) {
    return new ClassNameResolver().getKotlinFactoryName(descriptor);
  }

  public static String fullyQualifiedKotlinFactoryName(Descriptor descriptor) {
    return new ClassNameResolver().getFullyQualifiedKotlinFactoryName(descriptor);
  }

  public static String kotlinExtensionsClassName(Descriptor descriptor) {
    // This requires the full KotlinExtensionsClassName logic from ClassNameResolver
    // which was partially stubbed in previous steps. 
    // We assume ClassNameResolver has this method.
    // For this standalone class, we'll delegate.
    return new ClassNameResolver().getKotlinExtensionsClassName(descriptor); 
  }

  // Helper to determine OSS mode. In C++ this is google::protobuf::internal::IsOss().
  private static boolean isOss() {
    return true; // Defaulting to OSS behavior for this port.
  }
}