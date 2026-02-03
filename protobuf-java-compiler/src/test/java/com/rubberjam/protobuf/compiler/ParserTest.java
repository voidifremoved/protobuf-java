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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.rubberjam.protobuf.compiler.ErrorCollector;
import com.rubberjam.protobuf.compiler.Parser;
import com.rubberjam.protobuf.io.Tokenizer;

@RunWith(JUnit4.class)
public class ParserTest
{

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

	private Tokenizer createTokenizer(String input) {
		return new Tokenizer(new StringReader(input), new TestErrorCollector());
	}

	@Test
	public void testParseSimpleMessage()
	{
		String proto =
			"syntax = \"proto2\";\n" +
			"package com.example;\n" +
			"message Foo {\n" +
			"  optional int32 bar = 1;\n" +
			"}\n";

		Tokenizer tokenizer = createTokenizer(proto);
		Parser parser = new Parser();
		FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();

		boolean success = parser.parse(tokenizer, file);

		assertTrue("Parse should succeed", success);
		assertEquals("proto2", file.getSyntax());
		assertEquals("com.example", file.getPackage());
		assertEquals(1, file.getMessageTypeCount());

		var msg = file.getMessageType(0);
		assertEquals("Foo", msg.getName());
		assertEquals(1, msg.getFieldCount());

		var field = msg.getField(0);
		assertEquals("bar", field.getName());
		assertEquals(FieldDescriptorProto.Type.TYPE_INT32, field.getType());
		assertEquals(1, field.getNumber());
		assertEquals(FieldDescriptorProto.Label.LABEL_OPTIONAL, field.getLabel());
	}

	@Test
	public void testParseEnum()
	{
		String proto =
			"syntax = \"proto3\";\n" +
			"enum Color {\n" +
			"  RED = 0;\n" +
			"  BLUE = 1;\n" +
			"}\n";

		Tokenizer tokenizer = createTokenizer(proto);
		Parser parser = new Parser();
		FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();

		boolean success = parser.parse(tokenizer, file);

		assertTrue(success);
		assertEquals("proto3", file.getSyntax());
		assertEquals(1, file.getEnumTypeCount());

		var enumType = file.getEnumType(0);
		assertEquals("Color", enumType.getName());
		assertEquals(2, enumType.getValueCount());
		assertEquals("RED", enumType.getValue(0).getName());
		assertEquals(0, enumType.getValue(0).getNumber());
		assertEquals("BLUE", enumType.getValue(1).getName());
		assertEquals(1, enumType.getValue(1).getNumber());
	}

	@Test
	public void testParseService()
	{
		String proto =
			"syntax = \"proto3\";\n" +
			"service MyService {\n" +
			"  rpc MyMethod (Input) returns (Output);\n" +
			"  rpc MyStreamingMethod (stream Input) returns (stream Output) {\n" +
			"    option deprecated = true;\n" +
			"  }\n" +
			"}\n";

		Tokenizer tokenizer = createTokenizer(proto);
		Parser parser = new Parser();
		FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();

		boolean success = parser.parse(tokenizer, file);
		assertTrue(success);
		assertEquals(1, file.getServiceCount());
		var service = file.getService(0);
		assertEquals("MyService", service.getName());
		assertEquals(2, service.getMethodCount());

		var m1 = service.getMethod(0);
		assertEquals("MyMethod", m1.getName());
		assertEquals("Input", m1.getInputType());
		assertEquals("Output", m1.getOutputType());
		assertFalse(m1.getClientStreaming());
		assertFalse(m1.getServerStreaming());

		var m2 = service.getMethod(1);
		assertEquals("MyStreamingMethod", m2.getName());
		assertEquals("Input", m2.getInputType());
		assertEquals("Output", m2.getOutputType());
		assertTrue(m2.getClientStreaming());
		assertTrue(m2.getServerStreaming());
	}

	@Test
	public void testParseImports()
	{
		String proto =
			"syntax = \"proto3\";\n" +
			"import \"other.proto\";\n" +
			"import public \"public.proto\";\n" +
			"import weak \"weak.proto\";\n";

		Tokenizer tokenizer = createTokenizer(proto);
		Parser parser = new Parser();
		FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();

		boolean success = parser.parse(tokenizer, file);
		assertTrue(success);
		assertEquals(3, file.getDependencyCount());
		assertEquals("other.proto", file.getDependency(0));
		assertEquals("public.proto", file.getDependency(1));
		assertEquals("weak.proto", file.getDependency(2));

		assertEquals(1, file.getPublicDependencyCount());
		assertEquals(1, file.getPublicDependency(0)); // index of public.proto

		assertEquals(1, file.getWeakDependencyCount());
		assertEquals(2, file.getWeakDependency(0)); // index of weak.proto
	}

	@Test
	public void testParseExtensionsAndReserved()
	{
		String proto =
			"syntax = \"proto2\";\n" +
			"message Foo {\n" +
			"  extensions 100 to 199;\n" +
			"  extensions 500;\n" +
			"  reserved 200 to 299;\n" +
			"  reserved \"reserved_name\";\n" +
			"}\n" +
			"extend Foo {\n" +
			"  optional int32 bar = 100;\n" +
			"}\n";

		Tokenizer tokenizer = createTokenizer(proto);
		Parser parser = new Parser();
		FileDescriptorProto.Builder file = FileDescriptorProto.newBuilder();

		boolean success = parser.parse(tokenizer, file);
		assertTrue(success);

		assertEquals(1, file.getMessageTypeCount());
		var msg = file.getMessageType(0);
		assertEquals(2, msg.getExtensionRangeCount());
		assertEquals(100, msg.getExtensionRange(0).getStart());
		assertEquals(200, msg.getExtensionRange(0).getEnd()); // exclusive
		assertEquals(500, msg.getExtensionRange(1).getStart());
		assertEquals(501, msg.getExtensionRange(1).getEnd());

		assertEquals(1, msg.getReservedRangeCount());
		assertEquals(200, msg.getReservedRange(0).getStart());
		assertEquals(300, msg.getReservedRange(0).getEnd());

		assertEquals(1, msg.getReservedNameCount());
		assertEquals("reserved_name", msg.getReservedName(0));

		assertEquals(1, file.getExtensionCount());
		var ext = file.getExtension(0);
		assertEquals("Foo", ext.getExtendee());
		assertEquals("bar", ext.getName());
		assertEquals(100, ext.getNumber());
	}
}
