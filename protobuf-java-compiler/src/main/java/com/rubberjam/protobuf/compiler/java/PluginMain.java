package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.rubberjam.protobuf.compiler.GeneratorContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point for the protoc-gen-java plugin.
 */
public class PluginMain {

  public static void main(String[] args) throws IOException {
    CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in);
    CodeGeneratorResponse.Builder responseBuilder = CodeGeneratorResponse.newBuilder();

    try {
      List<FileDescriptor> filesToGenerate = new ArrayList<>();
      Map<String, FileDescriptor> descriptorMap = new HashMap<>();

      // Request contains all reachable files, topologically sorted.
      for (FileDescriptorProto fdp : request.getProtoFileList()) {
        FileDescriptor[] deps = new FileDescriptor[fdp.getDependencyCount()];
        for (int i = 0; i < fdp.getDependencyCount(); i++) {
          deps[i] = descriptorMap.get(fdp.getDependency(i));
          if (deps[i] == null) {
              // Should not happen if request is sorted correctly by protoc
              throw new RuntimeException("Dependency " + fdp.getDependency(i) + " not found for " + fdp.getName());
          }
        }

        FileDescriptor fd = FileDescriptor.buildFrom(fdp, deps);
        descriptorMap.put(fd.getName(), fd);

        if (request.getFileToGenerateList().contains(fd.getName())) {
          filesToGenerate.add(fd);
        }
      }

      JavaGenerator generator = new JavaGenerator();

      for (FileDescriptor file : filesToGenerate) {
          generator.generate(file, request.getParameter(), new PluginGeneratorContext(responseBuilder));
      }

    } catch (Exception e) {
      responseBuilder.setError(e.toString());
    }

    responseBuilder.build().writeTo(System.out);
  }

  private static class PluginGeneratorContext implements GeneratorContext {
      private final CodeGeneratorResponse.Builder responseBuilder;

      public PluginGeneratorContext(CodeGeneratorResponse.Builder responseBuilder) {
          this.responseBuilder = responseBuilder;
      }

      @Override
      public OutputStream open(String filename) throws IOException {
          return new ByteArrayOutputStream() {
              @Override
              public void close() throws IOException {
                  super.close();
                  CodeGeneratorResponse.File.Builder fileBuilder = CodeGeneratorResponse.File.newBuilder();
                  fileBuilder.setName(filename);
                  fileBuilder.setContent(this.toString("UTF-8"));
                  responseBuilder.addFile(fileBuilder);
              }
          };
      }
  }
}
