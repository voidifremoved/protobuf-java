package com.google.protobuf;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.rubberjam.protobuf.compiler.ErrorCollector;
import com.rubberjam.protobuf.compiler.Parser;
import com.rubberjam.protobuf.compiler.SourceLocationTable;
import com.rubberjam.protobuf.compiler.Tokenizer;

import junit.framework.TestCase;

public class ParserTest extends TestCase {

  // A test implementation of ErrorCollector that records errors in a list.
  private static class TestErrorCollector implements ErrorCollector {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void recordError(int line, int column, String message) {
      errors.add(line + ":" + column + ": " + message);
    }

    public int getErrorCount() {
      return errors.size();
    }

    public String getErrors() {
      return String.join("\n", errors);
    }
  }

  public void testParseOneof() throws Exception {
    String proto =
        "message TestMessage {\n"
            + "  oneof test_oneof {\n"
            + "    string name = 1;\n"
            + "    int32 sub_id = 2;\n"
            + "  }\n"
            + "}";

    TestErrorCollector errorCollector = new TestErrorCollector();
    SourceLocationTable sourceLocationTable = new SourceLocationTable();
    Tokenizer tokenizer = new Tokenizer(new StringReader(proto), errorCollector);
    Parser parser = new Parser(errorCollector, sourceLocationTable);
    DescriptorProtos.FileDescriptorProto.Builder fileBuilder =
        DescriptorProtos.FileDescriptorProto.newBuilder();

    boolean result = parser.parse(tokenizer, fileBuilder);

    assertTrue("Parsing failed with errors: " + errorCollector.getErrors(), result);
    assertEquals(
        "Expected 0 errors, but got: " + errorCollector.getErrors(),
        0,
        errorCollector.getErrorCount());

    DescriptorProtos.FileDescriptorProto file = fileBuilder.build();
    assertEquals(1, file.getMessageTypeCount());

    DescriptorProtos.DescriptorProto message = file.getMessageType(0);
    assertEquals("TestMessage", message.getName());
    assertEquals(1, message.getOneofDeclCount());
    assertEquals("test_oneof", message.getOneofDecl(0).getName());
    assertEquals(2, message.getFieldCount());

    DescriptorProtos.FieldDescriptorProto field1 = message.getField(0);
    assertEquals("name", field1.getName());
    assertEquals(0, field1.getOneofIndex());

    DescriptorProtos.FieldDescriptorProto field2 = message.getField(1);
    assertEquals("sub_id", field2.getName());
    assertEquals(0, field2.getOneofIndex());
  }
}
