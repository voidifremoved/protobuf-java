package com.rubberjam.protobuf.another.compiler.java;



import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

@RunWith(JUnit4.class)
public class ClassNameResolverTest {

  private static final String PACKAGE_PREFIX = "";

  // Helper to build a FileDescriptor from a String schema.
  // Replicates BuildFileAndPopulatePool from C++ test.
  private FileDescriptor buildFile(String name, String content) throws Exception {
    // In a real environment, we would need to handle dependencies like
    // "third_party/java/protobuf/java_features.proto".
    // For this unit test, we use a basic build process.
    FileDescriptor result = FileDescriptor.buildFrom(
        com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
            .setName(name)
            .setPackage("proto2_unittest")
            // We can manually parse the schema string here using a Parser or 
            // construct the Proto object. Since we don't have a dynamic proto parser 
            // in standard Java runtime without deps, we mock the descriptor 
            // structure that the test expects.
            //
            // **Crucial Note:** The C++ test parses raw .proto strings. 
            // Java's FileDescriptor.buildFrom expects a FileDescriptorProto object, 
            // not a string. To make this test Runnable without a full ProtoParser dependency,
            // we must construct the FileDescriptorProto manually to match the C++ test string semantics.
            .build(), 
        new FileDescriptor[] {});
        
    // Note: Because constructing FileDescriptorProto manually for every test case 
    // is verbose, the implementation below assumes a helper `parseProto` exists 
    // or constructs the specific objects needed for the assertions.
    return result;
  }

  // Since we cannot run a real Proto Parser in this snippet, the test cases below
  // define the FileDescriptor structure programmatically to match the C++ strings.

  @Test
  public void testFileImmutableClassNameDefault() throws Exception {
    // Corresponds to FileImmutableClassNameEdition2024 (roughly)
    // Schema: package proto2_unittest; message TestFileName2024 {} message FooProto {}
    // File: foo.proto
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("TestFileName2024"))
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("FooProto")) // Conflict!
        .build();
    
    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    ClassNameResolver resolver = new ClassNameResolver();
    
    // Default name from "foo.proto" -> "FooProto"
    assertEquals("FooProto", resolver.getFileDefaultImmutableClassName(file));
    
    // Because "FooProto" message exists, it should conflict and append OuterClass.
    assertEquals("FooProtoOuterClass", resolver.getFileImmutableClassName(file));
  }

  @Test
  public void testFileImmutableClassNameOverridden() throws Exception {
    // Corresponds to FileImmutableClassNameDefaultOverriddenEdition2024
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .setOptions(com.google.protobuf.DescriptorProtos.FileOptions.newBuilder()
            .setJavaOuterClassname("BarBuz"))
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("FooProto"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    ClassNameResolver resolver = new ClassNameResolver();

    assertEquals("FooProto", resolver.getFileDefaultImmutableClassName(file));
    assertEquals("BarBuz", resolver.getFileImmutableClassName(file));
  }

  @Test
  public void testSingleFileService() throws Exception {
    // Corresponds to SingleFileServiceEdition2023
    // java_generic_services = true; package proto2_unittest; service FooService
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .addService(com.google.protobuf.DescriptorProtos.ServiceDescriptorProto.newBuilder().setName("FooService"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    ServiceDescriptor service = file.findServiceByName("FooService");
    ClassNameResolver resolver = new ClassNameResolver();

    // Expect: proto2_unittest.FooProto.FooService (Nested in outer class because multiple_files=false)
    // Note: "foo.proto" -> "FooProto"
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooProto.FooService", 
                 resolver.getClassName(service, true));
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooProto$FooService", 
                 resolver.getJavaImmutableClassName(service));
  }

  @Test
  public void testMultipleFilesService() throws Exception {
    // Corresponds to MultipleFilesServiceEdition2023
    // java_multiple_files = true;
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .setOptions(com.google.protobuf.DescriptorProtos.FileOptions.newBuilder().setJavaMultipleFiles(true))
        .addService(com.google.protobuf.DescriptorProtos.ServiceDescriptorProto.newBuilder().setName("FooService"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    ServiceDescriptor service = file.findServiceByName("FooService");
    ClassNameResolver resolver = new ClassNameResolver();

    // Expect: proto2_unittest.FooService (Top level class)
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooService", 
                 resolver.getClassName(service, true));
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooService", 
                 resolver.getJavaImmutableClassName(service));
  }

  @Test
  public void testMultipleFilesMessage() throws Exception {
    // Corresponds to MultipleFilesMessageEdition2023
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .setOptions(com.google.protobuf.DescriptorProtos.FileOptions.newBuilder().setJavaMultipleFiles(true))
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder().setName("FooMessage"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    Descriptor message = file.findMessageTypeByName("FooMessage");
    ClassNameResolver resolver = new ClassNameResolver();

    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooMessage", 
                 resolver.getClassName(message, true));
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooMessage", 
                 resolver.getJavaImmutableClassName(message));
    // Verify Kotlin factory name
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.fooMessage", 
                 resolver.getFullyQualifiedKotlinFactoryName(message));
  }

  @Test
  public void testSingleFileEnum() throws Exception {
    // Corresponds to SingleFileEnumEdition2023
    
    var fileProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("proto2_unittest")
        .addEnumType(com.google.protobuf.DescriptorProtos.EnumDescriptorProto.newBuilder().setName("FooEnum"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{});
    EnumDescriptor enumType = file.findEnumTypeByName("FooEnum");
    ClassNameResolver resolver = new ClassNameResolver();

    // Nested in FooProto
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooProto.FooEnum", 
                 resolver.getClassName(enumType, true));
    assertEquals(PACKAGE_PREFIX + "proto2_unittest.FooProto$FooEnum", 
                 resolver.getJavaImmutableClassName(enumType));
  }
}