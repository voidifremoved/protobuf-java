package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class mirroring {@code google/protobuf/compiler/plugin.h}.
 */
public final class Plugin
{

  private Plugin() {}

  /**
   * Implements main() for a protoc plugin exposing the given code generator.
   */
  public static void pluginMain(String[] args, CodeGenerator generator) throws IOException
  {
    if (args.length > 0)
    {
      System.err.println("Unknown option: " + args[0]);
      System.exit(1);
    }

    try (InputStream input = System.in;
         OutputStream output = System.out;
         OutputStream errorOutput = System.err)
    {

      CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(input, ExtensionRegistry.newInstance());
      CodeGeneratorResponse response = generateCode(request, generator);
      response.writeTo(output);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Generates code using the given code generator.
   */
  public static CodeGeneratorResponse generateCode(CodeGeneratorRequest request, CodeGenerator generator)
  {
    CodeGeneratorResponse.Builder responseBuilder = CodeGeneratorResponse.newBuilder();
    StringBuilder errorMsg = new StringBuilder();

    try
    {
        // Build feature set defaults
        FeatureSetDefaults defaults = generator.buildFeatureSetDefaults();
        if (defaults != null)
        {
            // In C++, this sets defaults on the pool. In Java, we typically build Descriptors.
            // DescriptorPool in Java is internal or managed via FileDescriptor.buildFrom.
            // We might need to pass these defaults somehow or they are used when building.
            // For now, assuming standard behavior.
        }

        // Build FileDescriptors
        Map<String, FileDescriptor> parsedFiles = new HashMap<>();
        // We need to build files in dependency order.
        // CodeGeneratorRequest contains all transitive dependencies in topological order (usually).

        for (FileDescriptorProto fileProto : request.getProtoFileList())
        {
            try
            {
                List<FileDescriptor> deps = new ArrayList<>();
                for (String depName : fileProto.getDependencyList())
                {
                    if (parsedFiles.containsKey(depName))
                    {
                        deps.add(parsedFiles.get(depName));
                    }
                    else
                    {
                         // Missing dependency? This should not happen if request is complete.
                         // But for well-known types they might be pre-loaded.
                         // For parity with C++ plugin, we might need a SourceCodeInfo lookup or similar.
                    }
                }
                FileDescriptor[] depsArray = deps.toArray(new FileDescriptor[0]);
                FileDescriptor fd = FileDescriptor.buildFrom(fileProto, depsArray);
                parsedFiles.put(fd.getName(), fd);
            }
            catch (DescriptorValidationException e)
            {
                errorMsg.append("Error building descriptor for ").append(fileProto.getName()).append(": ").append(e.getMessage()).append("\n");
                return responseBuilder.setError(errorMsg.toString()).build();
            }
        }

        List<FileDescriptor> filesToGenerate = new ArrayList<>();
        for (String fileName : request.getFileToGenerateList())
        {
            FileDescriptor fd = parsedFiles.get(fileName);
            if (fd == null)
            {
                errorMsg.append("protoc asked plugin to generate a file but did not provide a descriptor for the file: ").append(fileName).append("\n");
                return responseBuilder.setError(errorMsg.toString()).build();
            }
            filesToGenerate.add(fd);
        }

        GeneratorResponseContext context = new GeneratorResponseContext(request.getCompilerVersion(), responseBuilder, new ArrayList<>(parsedFiles.values()));

        try
        {
            generator.generateAll(filesToGenerate, request.getParameter(), context);
        }
        catch (CodeGenerator.GenerationException e)
        {
             errorMsg.append(e.getMessage());
        }

    }
    catch (Exception e)
    {
        errorMsg.append("Internal error: ").append(e.toString());
    }

    if (errorMsg.length() > 0)
    {
        responseBuilder.setError(errorMsg.toString());
    }

    responseBuilder.setSupportedFeatures(generator.getSupportedFeatures());
    // Minimum/Maximum edition not strictly typed in Java CodeGenerator yet?
    // C++ has GetMinimumEdition. Java CodeGenerator interface should have it.

    return responseBuilder.build();
  }

  private static class GeneratorResponseContext implements GeneratorContext
  {
      private final Version compilerVersion;
      private final CodeGeneratorResponse.Builder responseBuilder;
      private final List<FileDescriptor> parsedFiles;

      public GeneratorResponseContext(Version compilerVersion, CodeGeneratorResponse.Builder responseBuilder, List<FileDescriptor> parsedFiles)
      {
          this.compilerVersion = compilerVersion;
          this.responseBuilder = responseBuilder;
          this.parsedFiles = parsedFiles;
      }

      @Override
      public OutputStream open(String filename) throws IOException
      {
          CodeGeneratorResponse.File.Builder fileBuilder = responseBuilder.addFileBuilder();
          fileBuilder.setName(filename);
          return new StringOutputStream(fileBuilder);
      }

      // Additional methods from C++ context not in interface yet?
      // OpenForInsert, OpenForInsertWithGeneratedCodeInfo, etc.
      // If GeneratorContext interface doesn't support them, we can't implement them here yet.
      // But we can check if we should update GeneratorContext interface.
      // For now, adhering to existing GeneratorContext.
  }

  private static class StringOutputStream extends OutputStream
  {
      private final CodeGeneratorResponse.File.Builder builder;
      private final StringBuilder content = new StringBuilder();

      public StringOutputStream(CodeGeneratorResponse.File.Builder builder)
      {
          this.builder = builder;
      }

      @Override
      public void write(int b) throws IOException
      {
          content.append((char) b);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException
      {
          content.append(new String(b, off, len, java.nio.charset.StandardCharsets.UTF_8));
      }

      @Override
      public void close() throws IOException
      {
          builder.setContent(content.toString());
      }
  }
}
