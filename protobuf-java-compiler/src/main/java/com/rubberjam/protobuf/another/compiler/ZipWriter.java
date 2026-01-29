package com.rubberjam.protobuf.another.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for writing zip files, mirroring {@code google/protobuf/compiler/zip_writer.h}.
 */
public final class ZipWriter {

  private final ZipOutputStream zipOutputStream;

  public ZipWriter(OutputStream outputStream) {
    this.zipOutputStream = new ZipOutputStream(outputStream);
  }

  public void write(String filename, String contents) throws IOException {
    ZipEntry entry = new ZipEntry(filename);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  public void writeDirectory() throws IOException {
      // No-op in Java ZipOutputStream generally, directories are implied or created as entries ending with /
  }

  public void finish() throws IOException {
      zipOutputStream.finish();
  }
}
