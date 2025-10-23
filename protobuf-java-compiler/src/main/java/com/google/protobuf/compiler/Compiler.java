package com.google.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An API for compiling .proto files. */
public final class Compiler {
  private static final class InMemoryGeneratorContext implements GeneratorContext {
    private final Map<String, ByteArrayOutputStream> files = new HashMap<>();

    @Override
    public OutputStream open(String filename) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      files.put(filename, stream);
      return stream;
    }

    public Map<String, String> getFiles() {
      Map<String, String> result = new HashMap<>();
      for (Map.Entry<String, ByteArrayOutputStream> entry : files.entrySet()) {
        result.put(entry.getKey(), entry.getValue().toString());
      }
      return Collections.unmodifiableMap(result);
    }
  }

  public Map<String, String> compile(
      Map<String, String> protoFileContents, List<String> languages)
      throws CompilationException {
    InMemoryGeneratorContext context = new InMemoryGeneratorContext();
    Map<String, FileDescriptorProto> fileDescriptorProtos = new HashMap<>();

    for (Map.Entry<String, String> entry : protoFileContents.entrySet()) {
      String fileName = entry.getKey();
      String fileContent = entry.getValue();

      FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
      fileBuilder.setName(fileName);

      List<String> errors = new ArrayList<>();
      ErrorCollector errorCollector = (line, column, message) -> {
        errors.add(fileName + ":" + line + ":" + column + ": " + message);
      };

      Tokenizer tokenizer = new Tokenizer(new StringReader(fileContent), errorCollector);
      Parser parser = new Parser(errorCollector, new SourceLocationTable());
      if (!parser.parse(tokenizer, fileBuilder) || !errors.isEmpty()) {
        throw new CompilationException("Error parsing proto file: " + String.join("\n", errors));
      }
      fileDescriptorProtos.put(fileName, fileBuilder.build());
    }

    Map<String, FileDescriptor> fileDescriptors = new HashMap<>();
    for (Map.Entry<String, FileDescriptorProto> entry : fileDescriptorProtos.entrySet()) {
      try {
        FileDescriptorProto proto = entry.getValue();
        FileDescriptor[] dependencies = new FileDescriptor[proto.getDependencyCount()];
        for (int i = 0; i < proto.getDependencyCount(); i++) {
          dependencies[i] = fileDescriptors.get(proto.getDependency(i));
        }
        fileDescriptors.put(entry.getKey(), FileDescriptor.buildFrom(proto, dependencies));
      } catch (Descriptors.DescriptorValidationException e) {
        throw new CompilationException("Error building file descriptor", e);
      }
    }

    for (FileDescriptor fileDescriptor : fileDescriptors.values()) {
      for (String language : languages) {
        if (language.equals("java")) {
          JavaCodeGenerator codeGenerator = new JavaCodeGenerator();
          try {
            codeGenerator.generate(fileDescriptor, "", context);
          } catch (CodeGenerator.GenerationException e) {
            throw new CompilationException("Error generating code", e);
          }
        } else {
          throw new CompilationException("Unsupported language: " + language);
        }
      }
    }
    return context.getFiles();
  }
}
