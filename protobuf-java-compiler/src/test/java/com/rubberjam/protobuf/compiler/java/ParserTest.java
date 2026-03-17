package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.UninterpretedOption;
import com.rubberjam.protobuf.compiler.ErrorCollector;
import com.rubberjam.protobuf.compiler.Parser;
import com.rubberjam.protobuf.io.Tokenizer;

/**
 * Tests for the proto file Parser.
 * Ported from parser_unittest.cc.
 */
@RunWith(JUnit4.class)
public class ParserTest {

    private static class TestErrorCollector implements ErrorCollector {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void recordError(int line, int column, String message) {
            errors.add(line + ":" + column + ": " + message);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorText() {
            StringBuilder sb = new StringBuilder();
            for (String e : errors) {
                sb.append(e).append("\n");
            }
            return sb.toString();
        }
    }

    private TestErrorCollector lastErrorCollector;

    private FileDescriptorProto parse(String input) {
        lastErrorCollector = new TestErrorCollector();
        Tokenizer tokenizer = new Tokenizer(new StringReader(input), lastErrorCollector);
        Parser parser = new Parser();
        FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();
        if (parser.parse(tokenizer, file)) {
            return file.build();
        }
        return null;
    }

    private FileDescriptorProto parseExpectingSuccess(String input) {
        FileDescriptorProto result = parse(input);
        assertNotNull("Expected parse success but got errors: " +
            (lastErrorCollector != null ? lastErrorCollector.getErrorText() : ""), result);
        return result;
    }

    private void expectParseFailure(String input) {
        FileDescriptorProto result = parse(input);
        assertNull("Expected parse failure but succeeded", result);
    }

    // ===================================================================
    // ParseMessageTest
    // ===================================================================

    @Test(timeout = 10000)
    public void testSimpleMessage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  required int32 foo = 1;\n" +
            "}\n");
        assertEquals(1, file.getMessageTypeCount());
        DescriptorProto msg = file.getMessageType(0);
        assertEquals("TestMessage", msg.getName());
        FieldDescriptorProto field = msg.getField(0);
        assertEquals("foo", field.getName());
        assertEquals(FieldDescriptorProto.Label.LABEL_REQUIRED, field.getLabel());
        assertEquals(FieldDescriptorProto.Type.TYPE_INT32, field.getType());
        assertEquals(1, field.getNumber());
    }

    @Test(timeout = 10000)
    public void testPrimitiveFieldTypes() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional int32    f1 = 1;\n" +
            "  optional int64    f2 = 2;\n" +
            "  optional uint32   f3 = 3;\n" +
            "  optional uint64   f4 = 4;\n" +
            "  optional sint32   f5 = 5;\n" +
            "  optional sint64   f6 = 6;\n" +
            "  optional fixed32  f7 = 7;\n" +
            "  optional fixed64  f8 = 8;\n" +
            "  optional sfixed32 f9 = 9;\n" +
            "  optional sfixed64 f10 = 10;\n" +
            "  optional float    f11 = 11;\n" +
            "  optional double   f12 = 12;\n" +
            "  optional bool     f13 = 13;\n" +
            "  optional string   f14 = 14;\n" +
            "  optional bytes    f15 = 15;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(FieldDescriptorProto.Type.TYPE_INT32, msg.getField(0).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_INT64, msg.getField(1).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_UINT32, msg.getField(2).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_UINT64, msg.getField(3).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_SINT32, msg.getField(4).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_SINT64, msg.getField(5).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_FIXED32, msg.getField(6).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_FIXED64, msg.getField(7).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_SFIXED32, msg.getField(8).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_SFIXED64, msg.getField(9).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_FLOAT, msg.getField(10).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_DOUBLE, msg.getField(11).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_BOOL, msg.getField(12).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_STRING, msg.getField(13).getType());
        assertEquals(FieldDescriptorProto.Type.TYPE_BYTES, msg.getField(14).getType());
    }

    @Test(timeout = 10000)
    public void testFieldDefaults() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional int32  i32 = 1 [default = 1];\n" +
            "  optional int32  i32n = 2 [default = -2];\n" +
            "  optional int64  i64 = 3 [default = 3];\n" +
            "  optional int64  i64n = 4 [default = -4];\n" +
            "  optional double d = 5 [default = 1.5];\n" +
            "  optional double dn = 6 [default = -1.5];\n" +
            "  optional double dinf = 7 [default = inf];\n" +
            "  optional double dnan = 8 [default = nan];\n" +
            "  optional string s = 9 [default = \"hello\"];\n" +
            "  optional string sesc = 10 [default = \"a\\\"b\"];\n" +
            "  optional bool   b = 11 [default = true];\n" +
            "  optional int32 hex = 12 [default = 0x10];\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals("1", msg.getField(0).getDefaultValue());
        assertEquals("-2", msg.getField(1).getDefaultValue());
        assertEquals("3", msg.getField(2).getDefaultValue());
        assertEquals("-4", msg.getField(3).getDefaultValue());
        assertEquals("1.5", msg.getField(4).getDefaultValue());
        assertEquals("-1.5", msg.getField(5).getDefaultValue());
        assertEquals("inf", msg.getField(6).getDefaultValue());
        assertEquals("nan", msg.getField(7).getDefaultValue());
        assertEquals("hello", msg.getField(8).getDefaultValue());
        assertEquals("a\\\"b", msg.getField(9).getDefaultValue());
        assertEquals("true", msg.getField(10).getDefaultValue());
        assertEquals("0x10", msg.getField(11).getDefaultValue());
    }

    @Test(timeout = 10000)
    public void testFieldOptions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional int32 foo = 1 [ctype = CORD, (custom.opt) = 1];\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);

        assertTrue(field.hasOptions());
        assertEquals(2, field.getOptions().getUninterpretedOptionCount());

        UninterpretedOption opt1 = field.getOptions().getUninterpretedOption(0);
        assertEquals("ctype", opt1.getName(0).getNamePart());
        assertFalse(opt1.getName(0).getIsExtension());
        assertEquals("CORD", opt1.getIdentifierValue());

        UninterpretedOption opt2 = field.getOptions().getUninterpretedOption(1);
        assertEquals("custom.opt", opt2.getName(0).getNamePart());
        assertTrue(opt2.getName(0).getIsExtension());
        assertEquals(1, opt2.getPositiveIntValue());
    }

    @Test(timeout = 10000)
    public void testOneof() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  oneof foo {\n" +
            "    int32 a = 1;\n" +
            "    string b = 2;\n" +
            "  }\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals(1, msg.getOneofDeclCount());
        OneofDescriptorProto oneof = msg.getOneofDecl(0);
        assertEquals("foo", oneof.getName());

        assertEquals(2, msg.getFieldCount());
        assertEquals(0, msg.getField(0).getOneofIndex());
        assertEquals(0, msg.getField(1).getOneofIndex());
    }

    @Test(timeout = 10000)
    public void testMultipleOneofs() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  oneof foo {\n" +
            "    int32 a = 1;\n" +
            "    string b = 2;\n" +
            "  }\n" +
            "  oneof bar {\n" +
            "    int32 c = 3;\n" +
            "    string d = 4;\n" +
            "  }\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals(2, msg.getOneofDeclCount());
        assertEquals("foo", msg.getOneofDecl(0).getName());
        assertEquals("bar", msg.getOneofDecl(1).getName());

        assertEquals(4, msg.getFieldCount());
        assertEquals(0, msg.getField(0).getOneofIndex());
        assertEquals(0, msg.getField(1).getOneofIndex());
        assertEquals(1, msg.getField(2).getOneofIndex());
        assertEquals(1, msg.getField(3).getOneofIndex());
    }

    @Test(timeout = 10000)
    public void testMaps() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  map<int32, string> primitive_map = 1;\n" +
            "  map<string, Foo> msg_map = 2;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals(2, msg.getFieldCount());

        FieldDescriptorProto f1 = msg.getField(0);
        assertEquals("primitive_map", f1.getName());
        assertEquals(FieldDescriptorProto.Label.LABEL_REPEATED, f1.getLabel());
        assertEquals(FieldDescriptorProto.Type.TYPE_MESSAGE, f1.getType());

        assertEquals(2, msg.getNestedTypeCount());
        DescriptorProto entry1 = msg.getNestedType(0);
        assertTrue(entry1.getOptions().getMapEntry());
        assertEquals("key", entry1.getField(0).getName());
        assertEquals(FieldDescriptorProto.Type.TYPE_INT32, entry1.getField(0).getType());
        assertEquals("value", entry1.getField(1).getName());
        assertEquals(FieldDescriptorProto.Type.TYPE_STRING, entry1.getField(1).getType());
    }

    @Test(timeout = 10000)
    public void testMapEntryName() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  map<string, int32> string_to_int32 = 1;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(1, msg.getNestedTypeCount());
        assertEquals("StringToInt32Entry", msg.getNestedType(0).getName());
        assertEquals("StringToInt32Entry", msg.getField(0).getTypeName());
    }

    @Test(timeout = 10000)
    public void testGroup() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional group MyGroup = 1 {\n" +
            "    optional int32 a = 1;\n" +
            "  }\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals(1, msg.getFieldCount());
        FieldDescriptorProto field = msg.getField(0);
        assertEquals(FieldDescriptorProto.Type.TYPE_GROUP, field.getType());
        assertEquals("MyGroup", field.getTypeName());
        assertEquals("mygroup", field.getName());

        assertEquals(1, msg.getNestedTypeCount());
        assertEquals("MyGroup", msg.getNestedType(0).getName());
    }

    @Test(timeout = 10000)
    public void testNestedMessage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  message Nested {}\n" +
            "  optional Nested test_nested = 1;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(1, msg.getNestedTypeCount());
        assertEquals("Nested", msg.getNestedType(0).getName());
        assertEquals(1, msg.getFieldCount());
        assertEquals("test_nested", msg.getField(0).getName());
        assertEquals("Nested", msg.getField(0).getTypeName());
    }

    @Test(timeout = 10000)
    public void testNestedEnum() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  enum NestedEnum {\n" +
            "    FOO = 0;\n" +
            "  }\n" +
            "  optional NestedEnum test_enum = 1;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(1, msg.getEnumTypeCount());
        assertEquals("NestedEnum", msg.getEnumType(0).getName());
    }

    @Test(timeout = 10000)
    public void testExtensions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  extensions 100 to 199;\n" +
            "  extensions 200 to max;\n" +
            "}\n" +
            "extend TestMessage {\n" +
            "  optional int32 ext_field = 100;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(2, msg.getExtensionRangeCount());
        assertEquals(100, msg.getExtensionRange(0).getStart());
        assertEquals(200, msg.getExtensionRange(0).getEnd());

        assertEquals(200, msg.getExtensionRange(1).getStart());
        assertEquals(536870912, msg.getExtensionRange(1).getEnd());

        assertEquals(1, file.getExtensionCount());
        FieldDescriptorProto ext = file.getExtension(0);
        assertEquals("TestMessage", ext.getExtendee());
        assertEquals("ext_field", ext.getName());
        assertEquals(100, ext.getNumber());
    }

    @Test(timeout = 10000)
    public void testExtensionsInMessageScope() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  extend Extendee1 { optional int32 foo = 12; }\n" +
            "  extend Extendee2 { repeated TestMessage bar = 22; }\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(2, msg.getExtensionCount());
        assertEquals("foo", msg.getExtension(0).getName());
        assertEquals("Extendee1", msg.getExtension(0).getExtendee());
        assertEquals("bar", msg.getExtension(1).getName());
        assertEquals("Extendee2", msg.getExtension(1).getExtendee());
    }

    @Test(timeout = 10000)
    public void testMultipleExtensionsOneExtendee() {
        FileDescriptorProto file = parseExpectingSuccess(
            "extend Extendee1 {\n" +
            "  optional int32 foo = 12;\n" +
            "  repeated TestMessage bar = 22;\n" +
            "}\n");

        assertEquals(2, file.getExtensionCount());
        assertEquals("foo", file.getExtension(0).getName());
        assertEquals("Extendee1", file.getExtension(0).getExtendee());
        assertEquals("bar", file.getExtension(1).getName());
        assertEquals("Extendee1", file.getExtension(1).getExtendee());
    }

    @Test(timeout = 10000)
    public void testReservedRange() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  reserved 1, 2 to 5, 10 to max;\n" +
            "  reserved \"foo\", \"bar\";\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);

        assertEquals(3, msg.getReservedRangeCount());
        assertEquals(1, msg.getReservedRange(0).getStart());
        assertEquals(2, msg.getReservedRange(0).getEnd());
        assertEquals(2, msg.getReservedRange(1).getStart());
        assertEquals(6, msg.getReservedRange(1).getEnd());
        assertEquals(10, msg.getReservedRange(2).getStart());
        assertEquals(536870912, msg.getReservedRange(2).getEnd());

        assertEquals(2, msg.getReservedNameCount());
        assertEquals("foo", msg.getReservedName(0));
        assertEquals("bar", msg.getReservedName(1));
    }

    @Test(timeout = 10000)
    public void testCompoundExtensionRange() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  extensions 2, 15, 9 to 11, 100 to 199, 3;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(5, msg.getExtensionRangeCount());
        assertEquals(2, msg.getExtensionRange(0).getStart());
        assertEquals(3, msg.getExtensionRange(0).getEnd());
        assertEquals(15, msg.getExtensionRange(1).getStart());
        assertEquals(16, msg.getExtensionRange(1).getEnd());
        assertEquals(9, msg.getExtensionRange(2).getStart());
        assertEquals(12, msg.getExtensionRange(2).getEnd());
        assertEquals(100, msg.getExtensionRange(3).getStart());
        assertEquals(200, msg.getExtensionRange(3).getEnd());
        assertEquals(3, msg.getExtensionRange(4).getStart());
        assertEquals(4, msg.getExtensionRange(4).getEnd());
    }

    @Test(timeout = 10000)
    public void testOptionalLabelProto3() {
        FileDescriptorProto file = parseExpectingSuccess(
            "syntax = \"proto3\";\n" +
            "message TestMessage {\n" +
            "  int32 foo = 1;\n" +
            "}\n");

        assertEquals("proto3", file.getSyntax());
        DescriptorProto msg = file.getMessageType(0);
        assertEquals(1, msg.getFieldCount());
        assertEquals("foo", msg.getField(0).getName());
        assertEquals(FieldDescriptorProto.Label.LABEL_OPTIONAL, msg.getField(0).getLabel());
    }

    // ===================================================================
    // ParseEnumTest
    // ===================================================================

    @Test(timeout = 10000)
    public void testSimpleEnum() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  FOO = 0;\n" +
            "}\n");

        assertEquals(1, file.getEnumTypeCount());
        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals("TestEnum", e.getName());
        assertEquals(1, e.getValueCount());
        assertEquals("FOO", e.getValue(0).getName());
        assertEquals(0, e.getValue(0).getNumber());
    }

    @Test(timeout = 10000)
    public void testEnumValues() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  ZERO = 0;\n" +
            "  NEG = -1;\n" +
            "  HEX = 0x10;\n" +
            "}\n");

        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals("TestEnum", e.getName());

        assertEquals(3, e.getValueCount());
        assertEquals(0, e.getValue(0).getNumber());
        assertEquals(-1, e.getValue(1).getNumber());
        assertEquals(16, e.getValue(2).getNumber());
    }

    @Test(timeout = 10000)
    public void testEnumValueOptions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  FOO = 13;\n" +
            "  BAR = -10 [ (something.text) = 'abc' ];\n" +
            "}\n");

        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals(2, e.getValueCount());
        assertEquals("FOO", e.getValue(0).getName());
        assertEquals(13, e.getValue(0).getNumber());

        assertEquals("BAR", e.getValue(1).getName());
        assertEquals(-10, e.getValue(1).getNumber());
        assertTrue(e.getValue(1).hasOptions());
        assertEquals(1, e.getValue(1).getOptions().getUninterpretedOptionCount());
        UninterpretedOption opt = e.getValue(1).getOptions().getUninterpretedOption(0);
        assertEquals("something.text", opt.getName(0).getNamePart());
        assertTrue(opt.getName(0).getIsExtension());
    }

    @Test(timeout = 10000)
    public void testEnumReservedRange() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  FOO = 0;\n" +
            "  reserved 2, 15, 9 to 11, 3;\n" +
            "}\n");

        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals(4, e.getReservedRangeCount());
        assertEquals(2, e.getReservedRange(0).getStart());
        assertEquals(2, e.getReservedRange(0).getEnd());
        assertEquals(15, e.getReservedRange(1).getStart());
        assertEquals(15, e.getReservedRange(1).getEnd());
        assertEquals(9, e.getReservedRange(2).getStart());
        assertEquals(11, e.getReservedRange(2).getEnd());
        assertEquals(3, e.getReservedRange(3).getStart());
        assertEquals(3, e.getReservedRange(3).getEnd());
    }

    @Test(timeout = 10000)
    public void testEnumReservedNames() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  FOO = 0;\n" +
            "  reserved \"FOO\", \"BAR\";\n" +
            "}\n");

        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals(2, e.getReservedNameCount());
        assertEquals("FOO", e.getReservedName(0));
        assertEquals("BAR", e.getReservedName(1));
    }

    // ===================================================================
    // ParseServiceTest
    // ===================================================================

    @Test(timeout = 10000)
    public void testSimpleService() {
        FileDescriptorProto file = parseExpectingSuccess(
            "service TestService {\n" +
            "  rpc Foo(In) returns (Out);\n" +
            "}\n");

        assertEquals(1, file.getServiceCount());
        ServiceDescriptorProto svc = file.getService(0);
        assertEquals("TestService", svc.getName());
        assertEquals(1, svc.getMethodCount());
        assertEquals("Foo", svc.getMethod(0).getName());
    }

    @Test(timeout = 10000)
    public void testServices() {
        FileDescriptorProto file = parseExpectingSuccess(
            "service TestService {\n" +
            "  rpc Foo(In) returns (Out);\n" +
            "  rpc Bar(stream In) returns (stream Out) { option deprecated = true; }\n" +
            "}\n");

        ServiceDescriptorProto svc = file.getService(0);
        assertEquals("TestService", svc.getName());

        assertEquals(2, svc.getMethodCount());
        MethodDescriptorProto m1 = svc.getMethod(0);
        assertEquals("Foo", m1.getName());
        assertFalse(m1.getClientStreaming());
        assertFalse(m1.getServerStreaming());

        MethodDescriptorProto m2 = svc.getMethod(1);
        assertEquals("Bar", m2.getName());
        assertTrue(m2.getClientStreaming());
        assertTrue(m2.getServerStreaming());
        assertTrue(m2.getOptions().getUninterpretedOptionCount() > 0);
        UninterpretedOption opt = m2.getOptions().getUninterpretedOption(0);
        assertEquals("deprecated", opt.getName(0).getNamePart());
        assertEquals("true", opt.getIdentifierValue());
    }

    // ===================================================================
    // ParseImportTest
    // ===================================================================

    @Test(timeout = 10000)
    public void testImportsAndPackage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "syntax = \"proto3\";\n" +
            "package com.example;\n" +
            "import \"a.proto\";\n" +
            "import public \"b.proto\";\n" +
            "import weak \"c.proto\";\n");

        assertEquals("proto3", file.getSyntax());
        assertEquals("com.example", file.getPackage());

        assertEquals(3, file.getDependencyCount());
        assertEquals("a.proto", file.getDependency(0));
        assertEquals("b.proto", file.getDependency(1));
        assertEquals("c.proto", file.getDependency(2));

        assertEquals(1, file.getPublicDependencyCount());
        assertEquals(1, file.getPublicDependency(0));

        assertEquals(1, file.getWeakDependencyCount());
        assertEquals(2, file.getWeakDependency(0));
    }

    @Test(timeout = 10000)
    public void testMultipleImports() {
        FileDescriptorProto file = parseExpectingSuccess(
            "import \"a.proto\";\n" +
            "import \"b.proto\";\n" +
            "import \"c.proto\";\n");

        assertEquals(3, file.getDependencyCount());
        assertEquals("a.proto", file.getDependency(0));
        assertEquals("b.proto", file.getDependency(1));
        assertEquals("c.proto", file.getDependency(2));
    }

    // ===================================================================
    // ParseMiscTest
    // ===================================================================

    @Test(timeout = 10000)
    public void testPackage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "package foo.bar.baz;\n");

        assertEquals("foo.bar.baz", file.getPackage());
    }

    @Test(timeout = 10000)
    public void testFileOptions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "option java_package = \"com.example.foo\";\n" +
            "option optimize_for = SPEED;\n");

        assertTrue(file.hasOptions());
        assertEquals(2, file.getOptions().getUninterpretedOptionCount());

        UninterpretedOption opt1 = file.getOptions().getUninterpretedOption(0);
        assertEquals("java_package", opt1.getName(0).getNamePart());

        UninterpretedOption opt2 = file.getOptions().getUninterpretedOption(1);
        assertEquals("optimize_for", opt2.getName(0).getNamePart());
    }

    @Test(timeout = 10000)
    public void testMessageOption() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  option message_set_wire_format = true;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertTrue(msg.hasOptions());
        assertEquals(1, msg.getOptions().getUninterpretedOptionCount());
    }

    @Test(timeout = 10000)
    public void testSyntaxProto2() {
        FileDescriptorProto file = parseExpectingSuccess(
            "syntax = \"proto2\";\n" +
            "message TestMessage {}\n");

        assertEquals("proto2", file.getSyntax());
    }

    @Test(timeout = 10000)
    public void testSyntaxProto3() {
        FileDescriptorProto file = parseExpectingSuccess(
            "syntax = \"proto3\";\n" +
            "message TestMessage {}\n");

        assertEquals("proto3", file.getSyntax());
    }

    @Test(timeout = 10000)
    public void testImplicitSyntax() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  required int32 foo = 1;\n" +
            "}\n");

        assertNotNull(file);
    }

    @Test(timeout = 10000)
    public void testEmptyMessage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {}\n");

        assertEquals(1, file.getMessageTypeCount());
        assertEquals("TestMessage", file.getMessageType(0).getName());
        assertEquals(0, file.getMessageType(0).getFieldCount());
    }

    @Test(timeout = 10000)
    public void testEmptyEnum() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {}\n");

        assertEquals(1, file.getEnumTypeCount());
        assertEquals("TestEnum", file.getEnumType(0).getName());
        assertEquals(0, file.getEnumType(0).getValueCount());
    }

    @Test(timeout = 10000)
    public void testEmptyService() {
        FileDescriptorProto file = parseExpectingSuccess(
            "service TestService {}\n");

        assertEquals(1, file.getServiceCount());
        assertEquals("TestService", file.getService(0).getName());
        assertEquals(0, file.getService(0).getMethodCount());
    }

    @Test(timeout = 10000)
    public void testRepeatedField() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  repeated int32 foo = 1;\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertEquals(FieldDescriptorProto.Label.LABEL_REPEATED, field.getLabel());
    }

    @Test(timeout = 10000)
    public void testMessageReferenceField() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional OtherMessage foo = 1;\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertEquals("OtherMessage", field.getTypeName());
    }

    @Test(timeout = 10000)
    public void testQualifiedTypeReference() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional com.example.OtherMessage foo = 1;\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertEquals("com.example.OtherMessage", field.getTypeName());
    }

    @Test(timeout = 10000)
    public void testMultipleMessages() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message Foo {}\n" +
            "message Bar {}\n" +
            "message Baz {}\n");

        assertEquals(3, file.getMessageTypeCount());
        assertEquals("Foo", file.getMessageType(0).getName());
        assertEquals("Bar", file.getMessageType(1).getName());
        assertEquals("Baz", file.getMessageType(2).getName());
    }

    @Test(timeout = 10000)
    public void testMultipleEnums() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum Foo { A = 0; }\n" +
            "enum Bar { B = 0; }\n");

        assertEquals(2, file.getEnumTypeCount());
        assertEquals("Foo", file.getEnumType(0).getName());
        assertEquals("Bar", file.getEnumType(1).getName());
    }

    @Test(timeout = 10000)
    public void testDeeplyNestedMessages() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message A {\n" +
            "  message B {\n" +
            "    message C {\n" +
            "      optional int32 x = 1;\n" +
            "    }\n" +
            "  }\n" +
            "}\n");

        DescriptorProto a = file.getMessageType(0);
        assertEquals("A", a.getName());
        DescriptorProto b = a.getNestedType(0);
        assertEquals("B", b.getName());
        DescriptorProto c = b.getNestedType(0);
        assertEquals("C", c.getName());
        assertEquals("x", c.getField(0).getName());
    }

    @Test(timeout = 10000)
    public void testFieldWithComment() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  // This is a comment.\n" +
            "  optional int32 foo = 1;\n" +
            "}\n");

        assertEquals(1, file.getMessageType(0).getFieldCount());
        assertEquals("foo", file.getMessageType(0).getField(0).getName());
    }

    @Test(timeout = 10000)
    public void testBlockComment() {
        FileDescriptorProto file = parseExpectingSuccess(
            "/* Block comment */\n" +
            "message TestMessage {\n" +
            "  /* another comment */\n" +
            "  optional int32 foo = 1;\n" +
            "}\n");

        assertNotNull(file);
        assertEquals(1, file.getMessageTypeCount());
    }

    @Test(timeout = 10000)
    public void testMultipleFieldsInMessage() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional int32 foo = 1;\n" +
            "  optional int32 bar = 2;\n" +
            "  optional string baz = 3;\n" +
            "}\n");

        assertEquals(3, file.getMessageType(0).getFieldCount());
        assertEquals("foo", file.getMessageType(0).getField(0).getName());
        assertEquals("bar", file.getMessageType(0).getField(1).getName());
        assertEquals("baz", file.getMessageType(0).getField(2).getName());
    }

    @Test(timeout = 10000)
    public void testEnumWithAllowAlias() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum TestEnum {\n" +
            "  option allow_alias = true;\n" +
            "  FOO = 1;\n" +
            "  BAR = 2;\n" +
            "  BAZ = 2;\n" +
            "}\n");

        EnumDescriptorProto e = file.getEnumType(0);
        assertTrue(e.hasOptions());
        assertEquals(3, e.getValueCount());
    }

    @Test(timeout = 10000)
    public void testExtendWithMultipleFields() {
        FileDescriptorProto file = parseExpectingSuccess(
            "extend Foo {\n" +
            "  optional int32 a = 1;\n" +
            "  optional string b = 2;\n" +
            "  optional bytes c = 3;\n" +
            "}\n");

        assertEquals(3, file.getExtensionCount());
        assertEquals("Foo", file.getExtension(0).getExtendee());
        assertEquals("Foo", file.getExtension(1).getExtendee());
        assertEquals("Foo", file.getExtension(2).getExtendee());
    }

    @Test(timeout = 10000)
    public void testServiceWithOptions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "service TestService {\n" +
            "  option deprecated = true;\n" +
            "  rpc Foo(In) returns (Out);\n" +
            "}\n");

        ServiceDescriptorProto svc = file.getService(0);
        assertTrue(svc.hasOptions());
        assertEquals(1, svc.getMethodCount());
    }

    @Test(timeout = 10000)
    public void testMethodWithBody() {
        FileDescriptorProto file = parseExpectingSuccess(
            "service TestService {\n" +
            "  rpc Foo(In) returns (Out) {\n" +
            "    option deadline = 1.0;\n" +
            "  }\n" +
            "}\n");

        MethodDescriptorProto method = file.getService(0).getMethod(0);
        assertTrue(method.hasOptions());
    }

    @Test(timeout = 10000)
    public void testProto3WithRepeated() {
        FileDescriptorProto file = parseExpectingSuccess(
            "syntax = \"proto3\";\n" +
            "message TestMessage {\n" +
            "  repeated int32 foo = 1;\n" +
            "  string bar = 2;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(FieldDescriptorProto.Label.LABEL_REPEATED, msg.getField(0).getLabel());
        assertEquals(FieldDescriptorProto.Label.LABEL_OPTIONAL, msg.getField(1).getLabel());
    }

    @Test(timeout = 10000)
    public void testMapWithEnumValue() {
        FileDescriptorProto file = parseExpectingSuccess(
            "enum Foo { A = 0; }\n" +
            "message TestMessage {\n" +
            "  map<string, Foo> my_map = 1;\n" +
            "}\n");

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(1, msg.getNestedTypeCount());
        DescriptorProto entry = msg.getNestedType(0);
        assertTrue(entry.getOptions().getMapEntry());
        assertEquals("MyMapEntry", entry.getName());
    }

    @Test(timeout = 10000)
    public void testFieldJsonName() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional string foo_bar = 1 [json_name = \"FooBar\"];\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertTrue(field.hasOptions());
    }

    @Test(timeout = 10000)
    public void testEmptyFileWithSyntax() {
        FileDescriptorProto file = parseExpectingSuccess("syntax = \"proto3\";\n");
        assertNotNull(file);
        assertEquals(0, file.getMessageTypeCount());
        assertEquals("proto3", file.getSyntax());
    }

    @Test(timeout = 10000)
    public void testRepeatedLabel() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  repeated string items = 1;\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertEquals(FieldDescriptorProto.Label.LABEL_REPEATED, field.getLabel());
        assertEquals(FieldDescriptorProto.Type.TYPE_STRING, field.getType());
    }

    @Test(timeout = 10000)
    public void testPackedOption() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  repeated int32 foo = 1 [packed = true];\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertTrue(field.hasOptions());
        assertEquals(1, field.getOptions().getUninterpretedOptionCount());
    }

    @Test(timeout = 10000)
    public void testDeprecatedField() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional int32 foo = 1 [deprecated = true];\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertTrue(field.hasOptions());
    }

    @Test(timeout = 10000)
    public void testMultipleFieldOptions() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  optional string foo = 1 [deprecated = true, (custom) = \"test\"];\n" +
            "}\n");

        FieldDescriptorProto field = file.getMessageType(0).getField(0);
        assertTrue(field.hasOptions());
        assertEquals(2, field.getOptions().getUninterpretedOptionCount());
    }

    @Test(timeout = 10000)
    public void testExtensionRangeWithMax() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  extensions 1 to max;\n" +
            "}\n");

        assertEquals(1, file.getMessageType(0).getExtensionRangeCount());
        assertEquals(536870912, file.getMessageType(0).getExtensionRange(0).getEnd());
    }

    @Test(timeout = 10000)
    public void testSingleFieldExtensionRange() {
        FileDescriptorProto file = parseExpectingSuccess(
            "message TestMessage {\n" +
            "  extensions 42;\n" +
            "}\n");

        assertEquals(1, file.getMessageType(0).getExtensionRangeCount());
        assertEquals(42, file.getMessageType(0).getExtensionRange(0).getStart());
        assertEquals(43, file.getMessageType(0).getExtensionRange(0).getEnd());
    }
}
