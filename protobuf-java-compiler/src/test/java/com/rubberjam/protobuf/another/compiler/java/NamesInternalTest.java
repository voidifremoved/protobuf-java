package com.rubberjam.protobuf.another.compiler.java;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NamesInternalTest {

  @Test
  public void testJoinPackage() {
    assertEquals("a.b", NamesInternal.joinPackage("a", "b"));
    assertEquals("b", NamesInternal.joinPackage("", "b"));
    assertEquals("a", NamesInternal.joinPackage("a", ""));
    assertEquals("", NamesInternal.joinPackage("", ""));
  }

  @Test
  public void testPackageToDir() {
    assertEquals("com/example/", NamesInternal.packageToDir("com.example"));
    assertEquals("", NamesInternal.packageToDir(""));
  }
}
