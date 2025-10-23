package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaCodeGenerator extends CodeGenerator {
  @Override
  public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
      throws GenerationException {
    try {
      Map<String, String> options = parseGeneratorParameter(parameter);
      String outputListFile = options.get("output_list_file");
      boolean immutable = options.containsKey("immutable");
      boolean mutable = options.containsKey("mutable");

      // By default we generate immutable code.
      if (!immutable && !mutable) {
        immutable = true;
      }

      if (immutable) {
        FileGenerator fileGenerator = new FileGenerator(file, /* immutable= */ true);
        String javaPackage = fileGenerator.getJavaPackage();
        String className = fileGenerator.getClassName();
        String outputFileName = javaPackage.replace('.', '/') + "/" + className + ".java";
        try (java.io.PrintWriter writer =
            new java.io.PrintWriter(generatorContext.open(outputFileName))) {
          fileGenerator.generate(writer);
        }
      }

      if (mutable) {
        FileGenerator fileGenerator = new FileGenerator(file, /* immutable= */ false);
        String javaPackage = fileGenerator.getJavaPackage();
        String className = fileGenerator.getClassName();
        String outputFileName = javaPackage.replace('.', '/') + "/" + className + ".java";
        try (java.io.PrintWriter writer =
            new java.io.PrintWriter(generatorContext.open(outputFileName))) {
          fileGenerator.generate(writer);
        }
      }

    } catch (java.io.IOException e) {
      throw new GenerationException(e);
    }
  }

  private Map<String, String> parseGeneratorParameter(String parameter) {
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

  @Override
  public long getSupportedFeatures() {
    return Feature.FEATURE_PROTO3_OPTIONAL.getValue();
  }

  @Override
  public Edition getMinimumEdition() {
    return Edition.EDITION_PROTO2;
  }

  @Override
  public Edition getMaximumEdition() {
    return Edition.EDITION_2023;
  }
}
