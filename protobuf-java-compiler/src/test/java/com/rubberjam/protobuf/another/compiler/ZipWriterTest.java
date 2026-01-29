package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;

public class ZipWriterTest
{
  @Test
  public void testWrite() throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ZipWriter writer = new ZipWriter(out);

    writer.write("file1.txt", "content1");
    writer.write("dir/file2.txt", "content2".getBytes(StandardCharsets.UTF_8));
    writer.writeDirectory();

    byte[] zipData = out.toByteArray();
    assertTrue(zipData.length > 0);

    // Verify using ZipInputStream
    try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipData)))
    {
      ZipEntry entry1 = zin.getNextEntry();
      assertNotNull(entry1);
      assertEquals("file1.txt", entry1.getName());
      assertEquals("content1", new String(readAllBytes(zin), StandardCharsets.UTF_8));

      ZipEntry entry2 = zin.getNextEntry();
      assertNotNull(entry2);
      assertEquals("dir/file2.txt", entry2.getName());
      assertEquals("content2", new String(readAllBytes(zin), StandardCharsets.UTF_8));
    }
  }

  private byte[] readAllBytes(ZipInputStream zin) throws IOException
  {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[1024];
    while ((nRead = zin.read(data, 0, data.length)) != -1)
    {
      buffer.write(data, 0, nRead);
    }
    return buffer.toByteArray();
  }
}
