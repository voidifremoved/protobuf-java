package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class mirroring {@code google/protobuf/compiler/subprocess.h}.
 */
public final class Subprocess
{
  public enum SearchMode
  {
    SEARCH_PATH,
    EXACT_NAME
  }

  private Process process;

  public void start(String program, SearchMode searchMode) throws IOException
  {
    List<String> command = new ArrayList<>();
    // Note: Java's ProcessBuilder searches PATH by default.
    // EXACT_NAME is not easily supported in a cross-platform way without
    // checking file existence manually, but typically plugins are passed by path.
    command.add(program);

    ProcessBuilder builder = new ProcessBuilder(command);
    process = builder.start();
  }

  public boolean communicate(Message input, Message.Builder output, StringBuilder error)
  {
    if (process == null)
    {
      throw new IllegalStateException("Must call start() first.");
    }

    final ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
    final StringBuilder stderrBuilder = new StringBuilder();

    Thread stdinThread = new Thread(() -> {
      try (OutputStream stdin = process.getOutputStream())
      {
        input.writeTo(stdin);
      }
      catch (IOException e)
      {
        // Child closed pipe or other error.
      }
    });

    Thread stdoutThread = new Thread(() -> {
      try (InputStream stdout = process.getInputStream())
      {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stdout.read(buffer)) != -1)
        {
          stdoutBuffer.write(buffer, 0, read);
        }
      }
      catch (IOException e)
      {
        // Ignore
      }
    });

    Thread stderrThread = new Thread(() -> {
      try (InputStream stderr = process.getErrorStream())
      {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stderr.read(buffer)) != -1)
        {
          stderrBuilder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
      }
      catch (IOException e)
      {
        // Ignore
      }
    });

    stdinThread.start();
    stdoutThread.start();
    stderrThread.start();

    try
    {
      stdinThread.join();
      stdoutThread.join();
      stderrThread.join();

      int exitCode = process.waitFor();

      if (stderrBuilder.length() > 0)
      {
         error.append(stderrBuilder.toString());
      }

      if (exitCode != 0)
      {
        error.append("Plugin failed with status code ").append(exitCode).append(".");
        return false;
      }

      try
      {
        output.mergeFrom(stdoutBuffer.toByteArray());
      }
      catch (IOException e)
      {
        error.append("Plugin output is unparseable: ").append(e.getMessage());
        return false;
      }

      return true;
    }
    catch (InterruptedException e)
    {
      error.append("Interrupted: ").append(e.getMessage());
      return false;
    }
  }
}
