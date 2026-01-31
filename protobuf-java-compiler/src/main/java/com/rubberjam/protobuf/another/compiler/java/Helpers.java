package com.rubberjam.protobuf.another.compiler.java;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.io.Printer;

/**
 * Helper methods for generating Java code. Ported from helpers.cc/helpers.h.
 */
public class Helpers
{

	public static final String THICK_SEPARATOR = "// ===================================================================\n";
	public static final String THIN_SEPARATOR = "// -------------------------------------------------------------------\n";

	private Helpers()
	{
	}

	public static void maybePrintGeneratedAnnotation(Context context, Printer printer, Descriptor descriptor, boolean immutable, String suffix) {
		// suffix logic?
		printGeneratedAnnotation(printer, '$', context.getOptions().getAnnotationListFile(), context.getOptions());
	}

	public static void maybePrintGeneratedAnnotation(Context context, Printer printer, com.google.protobuf.Descriptors.ServiceDescriptor descriptor, boolean immutable, String suffix) {
		printGeneratedAnnotation(printer, '$', context.getOptions().getAnnotationListFile(), context.getOptions());
	}

	public static void maybePrintGeneratedAnnotation(Context context, Printer printer, EnumDescriptor descriptor, boolean immutable) {
		printGeneratedAnnotation(printer, '$', context.getOptions().getAnnotationListFile(), context.getOptions());
	}

	public static void printGeneratedAnnotation(Printer printer, char delimiter, String annotationFile, Options options)
	{
		if (options.isAnnotateCode())
		{
			printer.emit("@javax.annotation.Generated(value=\"protoc\", comments=\"annotations:" +
					annotationFile + "\")\n");
		}
		else
		{
			printer.emit("@com.google.protobuf.Generated\n");
			if (annotationFile != null && !annotationFile.isEmpty())
			{
				printer.emit("@javax.annotation.Generated(value=\"protoc\", comments=\"annotations:" +
						annotationFile + "\")\n");
			}
		}
	}

	public static String uniqueFileScopeIdentifier(Descriptor descriptor) {
		return descriptor.getFullName().replace('.', '_');
	}

	public static boolean isOwnFile(Descriptor descriptor, boolean immutable) {
		return descriptor.getContainingType() == null && descriptor.getFile().getOptions().getJavaMultipleFiles();
	}

	public static boolean isOwnFile(com.google.protobuf.Descriptors.ServiceDescriptor descriptor, boolean immutable) {
		return descriptor.getFile().getOptions().getJavaMultipleFiles();
	}

	public static String getGeneratedCodeVersionSuffix() {
		return ""; // Was "V3", now empty to match standard generated message
	}

	public static String getExtraBuilderInterfaces(Descriptor descriptor) {
		return "";
	}

	public static String getExtraMessageOrBuilderInterfaces(Descriptor descriptor) {
		return "";
	}

	public static String getExtraMessageInterfaces(Descriptor descriptor) {
		return "";
	}

	public static boolean hasHasbit(FieldDescriptor field) {
		return field.hasPresence();
	}

	public static boolean isAnyMessage(Descriptor descriptor) {
		return descriptor.getFullName().equals("google.protobuf.Any");
	}

	public static boolean hasPackedFields(Descriptor descriptor) {
		for (FieldDescriptor field : descriptor.getFields()) {
			if (field.isPackable() && field.isPacked()) {
				return true;
			}
		}
		return false;
	}

	public static int getWireTypeForFieldType(FieldDescriptor.Type type) {
		switch (type) {
			case DOUBLE: return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
			case FLOAT: return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
			case INT64: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case UINT64: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case INT32: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case FIXED64: return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
			case FIXED32: return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
			case BOOL: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case STRING: return com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
			case GROUP: return com.google.protobuf.WireFormat.WIRETYPE_START_GROUP;
			case MESSAGE: return com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
			case BYTES: return com.google.protobuf.WireFormat.WIRETYPE_LENGTH_DELIMITED;
			case UINT32: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case ENUM: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case SFIXED32: return com.google.protobuf.WireFormat.WIRETYPE_FIXED32;
			case SFIXED64: return com.google.protobuf.WireFormat.WIRETYPE_FIXED64;
			case SINT32: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			case SINT64: return com.google.protobuf.WireFormat.WIRETYPE_VARINT;
			default: throw new IllegalArgumentException("No such field type");
		}
	}

	public static String fieldConstantName(FieldDescriptor field) {
		return getFieldConstantName(field);
	}

	// --- Naming & String Utilities ---

	public static String toCamelCase(String input, boolean lowerFirst)
	{
		StringBuilder result = new StringBuilder();
		boolean capNext = !lowerFirst;

		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			if (c == '_')
			{
				capNext = true;
			}
			else if (capNext)
			{
				result.append(Character.toUpperCase(c));
				capNext = false;
			}
			else
			{
				result.append(c);
			}
		}
		if (lowerFirst && result.length() > 0)
		{
			result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
		}
		return result.toString();
	}

	public static String underscoresToCamelCase(String input, boolean capNextLetter)
	{
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < input.length(); i++)
		{
			char c = input.charAt(i);
			if (c >= 'a' && c <= 'z')
			{
				if (capNextLetter)
				{
					result.append(Character.toUpperCase(c));
				}
				else
				{
					result.append(c);
				}
				capNextLetter = false;
			}
			else if (c >= 'A' && c <= 'Z')
			{
				if (i == 0 && !capNextLetter)
				{
					result.append(Character.toLowerCase(c));
				}
				else
				{
					result.append(c);
				}
				capNextLetter = false;
			}
			else if (c >= '0' && c <= '9')
			{
				result.append(c);
				capNextLetter = true;
			}
			else
			{
				capNextLetter = true;
			}
		}
		if (input.endsWith("#"))
		{
			result.append('_');
		}
		return result.toString();
	}

	public static String camelCaseFieldName(FieldDescriptor field)
	{
		String fieldName = underscoresToCamelCase(field.getName(), false);
		if (fieldName.length() > 0 && Character.isDigit(fieldName.charAt(0)))
		{
			return "_" + fieldName;
		}
		return fieldName;
	}

	public static String getFieldConstantName(FieldDescriptor field)
	{
		return field.getName().toUpperCase() + "_FIELD_NUMBER";
	}

    public static int getWireFormatForField(FieldDescriptor field) {
        return makeTag(field.getNumber(), getWireTypeForFieldType(field.getType()));
    }

	public static int makeTag(int fieldNumber, int wireType) {
		return (fieldNumber << 3) | wireType;
	}

	// --- Kotlin Support ---

	//
	private static final Set<String> KOTLIN_FORBIDDEN_NAMES = new HashSet<>(Arrays.asList(
			"as", "as?", "break", "class", "continue", "do",
			"else", "false", "for", "fun", "if", "in",
			"!in", "interface", "is", "!is", "null", "object",
			"package", "return", "super", "this", "throw", "true",
			"try", "typealias", "typeof", "val", "var", "when",
			"while"));

	public static boolean isForbiddenKotlin(String fieldName)
	{
		return KOTLIN_FORBIDDEN_NAMES.contains(fieldName);
	}

	// --- Type Mapping ---

	public static FieldDescriptor.Type getType(FieldDescriptor field) {
		return field.getType();
	}

	public enum JavaType
	{
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		BOOLEAN,
		STRING,
		BYTES,
		ENUM,
		MESSAGE
	}

	//
	public static JavaType getJavaType(FieldDescriptor field)
	{
		switch (field.getType())
		{
		case INT32:
		case UINT32:
		case SINT32:
		case FIXED32:
		case SFIXED32:
			return JavaType.INT;
		case INT64:
		case UINT64:
		case SINT64:
		case FIXED64:
		case SFIXED64:
			return JavaType.LONG;
		case FLOAT:
			return JavaType.FLOAT;
		case DOUBLE:
			return JavaType.DOUBLE;
		case BOOL:
			return JavaType.BOOLEAN;
		case STRING:
			return JavaType.STRING;
		case BYTES:
			return JavaType.BYTES;
		case ENUM:
			return JavaType.ENUM;
		case GROUP:
		case MESSAGE:
			return JavaType.MESSAGE;
		default:
			throw new IllegalArgumentException("Unknown type: " + field.getType());
		}
	}

	public static String getPrimitiveTypeName(JavaType type)
	{
		switch (type)
		{
		case INT:
			return "int";
		case LONG:
			return "long";
		case FLOAT:
			return "float";
		case DOUBLE:
			return "double";
		case BOOLEAN:
			return "boolean";
		case STRING:
			return "java.lang.String";
		case BYTES:
			return "com.google.protobuf.ByteString";
		case ENUM:
			return null;
		case MESSAGE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown type");
		}
	}

	public static String getBoxedPrimitiveTypeName(JavaType type)
	{
		switch (type)
		{
		case INT:
			return "java.lang.Integer";
		case LONG:
			return "java.lang.Long";
		case FLOAT:
			return "java.lang.Float";
		case DOUBLE:
			return "java.lang.Double";
		case BOOLEAN:
			return "java.lang.Boolean";
		case STRING:
			return "java.lang.String";
		case BYTES:
			return "com.google.protobuf.ByteString";
		case ENUM:
			return null;
		case MESSAGE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown type");
		}
	}

	//
	public static String getFieldTypeName(FieldDescriptor.Type type)
	{
		switch (type)
		{
		case INT32:
			return "INT32";
		case UINT32:
			return "UINT32";
		case SINT32:
			return "SINT32";
		case FIXED32:
			return "FIXED32";
		case SFIXED32:
			return "SFIXED32";
		case INT64:
			return "INT64";
		case UINT64:
			return "UINT64";
		case SINT64:
			return "SINT64";
		case FIXED64:
			return "FIXED64";
		case SFIXED64:
			return "SFIXED64";
		case FLOAT:
			return "FLOAT";
		case DOUBLE:
			return "DOUBLE";
		case BOOL:
			return "BOOL";
		case STRING:
			return "STRING";
		case BYTES:
			return "BYTES";
		case ENUM:
			return "ENUM";
		case GROUP:
			return "GROUP";
		case MESSAGE:
			return "MESSAGE";
		default:
			throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	//
	public static String getOneofStoredType(FieldDescriptor field)
	{
		JavaType javaType = getJavaType(field);
		switch (javaType)
		{
		case ENUM:
			return "java.lang.Integer";
		case MESSAGE:
			return new ClassNameResolver().getClassName(field.getMessageType(), true);
		default:
			return getBoxedPrimitiveTypeName(javaType);
		}
	}

	// --- Logic Helpers ---

	//
	public static boolean isMapEntry(Descriptor descriptor)
	{
		return descriptor.getOptions().getMapEntry();
	}

	// --- Default Values ---

	public static String defaultValue(FieldDescriptor field, boolean immutable, ClassNameResolver nameResolver, Options options)
	{
		switch (field.getType())
		{
		case INT32:
			return String.valueOf(field.getDefaultValue());
		case UINT32:
			return String.valueOf(field.getDefaultValue());
		case INT64:
			return field.getDefaultValue() + "L";
		case UINT64:
			return field.getDefaultValue() + "L";
		case DOUBLE:
		{
			Double value = (Double) field.getDefaultValue();
			if (value.equals(Double.POSITIVE_INFINITY)) return "Double.POSITIVE_INFINITY";
			if (value.equals(Double.NEGATIVE_INFINITY)) return "Double.NEGATIVE_INFINITY";
			if (value.isNaN()) return "Double.NaN";
			return value + "D";
		}
		case FLOAT:
		{
			Float value = (Float) field.getDefaultValue();
			if (value.equals(Float.POSITIVE_INFINITY)) return "Float.POSITIVE_INFINITY";
			if (value.equals(Float.NEGATIVE_INFINITY)) return "Float.NEGATIVE_INFINITY";
			if (value.isNaN()) return "Float.NaN";
			return value + "F";
		}
		case BOOL:
			return ((Boolean) field.getDefaultValue()) ? "true" : "false";
		case STRING:
			String s = (String) field.getDefaultValue();
			return "\"" + escapeJavaString(s) + "\"";
		case BYTES:
			return "com.google.protobuf.ByteString.EMPTY";
		case ENUM:
			EnumDescriptor enumType = field.getEnumType();
			return nameResolver.getClassName(enumType, immutable) + "." +
					((com.google.protobuf.Descriptors.EnumValueDescriptor) field.getDefaultValue()).getName();
		case MESSAGE:
			return nameResolver.getClassName(field.getMessageType(), immutable) + ".getDefaultInstance()";
		default:
			return "";
		}
	}

	public static boolean isDefaultValueJavaDefault(FieldDescriptor field)
	{
		switch (field.getJavaType())
		{
		case INT:
			return ((Integer) field.getDefaultValue()) == 0;
		case LONG:
			return ((Long) field.getDefaultValue()) == 0L;
		case FLOAT:
			return ((Float) field.getDefaultValue()) == 0.0f;
		case DOUBLE:
			return ((Double) field.getDefaultValue()) == 0.0d;
		case BOOLEAN:
			return !((Boolean) field.getDefaultValue());
		case ENUM:
			return ((EnumValueDescriptor) field.getDefaultValue()).getNumber() == 0;
		case STRING:
		case MESSAGE:
		case BYTE_STRING:
			return false;
		default:
			return false;
		}
	}

	public static boolean isByteStringWithCustomDefaultValue(FieldDescriptor field)
	{
		return field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING && !field.getDefaultValue().equals(com.google.protobuf.ByteString.EMPTY);
	}

	// --- Bitfield Logic ---

	public static String getBitFieldName(int index)
	{
		return "bitField" + index + "_";
	}

	public static String getBitFieldNameForBit(int bitIndex)
	{
		return getBitFieldName(bitIndex / 32);
	}

	private static final String[] BIT_MASKS = {
			"0x00000001", "0x00000002", "0x00000004", "0x00000008",
			"0x00000010", "0x00000020", "0x00000040", "0x00000080",
			"0x00000100", "0x00000200", "0x00000400", "0x00000800",
			"0x00001000", "0x00002000", "0x00004000", "0x00008000",
			"0x00010000", "0x00020000", "0x00040000", "0x00080000",
			"0x00100000", "0x00200000", "0x00400000", "0x00800000",
			"0x01000000", "0x02000000", "0x04000000", "0x08000000",
			"0x10000000", "0x20000000", "0x40000000", "0x80000000"
	};

	private static String generateGetBitInternal(String prefix, int bitIndex)
	{
		String varName = prefix + getBitFieldNameForBit(bitIndex);
		int maskIndex = bitIndex % 32;
		return "((" + varName + " & " + BIT_MASKS[maskIndex] + ") != 0)";
	}

	private static String generateSetBitInternal(String prefix, int bitIndex)
	{
		String varName = prefix + getBitFieldNameForBit(bitIndex);
		int maskIndex = bitIndex % 32;
		return varName + " |= " + BIT_MASKS[maskIndex];
	}

	public static String generateGetBit(int bitIndex)
	{
		return generateGetBitInternal("", bitIndex);
	}

	public static String generateGetBit(String prefix, int bitIndex)
	{
		return generateGetBitInternal(prefix, bitIndex);
	}

	public static String generateSetBit(int bitIndex)
	{
		return generateSetBitInternal("", bitIndex);
	}

	public static String generateClearBit(int bitIndex)
	{
		String varName = getBitFieldNameForBit(bitIndex);
		int maskIndex = bitIndex % 32;
		return varName + " = (" + varName + " & ~" + BIT_MASKS[maskIndex] + ")";
	}

	// --- Logic Helpers ---

	public static boolean hasRequiredFields(Descriptor descriptor)
	{
		for (FieldDescriptor field : descriptor.getFields())
		{
			if (field.isRequired()) return true;
			if (field.getType() == FieldDescriptor.Type.MESSAGE)
			{
				if (hasRequiredFields(field.getMessageType())) return true;
			}
		}
		return false;
	}

	private static String escapeJavaString(String s)
	{
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r");
	}

	public static void writeIntToUtf16CharSequence(int value, java.util.List<Integer> destination) {
		if (value < 0xD800) {
			destination.add(value);
		} else {
			destination.add(0xD800 | ((value >> 13) & 0x1F));
			destination.add(value & 0x1FFF);
		}
	}

	public static String immutableDefaultValue(FieldDescriptor field, ClassNameResolver nameResolver, Options options) {
		return defaultValue(field, true, nameResolver, options);
	}

	public static String getCapitalizedType(FieldDescriptor field, boolean immutable) {
		switch (field.getType()) {
			case INT32: return "Int32";
			case UINT32: return "UInt32";
			case SINT32: return "SInt32";
			case FIXED32: return "Fixed32";
			case SFIXED32: return "SFixed32";
			case INT64: return "Int64";
			case UINT64: return "UInt64";
			case SINT64: return "SInt64";
			case FIXED64: return "Fixed64";
			case SFIXED64: return "SFixed64";
			case FLOAT: return "Float";
			case DOUBLE: return "Double";
			case BOOL: return "Bool";
			case STRING: return "String";
			case BYTES: return "Bytes";
			case ENUM: return "Enum";
			case GROUP: return "Group";
			case MESSAGE: return "Message";
			default: return "";
		}
	}
}