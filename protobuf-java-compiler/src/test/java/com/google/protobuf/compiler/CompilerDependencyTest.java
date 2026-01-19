package com.google.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import com.rubberjam.protobuf.compiler.CompilationException;
import com.rubberjam.protobuf.compiler.Compiler;

public class CompilerDependencyTest {

  @Test
  public void testCompileWithMissingDependencyProvidedByResolver() throws Exception {
    String addressProto =
        "syntax = \"proto3\";\n"
            + "package com.example;\n"
            + "message Address {\n"
            + "  string street = 1;\n"
            + "}\n";

    String personProto =
        "syntax = \"proto3\";\n"
            + "package com.example;\n"
            + "import \"address.proto\";\n"
            + "message Person {\n"
            + "  string name = 1;\n"
            + "  Address address = 2;\n"
            + "}\n";

    Map<String, String> protoFiles = new HashMap<>();
    protoFiles.put("person.proto", personProto);

    Compiler compiler = new Compiler();
    Compiler.ProtoImportResolver resolver = (path) -> {
        if ("address.proto".equals(path)) {
            return addressProto;
        }
        return null;
    };

    Map<String, String> files = compiler.compile(protoFiles, Collections.singletonList("java"), resolver);

    // Should contain Person.java or PersonOuterClass.java or PersonProto.java
    String personFileName = "com/example/Person.java";
    if (!files.containsKey(personFileName)) {
        if (files.containsKey("com/example/PersonOuterClass.java")) {
            personFileName = "com/example/PersonOuterClass.java";
        } else if (files.containsKey("com/example/PersonProto.java")) {
            personFileName = "com/example/PersonProto.java";
        } else {
            System.err.println("Files generated: " + files.keySet());
        }
    }
    assertTrue("Could not find generated file for Person", files.containsKey(personFileName));

    // Should NOT contain Address.java because it was not in the initial protoFileContents map
    // (It was loaded as a dependency)
    // Wait, my logic in Compiler says: "Only generate code for the requested files"
    // "requested files" are keys of protoFileContents.
    String addressFileName = "com/example/Address.java";
    if (files.containsKey(addressFileName)) {
        // This is acceptable behavior if we decide dependencies should be generated too?
        // But typically we only generate for the input files.
        // Let's enforce that dependencies are NOT generated unless requested.
        throw new RuntimeException("Should not generate code for implicit dependency address.proto");
    }
  }

  @Test
  public void testCompileCircularDependency() throws Exception {
    String aProto =
        "syntax = \"proto3\";\n"
            + "import \"b.proto\";\n"
            + "message A {}\n";
    String bProto =
        "syntax = \"proto3\";\n"
            + "import \"a.proto\";\n"
            + "message B {}\n";

    Map<String, String> protoFiles = new HashMap<>();
    protoFiles.put("a.proto", aProto);
    protoFiles.put("b.proto", bProto);

    Compiler compiler = new Compiler();
    try {
        compiler.compile(protoFiles, Collections.singletonList("java"));
        // Should throw exception
        throw new RuntimeException("Expected circular dependency exception");
    } catch (CompilationException e) {
        assertTrue(e.getMessage().contains("Circular dependency"));
    }
  }
}
