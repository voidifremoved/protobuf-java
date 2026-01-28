package com.rubberjam.protobuf.compiler.java.proto2.speed;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.EnumGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ImmutableEnumGenerator extends EnumGenerator
{
	private final EnumDescriptor descriptor;
	private final JavaContext context;
	private final boolean immutableApi = true;

	private final List<EnumValueDescriptor> canonicalValues = new ArrayList<>();
	private final List<Alias> aliases = new ArrayList<>();

	private static class Alias
	{
		EnumValueDescriptor value;
		EnumValueDescriptor canonicalValue;
	}

	public ImmutableEnumGenerator(EnumDescriptor descriptor, JavaContext context)
	{
		this.descriptor = descriptor;
		this.context = context;

		for (EnumValueDescriptor value : descriptor.getValues())
		{
			EnumValueDescriptor canonicalValue = descriptor.findValueByNumber(value.getNumber());
			if (value == canonicalValue)
			{
				canonicalValues.add(value);
			}
			else
			{
				Alias alias = new Alias();
				alias.value = value;
				alias.canonicalValue = canonicalValue;
				aliases.add(alias);
			}
		}
	}

	@Override
	public void generate(PrintWriter printer)
	{
		// Match C++ WriteEnumDocComment behavior - use DocComment utility with 2-space indent
		// For nested enums, use 4-space indent
		String indent = descriptor.getContainingType() != null ? "    " : "  ";
		DocComment.writeEnumDocComment(printer, descriptor, context, false, indent);
		
		String classname = descriptor.getName();
		String deprecation = descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
		printer.println(indent + deprecation + "public enum " + classname);
		printer.println(indent + "    implements com.google.protobuf.ProtocolMessageEnum {");

		boolean ordinalIsIndex = true;
		for (int i = 0; i < canonicalValues.size(); i++)
		{
			if (canonicalValues.get(i).getIndex() != i)
			{
				ordinalIsIndex = false;
				break;
			}
		}

		String valueIndent = indent + "  ";
		for (int i = 0; i < canonicalValues.size(); i++)
		{
			EnumValueDescriptor value = canonicalValues.get(i);
			
			// Match C++ WriteEnumValueDocComment behavior - use DocComment utility
			// Add proper indentation prefix for enum values inside enum class
			DocComment.writeEnumValueDocComment(printer, value, context, valueIndent);
			
			// Add deprecation annotation if needed
			if (value.getOptions().getDeprecated())
			{
				printer.println(valueIndent + "@java.lang.Deprecated");
			}
			
			// Generate enum value with proper indentation
			printer.print(valueIndent + value.getName() + "(");
			if (ordinalIsIndex)
			{
				printer.print(value.getNumber());
			}
			else
			{
				printer.print(value.getIndex() + ", " + value.getNumber());
			}
			printer.println("),");
		}

		// Add UNRECOGNIZED for proto3/open enums (before semicolon)
		// Match C++ behavior: UNRECOGNIZED comes before semicolon

		// Semicolon on its own line (match C++ format)
		printer.println(valueIndent + ";");
		printer.println();

		// Static block with version validator (match C++ order)
		printer.println(valueIndent + "static {");
		printer.println(valueIndent + "  com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(");
		printer.println(valueIndent + "    com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,");
		printer.println(valueIndent + "    /* major= */ 4,");
		printer.println(valueIndent + "    /* minor= */ 33,");
		printer.println(valueIndent + "    /* patch= */ 4,");
		printer.println(valueIndent + "    /* suffix= */ \"\",");
		printer.println(valueIndent + "    \"" + classname + "\");");
		printer.println(valueIndent + "}");

		// Aliases
		for (Alias alias : aliases)
		{
			// Match C++ WriteEnumValueDocComment for aliases - use DocComment utility
			// Add proper indentation prefix for aliases inside enum class
			DocComment.writeEnumValueDocComment(printer, alias.value, context, valueIndent);
			printer.println(valueIndent + "public static final " + classname + " " + alias.value.getName() + " = "
					+ alias.canonicalValue.getName() + ";");
		}

		// Value constants
		for (EnumValueDescriptor value : descriptor.getValues())
		{
			// Match C++ WriteEnumValueDocComment for value constants - use DocComment utility
			// Add proper indentation prefix for value constants inside enum class
			DocComment.writeEnumValueDocComment(printer, value, context, valueIndent);
			String deprecationComment = value.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
			printer.println(valueIndent + deprecationComment + "public static final int " + value.getName() + "_VALUE = " + value.getNumber() + ";");
		}
		printer.println();
		printer.println();

		// Standard methods
		printer.println(valueIndent + "public final int getNumber() {");
		printer.println(valueIndent + "  return value;");
		printer.println(valueIndent + "}");
		printer.println();

		printer.println(valueIndent + "/**");
		printer.println(valueIndent + " * @param value The numeric wire value of the corresponding enum entry.");
		printer.println(valueIndent + " * @return The enum associated with the given numeric wire value.");
		printer.println(valueIndent + " * @deprecated Use {@link #forNumber(int)} instead.");
		printer.println(valueIndent + " */");
		printer.println(valueIndent + "@java.lang.Deprecated");
		printer.println(valueIndent + "public static " + classname + " valueOf(int value) {");
		printer.println(valueIndent + "  return forNumber(value);");
		printer.println(valueIndent + "}");
		printer.println();

		printer.println(valueIndent + "/**");
		printer.println(valueIndent + " * @param value The numeric wire value of the corresponding enum entry.");
		printer.println(valueIndent + " * @return The enum associated with the given numeric wire value.");
		printer.println(valueIndent + " */");
		printer.println(valueIndent + "public static " + classname + " forNumber(int value) {");
		printer.println(valueIndent + "  switch (value) {");
		for (EnumValueDescriptor value : canonicalValues)
		{
			printer.println(valueIndent + "    case " + value.getNumber() + ": return " + value.getName() + ";");
		}
		printer.println(valueIndent + "    default: return null;");
		printer.println(valueIndent + "  }");
		printer.println(valueIndent + "}");
		printer.println();

		// Internal map
		printer.println(valueIndent + "public static com.google.protobuf.Internal.EnumLiteMap<" + classname + ">");
		printer.println(valueIndent + "    internalGetValueMap() {");
		printer.println(valueIndent + "  return internalValueMap;");
		printer.println(valueIndent + "}");
		printer.println(valueIndent + "private static final com.google.protobuf.Internal.EnumLiteMap<");
		printer.println(valueIndent + "    " + classname + "> internalValueMap =");
		printer.println(valueIndent + "      new com.google.protobuf.Internal.EnumLiteMap<" + classname + ">() {");
		printer.println(valueIndent + "        public " + classname + " findValueByNumber(int number) {");
		printer.println(valueIndent + "          return " + classname + ".forNumber(number);");
		printer.println(valueIndent + "        }");
		printer.println(valueIndent + "      };");
		printer.println();

		// Reflection
		printer.println(valueIndent + "public final com.google.protobuf.Descriptors.EnumValueDescriptor");
		printer.println(valueIndent + "    getValueDescriptor() {");
		// Simplified logic for proto3 UNRECOGNIZED check
		printer.println(valueIndent + "  return getDescriptor().getValues().get(ordinal());");
		printer.println(valueIndent + "}");

		printer.println(valueIndent + "public final com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println(valueIndent + "    getDescriptorForType() {");
		printer.println(valueIndent + "  return getDescriptor();");
		printer.println(valueIndent + "}");
		printer.println(valueIndent + "public static com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println(valueIndent + "    getDescriptor() {");
		if (descriptor.getContainingType() == null)
		{
			String packageName = context.getNameResolver().getFileJavaPackage(descriptor.getFile());
			String fileClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
			String outerClassName = packageName.isEmpty() ? fileClassName : packageName + "." + fileClassName;
			printer.println(valueIndent + "  return " + outerClassName + ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentMessage = context.getNameResolver().getImmutableClassName(descriptor.getContainingType());
			printer.println(valueIndent + "  return " + parentMessage + ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");");
		}
		printer.println(valueIndent + "}");
		printer.println();

		if (aliases.isEmpty())
		{
			printer.println(valueIndent + "private static final " + classname + "[] VALUES = values();");
			printer.println();
		}
		else
		{
			printer.println(valueIndent + "private static final " + classname + "[] VALUES = getStaticValuesArray();");
			printer.println(valueIndent + "private static " + classname + "[] getStaticValuesArray() {");
			printer.println(valueIndent + "  return new " + classname + "[] {");
			printer.print(valueIndent + "  ");
			for (EnumValueDescriptor value : descriptor.getValues())
			{
				printer.print(value.getName() + ", ");
			}
			printer.println();
			printer.println(valueIndent + "  };");
			printer.println(valueIndent + "}");
		}

		printer.println(valueIndent + "public static " + classname + " valueOf(");
		printer.println(valueIndent + "    com.google.protobuf.Descriptors.EnumValueDescriptor desc) {");
		printer.println(valueIndent + "  if (desc.getType() != getDescriptor()) {");
		printer.println(valueIndent + "    throw new java.lang.IllegalArgumentException(");
		printer.println(valueIndent + "      \"EnumValueDescriptor is not for this type.\");");
		printer.println(valueIndent + "  }");
		printer.println(valueIndent + "  return VALUES[desc.getIndex()];");
		printer.println(valueIndent + "}");
		printer.println();

		printer.println(valueIndent + "private final int value;");
		printer.println();

		if (ordinalIsIndex)
		{
			printer.println(valueIndent + "private " + classname + "(int value) {");
			printer.println(valueIndent + "  this.value = value;");
			printer.println(valueIndent + "}");
		}
		else
		{
			printer.println(valueIndent + "private final int index;");
			printer.println();
			printer.println(valueIndent + "private " + classname + "(int index, int value) {");
			printer.println(valueIndent + "  this.index = index;");
			printer.println(valueIndent + "  this.value = value;");
			printer.println(valueIndent + "}");
		}
		printer.println();

		printer.println(valueIndent + "// @@protoc_insertion_point(enum_scope:" + descriptor.getFullName() + ")");
		printer.println(indent + "}");
		printer.println();
	}
}
