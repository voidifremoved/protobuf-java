package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.compiler.java.FileGenerator;
import com.google.protobuf.compiler.java.Options;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class JavaCodeGeneratorImpl implements JavaCodeGenerator {
  @Override
  public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
      throws CodeGenerator.GenerationException {
    Options fileOptions = new Options();

    for (String option : parameter.split(",")) {
      if (option.equals("output_list_file")) {
        fileOptions.outputListFile = "";
      } else if (option.equals("immutable")) {
        fileOptions.generateImmutableCode = true;
      } else if (option.equals("mutable")) {
        fileOptions.generateMutableCode = true;
      } else if (option.equals("shared")) {
        fileOptions.generateSharedCode = true;
      } else if (option.equals("lite")) {
        fileOptions.enforceLite = true;
      } else if (option.equals("annotate_code")) {
        fileOptions.annotateCode = true;
      } else if (option.equals("annotation_list_file")) {
        fileOptions.annotationListFile = "";
      } else if (option.equals("experimental_strip_nonfunctional_codegen")) {
        fileOptions.stripNonfunctionalCodegen = true;
      } else if (option.equals("bootstrap")) {
        fileOptions.bootstrap = true;
      }
    }

    if (fileOptions.enforceLite && fileOptions.generateMutableCode) {
      throw new CodeGenerator.GenerationException(
          "lite runtime generator option cannot be used with mutable API.");
    }

    if (!fileOptions.generateImmutableCode
        && !fileOptions.generateMutableCode
        && !fileOptions.generateSharedCode) {
      fileOptions.generateImmutableCode = true;
      fileOptions.generateSharedCode = true;
    }

    List<String> allFiles = new ArrayList<>();
    List<String> allAnnotations = new ArrayList<>();

    List<FileGenerator> fileGenerators = new ArrayList<>();
    if (fileOptions.generateImmutableCode) {
      fileGenerators.add(new FileGenerator(file, fileOptions));
    }

    for (FileGenerator fileGenerator : fileGenerators) {
      String error = fileGenerator.validate();
      if (error != null) {
        throw new CodeGenerator.GenerationException(error);
      }
    }

    try {
      for (FileGenerator fileGenerator : fileGenerators) {
        String packageDir = fileGenerator.getJavaPackage().replace('.', '/');
        String javaFilename = packageDir + "/" + fileGenerator.getClassname() + ".java";
        allFiles.add(javaFilename);
        String infoFullPath = javaFilename + ".pb.meta";
        if (fileOptions.annotateCode) {
          allAnnotations.add(infoFullPath);
        }

        StringWriter writer = new StringWriter();
        fileGenerator.generate(new PrintWriter(writer));
        generatorContext.open(javaFilename).write(writer.toString().getBytes());
      }

      if (fileOptions.outputListFile != null) {
        StringWriter writer = new StringWriter();
        for (String fileName : allFiles) {
          writer.write(fileName + "\n");
        }
        generatorContext.open(fileOptions.outputListFile).write(writer.toString().getBytes());
      }

      if (fileOptions.annotationListFile != null) {
        StringWriter writer = new StringWriter();
        for (String fileName : allAnnotations) {
          writer.write(fileName + "\n");
        }
        generatorContext.open(fileOptions.annotationListFile).write(writer.toString().getBytes());
      }
    } catch (java.io.IOException e) {
      throw new CodeGenerator.GenerationException(e.getMessage());
    }
  }
}
