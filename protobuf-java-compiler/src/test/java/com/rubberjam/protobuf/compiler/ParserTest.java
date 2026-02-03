package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    }

    private FileDescriptorProto parse(String input) {
        Tokenizer tokenizer = new Tokenizer(new StringReader(input), new TestErrorCollector());
        Parser parser = new Parser();
        FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();
        if (parser.parse(tokenizer, file)) {
            return file.build();
        }
        return null;
    }

    private void assertParses(String input) {
        FileDescriptorProto result = parse(input);
        assertTrue("Expected parse success", result != null);
    }

    @Test
    public void testSimpleMessage() {
        String input =
            "message TestMessage {\n" +
            "  required int32 foo = 1;\n" +
            "}\n";
        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
        DescriptorProto msg = file.getMessageType(0);
        assertEquals("TestMessage", msg.getName());
        FieldDescriptorProto field = msg.getField(0);
        assertEquals("foo", field.getName());
        assertEquals(FieldDescriptorProto.Label.LABEL_REQUIRED, field.getLabel());
        assertEquals(FieldDescriptorProto.Type.TYPE_INT32, field.getType());
        assertEquals(1, field.getNumber());
    }

    @Test
    public void testPrimitiveFieldTypes() {
        String input =
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
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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

    @Test
    public void testFieldDefaults() {
        String input =
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
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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
        assertEquals("0x10", msg.getField(11).getDefaultValue()); // or 16 depending on normalization
    }

    @Test
    public void testFieldOptions() {
        String input =
            "message TestMessage {\n" +
            "  optional int32 foo = 1 [ctype = CORD, (custom.opt) = 1];\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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

    @Test
    public void testOneof() {
        String input =
            "message TestMessage {\n" +
            "  oneof foo {\n" +
            "    int32 a = 1;\n" +
            "    string b = 2;\n" +
            "  }\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
        DescriptorProto msg = file.getMessageType(0);

        assertEquals(1, msg.getOneofDeclCount());
        OneofDescriptorProto oneof = msg.getOneofDecl(0);
        assertEquals("foo", oneof.getName());

        assertEquals(2, msg.getFieldCount());
        assertEquals(0, msg.getField(0).getOneofIndex());
        assertEquals(0, msg.getField(1).getOneofIndex());
    }

    @Test
    public void testMaps() {
        String input =
            "message TestMessage {\n" +
            "  map<int32, string> primitive_map = 1;\n" +
            "  map<string, Foo> msg_map = 2;\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
        DescriptorProto msg = file.getMessageType(0);

        assertEquals(2, msg.getFieldCount());

        FieldDescriptorProto f1 = msg.getField(0);
        assertEquals("primitive_map", f1.getName());
        assertEquals(FieldDescriptorProto.Label.LABEL_REPEATED, f1.getLabel());
        assertEquals(FieldDescriptorProto.Type.TYPE_MESSAGE, f1.getType());

        // Check synthetic Entry message
        String entry1Name = f1.getTypeName(); // "PrimitiveMapEntry" typically, or resolvable
        // But in proto, type_name isn't set until resolution usually? Or parser sets a guess?
        // Parser.java usually sets type_name to the generated nested type name.

        assertEquals(2, msg.getNestedTypeCount());
        DescriptorProto entry1 = msg.getNestedType(0);
        assertTrue(entry1.getOptions().getMapEntry());
        assertEquals("key", entry1.getField(0).getName());
        assertEquals(FieldDescriptorProto.Type.TYPE_INT32, entry1.getField(0).getType());
        assertEquals("value", entry1.getField(1).getName());
        assertEquals(FieldDescriptorProto.Type.TYPE_STRING, entry1.getField(1).getType());
    }

    @Test
    public void testGroup() {
        String input =
            "message TestMessage {\n" +
            "  optional group MyGroup = 1 {\n" +
            "    optional int32 a = 1;\n" +
            "  }\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
        DescriptorProto msg = file.getMessageType(0);

        assertEquals(1, msg.getFieldCount());
        FieldDescriptorProto field = msg.getField(0);
        assertEquals(FieldDescriptorProto.Type.TYPE_GROUP, field.getType());
        assertEquals("MyGroup", field.getTypeName());
        assertEquals("mygroup", field.getName()); // Lowercased name

        assertEquals(1, msg.getNestedTypeCount());
        assertEquals("MyGroup", msg.getNestedType(0).getName());
    }

    @Test
    public void testExtensions() {
        String input =
            "message TestMessage {\n" +
            "  extensions 100 to 199;\n" +
            "  extensions 200 to max;\n" +
            "}\n" +
            "extend TestMessage {\n" +
            "  optional int32 ext_field = 100;\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);

        DescriptorProto msg = file.getMessageType(0);
        assertEquals(2, msg.getExtensionRangeCount());
        assertEquals(100, msg.getExtensionRange(0).getStart());
        assertEquals(200, msg.getExtensionRange(0).getEnd());

        assertEquals(200, msg.getExtensionRange(1).getStart());
        assertEquals(536870912, msg.getExtensionRange(1).getEnd()); // 2^29

        assertEquals(1, file.getExtensionCount());
        FieldDescriptorProto ext = file.getExtension(0);
        assertEquals("TestMessage", ext.getExtendee());
        assertEquals("ext_field", ext.getName());
        assertEquals(100, ext.getNumber());
    }

    @Test
    public void testReserved() {
        String input =
            "message TestMessage {\n" +
            "  reserved 1, 2 to 5, 10 to max;\n" +
            "  reserved \"foo\", \"bar\";\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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

    @Test
    public void testServices() {
        String input =
            "service TestService {\n" +
            "  rpc Foo(In) returns (Out);\n" +
            "  rpc Bar(stream In) returns (stream Out) { option deprecated = true; }\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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
        // We only test parsing, not resolution, so we check for UninterpretedOption
        assertTrue(m2.getOptions().getUninterpretedOptionCount() > 0);
        UninterpretedOption opt = m2.getOptions().getUninterpretedOption(0);
        assertEquals("deprecated", opt.getName(0).getNamePart());
        assertEquals("true", opt.getIdentifierValue());
    }

    @Test
    public void testImportsAndPackage() {
        String input =
            "syntax = \"proto3\";\n" +
            "package com.example;\n" +
            "import \"a.proto\";\n" +
            "import public \"b.proto\";\n" +
            "import weak \"c.proto\";\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
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

    @Test
    public void testEnums() {
        String input =
            "enum TestEnum {\n" +
            "  ZERO = 0;\n" +
            "  NEG = -1;\n" +
            "  HEX = 0x10;\n" +
            "}\n";

        FileDescriptorProto file = parse(input);
        assertTrue(file != null);
        EnumDescriptorProto e = file.getEnumType(0);
        assertEquals("TestEnum", e.getName());

        assertEquals(3, e.getValueCount());
        assertEquals(0, e.getValue(0).getNumber());
        assertEquals(-1, e.getValue(1).getNumber());
        assertEquals(16, e.getValue(2).getNumber());
    }
}
