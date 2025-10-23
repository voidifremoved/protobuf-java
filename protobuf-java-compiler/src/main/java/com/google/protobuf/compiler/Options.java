package com.google.protobuf.compiler;

import java.util.HashMap;
import java.util.Map;

class Options {
  String outputListFile;
  boolean generateImmutableCode;
  boolean generateMutableCode;
  boolean generateSharedCode;
  boolean enforceLite;
  boolean annotateCode;
  String annotationListFile;
  boolean stripNonfunctionalCodegen;
  boolean bootstrap;

  static Options fromParameter(String parameter) {
    Options options = new Options();
    Map<String, String> parsedOptions = parseGeneratorParameter(parameter);
    options.outputListFile = parsedOptions.get("output_list_file");
    options.generateImmutableCode = parsedOptions.containsKey("immutable");
    options.generateMutableCode = parsedOptions.containsKey("mutable");
    options.generateSharedCode = parsedOptions.containsKey("shared");
    options.enforceLite = parsedOptions.containsKey("lite");
    options.annotateCode = parsedOptions.containsKey("annotate_code");
    options.annotationListFile = parsedOptions.get("annotation_list_file");
    options.stripNonfunctionalCodegen = parsedOptions.containsKey("experimental_strip_nonfunctional_codegen");
    options.bootstrap = parsedOptions.containsKey("bootstrap");
    return options;
  }

  private static Map<String, String> parseGeneratorParameter(String parameter) {
    Map<String, String> options = new HashMap<>();
    if (parameter != null && !parameter.isEmpty()) {
      String[] parts = parameter.split(",");
      for (String part : parts) {
        String[] keyValue = part.split("=");
        if (keyValue.length == 1) {
          options.put(keyValue[0], "");
        } else if (keyValue.length == 2) {
          options.put(keyValue[0], keyValue[1]);
        }
      }
    }
    return options;
  }
}
