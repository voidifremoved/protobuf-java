package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZipWriterTest {
    @Test
    public void testZipWriter() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipWriter writer = new ZipWriter(baos);
        writer.write("test.txt", "content");
        writer.finish();
        assertTrue(baos.size() > 0);
    }
}
