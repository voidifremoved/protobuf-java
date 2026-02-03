package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.Names;

@RunWith(JUnit4.class)
public class NamesTest {

  @Test
  public void testReservedNames() {
    assertTrue(Names.isReservedName("class"));
    assertTrue(Names.isReservedName("int"));
    assertFalse(Names.isReservedName("Class"));
    assertFalse(Names.isReservedName("foo"));
  }

  @Test
  public void testUnderscoresToCamelCase() {
    assertEquals("FooBar", Names.underscoresToCamelCase("foo_bar", true));
    assertEquals("fooBar", Names.underscoresToCamelCase("foo_bar", false));
    assertEquals("FooBar", Names.underscoresToCamelCase("FooBar", true));
    assertEquals("fooBar", Names.underscoresToCamelCase("FooBar", false));
    assertEquals("Foo123Bar", Names.underscoresToCamelCase("foo_123_bar", true));
    
    // Test logic from C++ implementation where digits trigger next cap
    assertEquals("Foo123Bar", Names.underscoresToCamelCase("foo123bar", true)); 
  }

  @Test
  public void testForbiddenNames() {
    // "Class" is forbidden
    assertTrue(Names.isForbidden("class")); 
    // "InitializationErrorString" is forbidden
    assertTrue(Names.isForbidden("initialization_error_string"));
    assertFalse(Names.isForbidden("valid_field"));
  }

  @Test
  public void testFileJavaPackage() throws Exception {
    // Create a dummy FileDescriptor
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("my.pkg")
        .build();
    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});

    // Should return package directly in OSS mode
    assertEquals("my.pkg", Names.fileJavaPackage(file));
    assertEquals("my/pkg/", Names.javaPackageDirectory(file));
  }

  @Test
  public void testFileJavaPackageOverride() throws Exception {
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("my.pkg")
        .setOptions(com.google.protobuf.DescriptorProtos.FileOptions.newBuilder()
            .setJavaPackage("com.example.overridden"))
        .build();
    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});

    assertEquals("com.example.overridden", Names.fileJavaPackage(file));
    assertEquals("com/example/overridden/", Names.javaPackageDirectory(file));
  }
}