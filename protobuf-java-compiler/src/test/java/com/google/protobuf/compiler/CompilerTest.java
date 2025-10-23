package com.google.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.junit.Test;

public class CompilerTest {
  @Test
  public void testCompile() throws Exception {
    String protoFile =
        "syntax = \"proto3\";\n"
            + "package com.example;\n"
            + "message Person {\n"
            + "  string name = 1;\n"
            + "  int32 id = 2;\n"
            + "}\n";

    Compiler compiler = new Compiler();
    Map<String, String> files =
        compiler.compile(
            Collections.singletonMap("test.proto", protoFile), Collections.singletonList("java"));

    assertEquals(1, files.size());
    String fileName = "com/example/Test.java";
    assertTrue(files.containsKey(fileName));
    String fileContents = files.get(fileName);
    assertTrue(fileContents.contains("public final class Test"));
    assertTrue(fileContents.contains("private java.lang.String name_;"));
  }

  @Test
  public void testCompileWithDependencies() throws Exception {
    String addressProto =
        "syntax = \"proto3\";\n"
            + "package com.example;\n"
            + "message Address {\n"
            + "  string street = 1;\n"
            + "  string city = 2;\n"
            + "}\n";

    String personProto =
        "syntax = \"proto3\";\n"
            + "package com.example;\n"
            + "import \"address.proto\";\n"
            + "message Person {\n"
            + "  string name = 1;\n"
            + "  Address address = 2;\n"
            + "}\n";

    Map<String, String> protoFiles = new java.util.HashMap<>();
    protoFiles.put("address.proto", addressProto);
    protoFiles.put("person.proto", personProto);

    Compiler compiler = new Compiler();
    Map<String, String> files = compiler.compile(protoFiles, Collections.singletonList("java"));

    assertEquals(2, files.size());
    String personFileName = "com/example/Person.java";
    assertTrue(files.containsKey(personFileName));
    String personFileContents = files.get(personFileName);
    assertTrue(personFileContents.contains("public final class Person"));
    assertTrue(personFileContents.contains("private com.example.Address address_;"));
  }

  @Test
  public void testCompileWithOptions() throws Exception {
    String protoFile =
        "option java_package = \"com.rubberjam.gameservices.core.messages\";\n"
            + "option java_outer_classname = \"CoreEnums\";\n"
            + "option optimize_for = SPEED;\n"
            + "message TestMessage {\n"
            + "  optional string query = 1;\n"
            + "}\n";

    Compiler compiler = new Compiler();
    Map<String, String> files =
        compiler.compile(
            Collections.singletonMap("test.proto", protoFile), Collections.singletonList("java"));

    assertEquals(1, files.size());
    String fileName = "com/rubberjam/gameservices/core/messages/CoreEnums.java";
    assertTrue(files.containsKey(fileName));
    String fileContents = files.get(fileName);
    assertTrue(fileContents.contains("public final class CoreEnums"));
  }
}
