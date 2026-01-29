package com.rubberjam.protobuf.another.compiler;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class mirroring {@code google/protobuf/compiler/code_generator_lite.h}.
 */
public final class CodeGeneratorLite
{

  private CodeGeneratorLite()
  {
  }

  /**
   * Parses a set of comma-delimited name/value pairs.
   *
   * @param text the text to parse
   * @param output the list to populate with pairs
   */
  public static void parseGeneratorParameter(
      String text, List<Map.Entry<String, String>> output)
  {
    if (text == null || text.isEmpty())
    {
      return;
    }
    String[] parts = text.split(",");
    for (String part : parts)
    {
      if (part.isEmpty())
      {
        continue;
      }
      int equalsPos = part.indexOf('=');
      if (equalsPos == -1)
      {
        output.add(new AbstractMap.SimpleImmutableEntry<>(part, ""));
      }
      else
      {
        output.add(new AbstractMap.SimpleImmutableEntry<>(
            part.substring(0, equalsPos), part.substring(equalsPos + 1)));
      }
    }
  }

  /**
   * Strips ".proto" or ".protodevel" from the end of a filename.
   */
  public static String stripProto(String filename)
  {
    if (filename.endsWith(".protodevel"))
    {
      return filename.substring(0, filename.length() - ".protodevel".length());
    }
    else if (filename.endsWith(".proto"))
    {
      return filename.substring(0, filename.length() - ".proto".length());
    }
    return filename;
  }

  /**
   * Returns true if the proto path corresponds to a known feature file.
   */
  public static boolean isKnownFeatureProto(String filename)
  {
    return "google/protobuf/cpp_features.proto".equals(filename)
        || "google/protobuf/java_features.proto".equals(filename);
  }

  private static boolean isOss = true;

  public static boolean isOss()
  {
    return isOss;
  }

  // Not strictly needed as per C++ but good for completeness if we want to change it.
  public static void setIsOss(boolean value)
  {
    isOss = value;
  }
}
