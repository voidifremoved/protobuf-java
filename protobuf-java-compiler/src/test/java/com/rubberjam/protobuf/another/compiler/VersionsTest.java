package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.protobuf.compiler.PluginProtos.Version;
import org.junit.Test;

public class VersionsTest
{
  @Test
  public void testParseProtobufVersion()
  {
    Version v = Versions.parseProtobufVersion("3.21.0");
    assertEquals(3, v.getMajor());
    assertEquals(21, v.getMinor());
    assertEquals(0, v.getPatch());
    assertEquals("", v.getSuffix());

    v = Versions.parseProtobufVersion("4.0.0-rc1");
    assertEquals(4, v.getMajor());
    assertEquals(0, v.getMinor());
    assertEquals(0, v.getPatch());
    assertEquals("-rc1", v.getSuffix());
  }

  @Test
  public void testParseProtobufVersionErrors()
  {
    try {
      Versions.parseProtobufVersion("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      Versions.parseProtobufVersion("1.0-foo-bar");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      Versions.parseProtobufVersion("1.0");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  @Test
  public void testConstants()
  {
    Version javaVersion = Versions.getProtobufJavaVersion(true);
    assertNotNull(javaVersion);
    assertEquals(4, javaVersion.getMajor());
    assertEquals(34, javaVersion.getMinor());
    assertEquals(0, javaVersion.getPatch());
    assertEquals("-dev", javaVersion.getSuffix());
  }
}
