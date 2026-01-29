package com.rubberjam.protobuf.another.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class mirroring {@code google/protobuf/compiler/zip_writer.h}.
 */
public final class ZipWriter
{
  private final ZipOutputStream zipOutput;

  public ZipWriter(OutputStream rawOutput)
  {
    this.zipOutput = new ZipOutputStream(rawOutput);
  }

  public void write(String filename, String contents) throws IOException
  {
    write(filename, contents.getBytes(StandardCharsets.UTF_8));
  }

  public void write(String filename, byte[] contents) throws IOException
  {
    ZipEntry entry = new ZipEntry(filename);
    entry.setMethod(ZipEntry.STORED);
    entry.setSize(contents.length);
    CRC32 crc = new CRC32();
    crc.update(contents);
    entry.setCrc(crc.getValue());

    zipOutput.putNextEntry(entry);
    zipOutput.write(contents);
    zipOutput.closeEntry();
  }

  public void writeDirectory() throws IOException
  {
    zipOutput.finish();
  }
}
