package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for launching sub-processes, mirroring {@code google/protobuf/compiler/subprocess.h}.
 */
public final class Subprocess {

  public enum SearchMode {
    SEARCH_PATH,
    EXACT_NAME
  }

  private Process process;
  private String program;
  private SearchMode searchMode;

  public Subprocess() {}

  public void start(String program, SearchMode searchMode) {
      this.program = program;
      this.searchMode = searchMode;
  }

  public boolean communicate(Message input, Message.Builder output, StringBuilder error) {
      List<String> command = new ArrayList<>();
      command.add(program);

      ProcessBuilder builder = new ProcessBuilder(command);
      try {
          process = builder.start();

          // Write input to stdin
          try (OutputStream stdin = process.getOutputStream()) {
              input.writeTo(stdin);
          }

          // Read output from stdout
          try (InputStream stdout = process.getInputStream()) {
              output.mergeFrom(stdout);
          }

          // Read errors from stderr
          try (InputStream stderr = process.getErrorStream()) {
              ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
              byte[] buffer = new byte[1024];
              int length;
              while ((length = stderr.read(buffer)) != -1) {
                  errorBuffer.write(buffer, 0, length);
              }
              if (errorBuffer.size() > 0) {
                  error.append(errorBuffer.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
              }
          }

          int exitCode = process.waitFor();
          if (exitCode != 0) {
              error.append("Plugin failed with exit code ").append(exitCode);
              return false;
          }

          return true;

      } catch (Exception e) {
          error.append("Failed to execute subprocess: ").append(e.getMessage());
          return false;
      }
  }
}
