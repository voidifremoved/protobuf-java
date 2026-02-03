package com.rubberjam.protobuf.compiler;

import com.google.protobuf.compiler.PluginProtos.Version;

/**
 * Defines compiler version strings for Protobuf code generators.
 * Mirrors {@code google/protobuf/compiler/versions.h} and {@code versions.cc}.
 */
public final class Versions
{
  public static final String PROTOBUF_CPP_VERSION_STRING = "6.34.0-dev";
  public static final String PROTOBUF_JAVA_VERSION_STRING = "4.33.4";
  public static final String PROTOBUF_PYTHON_VERSION_STRING = "6.34.0-dev";
  public static final String PROTOBUF_RUST_VERSION_STRING = "4.34.0-dev";

  private Versions() {}

  /**
   * Parses the Protobuf language version strings.
   */
  public static Version parseProtobufVersion(String version)
  {
    if (version == null || version.isEmpty())
    {
      throw new IllegalArgumentException("version cannot be empty.");
    }
    Version.Builder result = Version.newBuilder();
    String[] parts = version.split("-");
    if (parts.length > 2)
    {
      throw new IllegalArgumentException("version cannot have more than one suffix annotated by \"-\".");
    }
    if (parts.length == 2)
    {
      result.setSuffix("-" + parts[1]);
    }

    String[] numberParts = parts[0].split("\\.");
    if (numberParts.length != 3)
    {
      throw new IllegalArgumentException("version string must provide major, minor and micro numbers.");
    }

    result.setMajor(Integer.parseInt(numberParts[0]));
    result.setMinor(Integer.parseInt(numberParts[1]));
    result.setPatch(Integer.parseInt(numberParts[2]));

    return result.build();
  }

  public static Version getProtobufCPPVersion(boolean ossRuntime)
  {
    return parseProtobufVersion(PROTOBUF_CPP_VERSION_STRING);
  }

  public static Version getProtobufJavaVersion(boolean ossRuntime)
  {
    return parseProtobufVersion(PROTOBUF_JAVA_VERSION_STRING);
  }

  public static Version getProtobufPythonVersion(boolean ossRuntime)
  {
    return parseProtobufVersion(PROTOBUF_PYTHON_VERSION_STRING);
  }
}
