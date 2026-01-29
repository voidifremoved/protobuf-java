package com.rubberjam.protobuf.another.compiler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.google.protobuf.compiler.PluginProtos.Version;

public class VersionsTest {
    @Test
    public void testParseProtobufVersion() {
        Version v = Versions.parseProtobufVersion("3.14.15-rc1");
        assertEquals(3, v.getMajor());
        assertEquals(14, v.getMinor());
        assertEquals(15, v.getPatch());
        assertEquals("rc1", v.getSuffix());
    }
}
