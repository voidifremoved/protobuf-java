package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.compiler.PluginProtos.Version;

/**
 * Utility class for version handling, mirroring {@code google/protobuf/compiler/versions.h}.
 */
public final class Versions {

  private Versions() {}

  // Defines compiler version strings for Protobuf code generators.
  public static final String PROTOBUF_CPP_VERSION_STRING = "6.34.0-dev";
  public static final String PROTOBUF_JAVA_VERSION_STRING = "4.34.0-dev";
  public static final String PROTOBUF_PYTHON_VERSION_STRING = "6.34.0-dev";
  public static final String PROTOBUF_RUST_VERSION_STRING = "4.34.0-dev";

  /**
   * Parses a version string into a Version message.
   */
  public static Version parseProtobufVersion(String versionString) {
    if (versionString == null || versionString.isEmpty()) {
      return Version.getDefaultInstance();
    }

    Version.Builder version = Version.newBuilder();
    String[] parts = versionString.split("-", 2);
    String versionNumber = parts[0];
    String suffix = parts.length > 1 ? parts[1] : "";

    if (!suffix.isEmpty()) {
      version.setSuffix(suffix);
    }

    String[] numberParts = versionNumber.split("\\.");
    try {
      if (numberParts.length > 0) {
        version.setMajor(Integer.parseInt(numberParts[0]));
      }
      if (numberParts.length > 1) {
        version.setMinor(Integer.parseInt(numberParts[1]));
      }
      if (numberParts.length > 2) {
        version.setPatch(Integer.parseInt(numberParts[2]));
      }
    } catch (NumberFormatException e) {
      // If parsing fails, we return whatever we managed to parse so far or defaults.
    }

    return version.build();
  }

  public static Version getProtobufCPPVersion(boolean ossRuntime) {
    return parseProtobufVersion(PROTOBUF_CPP_VERSION_STRING);
  }

  public static Version getProtobufJavaVersion(boolean ossRuntime) {
    return parseProtobufVersion(PROTOBUF_JAVA_VERSION_STRING);
  }

  public static Version getProtobufPythonVersion(boolean ossRuntime) {
    return parseProtobufVersion(PROTOBUF_PYTHON_VERSION_STRING);
  }
}
