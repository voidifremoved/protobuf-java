package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.rubberjam.protobuf.io.Tokenizer;

@RunWith(JUnit4.class)
public class ParserTest
{

	// Simple Mock Tokenizer
	private static class MockTokenizer extends Tokenizer
	{
		private final List<Token> tokens = new ArrayList<>();
		private int index = -1;

		public MockTokenizer(String... rawTokens)
		{
			super(null, null); // Assuming constructor accepts null input stream
			for (String t : rawTokens)
			{
				TokenType type = TokenType.IDENTIFIER;
				if (t.matches("-?\\d+"))
					type = TokenType.INTEGER;
				else if (t.startsWith("\""))
					type = TokenType.STRING;
				else if (t.matches("[{};=.]")) type = TokenType.SYMBOL;

				tokens.add(new Token(type, t, 0, 0, 0));
			}
			tokens.add(new Token(TokenType.END, "", 0, 0, 0));
		}

		@Override
		public boolean next()
		{
			index++;
			return index < tokens.size();
		}

		@Override
		public Token current()
		{
			if (index < 0) return new Token(TokenType.START, "", 0, 0, 0);
			if (index >= tokens.size()) return tokens.get(tokens.size() - 1);
			return tokens.get(index);
		}
	}

	@Test
	public void testParseSimpleMessage()
	{
		MockTokenizer tokenizer = new MockTokenizer(
				"syntax", "\"proto2\"", ";",
				"package", "com.example", ";",
				"message", "Foo", "{",
				"optional", "int32", "bar", "=", "1", ";",
				"}");

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
		MockTokenizer tokenizer = new MockTokenizer(
				"syntax", "\"proto3\"", ";",
				"enum", "Color", "{",
				"RED", "=", "0", ";",
				"BLUE", "=", "1", ";",
				"}");

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
	}
}