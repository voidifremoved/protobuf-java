package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.CodeGenerator;
import com.rubberjam.protobuf.compiler.GeneratorContext;
import com.rubberjam.protobuf.compiler.java.full.ImmutableGeneratorFactory;
import com.rubberjam.protobuf.compiler.java.lite.ImmutableLiteGeneratorFactory;
import com.rubberjam.protobuf.io.Printer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Generates Java code for a .proto file.
 * Ported from java/generator.h and java/generator.cc.
 */
public class JavaGenerator extends CodeGenerator {

  @Override
  public void generate(FileDescriptor file, String parameter, GeneratorContext context)
      throws GenerationException {

    Options options = new Options();
    parseGeneratorParameter(parameter, options);

    // Initialize context
    Context javaContext = new Context(file, options);
    ClassNameResolver nameResolver = javaContext.getNameResolver();

    // We only support immutable code generation for now as per the conversion order (Phase 8-9).
    // Select the factory based on whether Lite runtime is required.
    GeneratorFactory factory;
    if (javaContext.enforceLite()
        || file.getOptions().getOptimizeFor() == FileOptions.OptimizeMode.LITE_RUNTIME) {
      factory = new ImmutableLiteGeneratorFactory(javaContext);
    } else {
      factory = new ImmutableGeneratorFactory(javaContext);
    }

    try {
      generateFile(file, options, javaContext, nameResolver, factory, context);
    } catch (IOException e) {
      throw new GenerationException(e);
    }
  }

  private void generateFile(FileDescriptor file, Options options, Context javaContext,
      ClassNameResolver nameResolver, GeneratorFactory factory, GeneratorContext outputContext)
      throws IOException {

    String javaPackage = file.getOptions().getJavaPackage();
    if (javaPackage.isEmpty()) {
        javaPackage = NamesInternal.defaultJavaPackage(file);
    }
    String javaPackageDir = NamesInternal.packageToDir(javaPackage);

    // Generate outer class
    String outerClassname = nameResolver.getFileClassName(file, !options.isEnforceLite());
    String outerFilename = javaPackageDir + outerClassname + ".java";

    Printer printer = new Printer(new Printer.Options());
    FileGenerator fileGenerator = new FileGenerator(file, options, javaContext, factory);
    fileGenerator.generate(printer);

    try (OutputStream output = outputContext.open(outerFilename)) {
        output.write(printer.toString().getBytes(StandardCharsets.UTF_8));
    }

    // Generate separate files for top-level messages, enums, and services if java_multiple_files is true
    if (file.getOptions().getJavaMultipleFiles()) {
      for (Descriptor message : file.getMessageTypes()) {
        generateMessage(message, javaContext, nameResolver, factory, outputContext, javaPackage);
      }
      for (EnumDescriptor enumType : file.getEnumTypes()) {
        generateEnum(enumType, javaContext, nameResolver, factory, outputContext, javaPackage);
      }
      for (ServiceDescriptor service : file.getServices()) {
        generateService(service, javaContext, nameResolver, factory, outputContext, javaPackage);
      }
    }
  }

  private void generateMessage(Descriptor message, Context javaContext,
      ClassNameResolver nameResolver, GeneratorFactory factory, GeneratorContext outputContext, String javaPackage)
      throws IOException {

    String className = nameResolver.getClassName(message, !javaContext.enforceLite());
    String filename = className.replace('.', '/') + ".java";

    Printer printer = new Printer(new Printer.Options());
    printPreamble(printer, message.getFile().getName(), javaPackage);
    factory.newMessageGenerator(message).generate(printer);

    try (OutputStream output = outputContext.open(filename)) {
        output.write(printer.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void generateEnum(EnumDescriptor enumType, Context javaContext,
      ClassNameResolver nameResolver, GeneratorFactory factory, GeneratorContext outputContext, String javaPackage)
      throws IOException {

    String className = nameResolver.getClassName(enumType, !javaContext.enforceLite());
    String filename = className.replace('.', '/') + ".java";

    Printer printer = new Printer(new Printer.Options());
    printPreamble(printer, enumType.getFile().getName(), javaPackage);
    factory.newEnumGenerator(enumType).generate(printer);

    try (OutputStream output = outputContext.open(filename)) {
        output.write(printer.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void generateService(ServiceDescriptor service, Context javaContext,
      ClassNameResolver nameResolver, GeneratorFactory factory, GeneratorContext outputContext, String javaPackage)
      throws IOException {

    String className = nameResolver.getClassName(service, !javaContext.enforceLite());
    String filename = className.replace('.', '/') + ".java";

    Printer printer = new Printer(new Printer.Options());
    printPreamble(printer, service.getFile().getName(), javaPackage);
    factory.newServiceGenerator(service).generate(printer);

    try (OutputStream output = outputContext.open(filename)) {
        output.write(printer.toString().getBytes(StandardCharsets.UTF_8));
    }
  }

  private void printPreamble(Printer printer, String sourceInfo, String javaPackage) {
      printer.print(
        "// Generated by the protocol buffer compiler.  DO NOT EDIT!\n" +
        "// source: " + sourceInfo + "\n" +
        "\n");
      if (!javaPackage.isEmpty()) {
        printer.print("package " + javaPackage + ";\n\n");
      }
  }

  private void parseGeneratorParameter(String parameter, Options options) {
    if (parameter == null || parameter.isEmpty()) {
      return;
    }
    String[] parts = parameter.split(",");
    for (String part : parts) {
      if (part.equals("lite")) {
        options.setEnforceLite(true);
      } else if (part.equals("annotate_code")) {
        options.setAnnotateCode(true);
      } else if (part.equals("shared")) {
        options.setGenerateSharedCode(true);
      } else if (part.equals("immutable")) {
        options.setGenerateImmutableCode(true);
      }
      // Add other options as needed
    }
  }
}
