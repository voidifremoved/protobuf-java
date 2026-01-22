// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.TextFormat;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * Helper methods for Java code generation.
 */
public final class Helpers
{
	private static final String[] BIT_MASKS = new String[]
	{
			"0x00000001", "0x00000002", "0x00000004", "0x00000008",
			"0x00000010", "0x00000020", "0x00000040", "0x00000080",
			"0x00000100", "0x00000200", "0x00000400", "0x00000800",
			"0x00001000", "0x00002000", "0x00004000", "0x00008000",
			"0x00010000", "0x00020000", "0x00040000", "0x00080000",
			"0x00100000", "0x00200000", "0x00400000", "0x00800000",
			"0x01000000", "0x02000000", "0x04000000", "0x08000000",
			"0x10000000", "0x20000000", "0x40000000", "0x80000000"
	};

	private Helpers()
	{
	}

	public static String getBitFieldName(int index)
	{
		return "bitField" + index + "_";
	}

	private static String getBitFieldNameForBit(int bitIndex)
	{
		return getBitFieldName(bitIndex / 32);
	}

	private static String generateGetBitInternal(String prefix, int bitIndex)
	{
		String varName = prefix + getBitFieldNameForBit(bitIndex);
		int bitInVarIndex = bitIndex % 32;
		return "((" + varName + " & " + BIT_MASKS[bitInVarIndex] + ") != 0)";
	}

	private static String generateSetBitInternal(String prefix, int bitIndex)
	{
		String varName = prefix + getBitFieldNameForBit(bitIndex);
		int bitInVarIndex = bitIndex % 32;
		return varName + " |= " + BIT_MASKS[bitInVarIndex];
	}

	public static String generateGetBit(int bitIndex)
	{
		return generateGetBitInternal("", bitIndex);
	}

	public static String generateSetBit(int bitIndex)
	{
		return generateSetBitInternal("", bitIndex);
	}

	public static String generateClearBit(int bitIndex)
	{
		String varName = getBitFieldNameForBit(bitIndex);
		int bitInVarIndex = bitIndex % 32;
		return varName + " = (" + varName + " & ~" + BIT_MASKS[bitInVarIndex] + ")";
	}

	public static String generateGetBitFromLocal(int bitIndex)
	{
		return generateGetBitInternal("from_", bitIndex);
	}

	public static String generateSetBitToLocal(int bitIndex)
	{
		return generateSetBitInternal("to_", bitIndex);
	}

	public static String generateGetBitMutableLocal(int bitIndex)
	{
		return generateGetBitInternal("mutable_", bitIndex);
	}

	public static String generateSetBitMutableLocal(int bitIndex)
	{
		return generateSetBitInternal("mutable_", bitIndex);
	}

	public static boolean isReferenceType(JavaType type)
	{
		switch (type)
		{
		case INT:
		case LONG:
		case FLOAT:
		case DOUBLE:
		case BOOLEAN:
			return false;
		case STRING:
		case BYTES:
		case ENUM:
		case MESSAGE:
			return true;
		}
		return false;
	}

	public static String getCapitalizedType(FieldDescriptor field)
	{
		switch (field.getType())
		{
		case INT32:
			return "Int32";
		case UINT32:
			return "UInt32";
		case SINT32:
			return "SInt32";
		case FIXED32:
			return "Fixed32";
		case SFIXED32:
			return "SFixed32";
		case INT64:
			return "Int64";
		case UINT64:
			return "UInt64";
		case SINT64:
			return "SInt64";
		case FIXED64:
			return "Fixed64";
		case SFIXED64:
			return "SFixed64";
		case FLOAT:
			return "Float";
		case DOUBLE:
			return "Double";
		case BOOL:
			return "Bool";
		case STRING:
			return "String";
		case BYTES:
			return "Bytes";
		case ENUM:
			return "Enum";
		case GROUP:
			return "Group";
		case MESSAGE:
			return "Message";
		}
		throw new IllegalArgumentException("Unknown field type: " + field.getType());
	}

	public static String defaultValue(FieldDescriptor field, ClassNameResolver nameResolver, Options options, boolean immutable)
	{
		switch (field.getType())
		{
		case INT32:
		case SINT32:
		case FIXED32:
		case SFIXED32:
			return Integer.toString(((Integer) field.getDefaultValue()).intValue());
		case UINT32:
			return Integer.toString(((Integer) field.getDefaultValue()).intValue());
		case INT64:
		case SINT64:
		case FIXED64:
		case SFIXED64:
			return ((Long) field.getDefaultValue()) + "L";
		case UINT64:
			return Long.toString(((Long) field.getDefaultValue()).longValue()) + "L";
		case DOUBLE:
		{
			double value = ((Double) field.getDefaultValue()).doubleValue();
			if (Double.isInfinite(value))
			{
				return value > 0 ? "Double.POSITIVE_INFINITY" : "Double.NEGATIVE_INFINITY";
			}
			if (Double.isNaN(value))
			{
				return "Double.NaN";
			}
			return Double.toString(value) + "D";
		}
		case FLOAT:
		{
			float value = ((Float) field.getDefaultValue()).floatValue();
			if (Float.isInfinite(value))
			{
				return value > 0 ? "Float.POSITIVE_INFINITY" : "Float.NEGATIVE_INFINITY";
			}
			if (Float.isNaN(value))
			{
				return "Float.NaN";
			}
			return Float.toString(value) + "F";
		}
		case BOOL:
			return ((Boolean) field.getDefaultValue()) ? "true" : "false";
		case STRING:
		{
			String value = (String) field.getDefaultValue();
			String escaped = escapeText(value);
			if (allAscii(value))
			{
				return "\"" + escaped + "\"";
			}
			String escapedBytes = escapeBytes(ByteString.copyFromUtf8(value));
			return "com.google.protobuf.Internal.stringDefaultValue(\"" + escapedBytes + "\")";
		}
		case BYTES:
		{
			if (field.hasDefaultValue())
			{
				ByteString value = (ByteString) field.getDefaultValue();
				String escaped = escapeBytes(value);
				return "com.google.protobuf.Internal.bytesDefaultValue(\"" + escaped + "\")";
			}
			return "com.google.protobuf.ByteString.EMPTY";
		}
		case ENUM:
		{
			EnumValueDescriptor value = (EnumValueDescriptor) field.getDefaultValue();
			return nameResolver.getImmutableClassName(field.getEnumType()) + "." + value.getName();
		}
		case GROUP:
		case MESSAGE:
			return nameResolver.getClassName(field.getMessageType(), immutable) + ".getDefaultInstance()";
		}
		throw new IllegalArgumentException("Unknown field type: " + field.getType());
	}

	public static boolean isDefaultValueJavaDefault(FieldDescriptor field)
	{
		switch (field.getType())
		{
		case INT32:
		case SINT32:
		case FIXED32:
		case SFIXED32:
			return ((Integer) field.getDefaultValue()).intValue() == 0;
		case UINT32:
			return ((Integer) field.getDefaultValue()).intValue() == 0;
		case INT64:
		case SINT64:
		case FIXED64:
		case SFIXED64:
		case UINT64:
			return ((Long) field.getDefaultValue()).longValue() == 0L;
		case DOUBLE:
			return ((Double) field.getDefaultValue()).doubleValue() == 0.0d;
		case FLOAT:
			return ((Float) field.getDefaultValue()).floatValue() == 0.0f;
		case BOOL:
			return !((Boolean) field.getDefaultValue());
		case ENUM:
			return ((EnumValueDescriptor) field.getDefaultValue()).getNumber() == 0;
		case STRING:
		case BYTES:
		case GROUP:
		case MESSAGE:
			return false;
		}
		return false;
	}

	public static int getTag(FieldDescriptor field)
	{
		return (field.getNumber() << 3) | getWireType(field);
	}

	public static int getTagSize(FieldDescriptor field)
	{
		return com.google.protobuf.CodedOutputStream.computeTagSize(field.getNumber());
	}

	private static int getWireType(FieldDescriptor field)
	{
		switch (field.getType())
		{
		case INT32:
		case INT64:
		case UINT32:
		case UINT64:
		case SINT32:
		case SINT64:
		case BOOL:
		case ENUM:
			return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
		case FIXED64:
		case SFIXED64:
		case DOUBLE:
			return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
		case STRING:
		case BYTES:
		case MESSAGE:
			return com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
		case GROUP:
			return com.google.protobuf.WireFormat.WIRETYPE_START_GROUP;
		case FIXED32:
		case SFIXED32:
		case FLOAT:
			return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
		}
		throw new IllegalArgumentException("Unknown field type: " + field.getType());
	}

	private static boolean allAscii(String text)
	{
		for (int i = 0; i < text.length(); i++)
		{
			if ((text.charAt(i) & 0x80) != 0)
			{
				return false;
			}
		}
		return true;
	}

	private static String escapeText(String text)
	{
		return escapeBytes(ByteString.copyFromUtf8(text));
	}

	private static String escapeBytes(ByteString bytes)
	{
		return TextFormat.escapeBytes(bytes);
	}

	public static void writeDocComment(PrintWriter out, String indent, Consumer<PrintWriter> writer)
	{
		StringWriter buffer = new StringWriter();
		PrintWriter commentWriter = new PrintWriter(buffer);
		writer.accept(commentWriter);
		commentWriter.flush();
		String[] lines = buffer.toString().split("\n", -1);
		for (String line : lines)
		{
			if (line.isEmpty())
			{
				out.println(indent);
			}
			else
			{
				out.println(indent + line);
			}
		}
	}
}
