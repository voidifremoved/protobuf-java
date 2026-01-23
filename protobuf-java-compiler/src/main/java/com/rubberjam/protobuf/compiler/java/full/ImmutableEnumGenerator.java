package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.EnumGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ImmutableEnumGenerator extends EnumGenerator
{
	private final EnumDescriptor descriptor;
	private final Context context;
	private final boolean immutableApi = true;

	private final List<EnumValueDescriptor> canonicalValues = new ArrayList<>();
	private final List<Alias> aliases = new ArrayList<>();

	private static class Alias
	{
		EnumValueDescriptor value;
		EnumValueDescriptor canonicalValue;
	}

	public ImmutableEnumGenerator(EnumDescriptor descriptor, Context context)
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
		DocComment.writeEnumDocComment(printer, descriptor, context.getOptions(), false, "  ");
		
		String classname = descriptor.getName();
		String deprecation = descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
		printer.println("  " + deprecation + "public enum " + classname);
		printer.println("      implements com.google.protobuf.ProtocolMessageEnum {");

		boolean ordinalIsIndex = true;
		for (int i = 0; i < canonicalValues.size(); i++)
		{
			if (canonicalValues.get(i).getIndex() != i)
			{
				ordinalIsIndex = false;
				break;
			}
		}

		for (int i = 0; i < canonicalValues.size(); i++)
		{
			EnumValueDescriptor value = canonicalValues.get(i);
			
			// Match C++ WriteEnumValueDocComment behavior - use DocComment utility
			// Add 4-space indentation prefix for enum values inside enum class
			DocComment.writeEnumValueDocComment(printer, value, context.getOptions(), "    ");
			
			// Add deprecation annotation if needed
			if (value.getOptions().getDeprecated())
			{
				printer.println("    @java.lang.Deprecated");
			}
			
			// Generate enum value with proper indentation (4 spaces)
			printer.print("    " + value.getName() + "(");
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
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			// Generate JavaDoc for UNRECOGNIZED manually (no EnumValueDescriptor exists for it)
			printer.println("    /**");
			printer.println("     * <code>UNRECOGNIZED = -1;</code>");
			printer.println("     */");
			printer.print("    UNRECOGNIZED(");
			if (ordinalIsIndex)
			{
				printer.print("-1");
			}
			else
			{
				printer.print("-1, -1");
			}
			printer.println("),");
		}

		// Semicolon on its own line (match C++ format)
		printer.println("    ;");
		printer.println();

		// Static block with version validator (match C++ order)
		printer.println("    static {");
		printer.println("      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(");
		printer.println("        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,");
		printer.println("        /* major= */ 4,");
		printer.println("        /* minor= */ 33,");
		printer.println("        /* patch= */ 4,");
		printer.println("        /* suffix= */ \"\",");
		printer.println("        \"" + classname + "\");");
		printer.println("    }");

		// Aliases
		for (Alias alias : aliases)
		{
			// Match C++ WriteEnumValueDocComment for aliases - use DocComment utility
			// Add 4-space indentation prefix for aliases inside enum class
			DocComment.writeEnumValueDocComment(printer, alias.value, context.getOptions(), "    ");
			printer.println("    public static final " + classname + " " + alias.value.getName() + " = "
					+ alias.canonicalValue.getName() + ";");
		}
		if (!aliases.isEmpty())
		{
			printer.println();
		}

		// Value constants
		for (EnumValueDescriptor value : descriptor.getValues())
		{
			// Match C++ WriteEnumValueDocComment for value constants - use DocComment utility
			// Add 4-space indentation prefix for value constants inside enum class
			DocComment.writeEnumValueDocComment(printer, value, context.getOptions(), "    ");
			String deprecationComment = value.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
			printer.println("    " + deprecationComment + "public static final int " + value.getName() + "_VALUE = " + value.getNumber() + ";");
		}
		printer.println();
		printer.println();

		// Standard methods (4-space indentation inside enum)
		printer.println("    public final int getNumber() {");
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("      if (this == UNRECOGNIZED) {");
			printer.println("        throw new java.lang.IllegalArgumentException(");
			printer.println("            \"Can't get the number of an unknown enum value.\");");
			printer.println("      }");
		}
		printer.println("      return value;");
		printer.println("    }");
		printer.println();

		printer.println("    /**");
		printer.println("     * @param value The numeric wire value of the corresponding enum entry.");
		printer.println("     * @return The enum associated with the given numeric wire value.");
		printer.println("     * @deprecated Use {@link #forNumber(int)} instead.");
		printer.println("     */");
		printer.println("    @java.lang.Deprecated");
		printer.println("    public static " + classname + " valueOf(int value) {");
		printer.println("      return forNumber(value);");
		printer.println("    }");
		printer.println();

		printer.println("    /**");
		printer.println("     * @param value The numeric wire value of the corresponding enum entry.");
		printer.println("     * @return The enum associated with the given numeric wire value.");
		printer.println("     */");
		printer.println("    public static " + classname + " forNumber(int value) {");
		printer.println("      switch (value) {");
		for (EnumValueDescriptor value : canonicalValues)
		{
			printer.println("        case " + value.getNumber() + ": return " + value.getName() + ";");
		}
		printer.println("        default: return null;");
		printer.println("      }");
		printer.println("    }");
		printer.println();

		// Internal map
		printer.println("    public static com.google.protobuf.Internal.EnumLiteMap<" + classname + ">");
		printer.println("        internalGetValueMap() {");
		printer.println("      return internalValueMap;");
		printer.println("    }");
		printer.println("    private static final com.google.protobuf.Internal.EnumLiteMap<");
		printer.println("        " + classname + "> internalValueMap =");
		printer.println("          new com.google.protobuf.Internal.EnumLiteMap<" + classname + ">() {");
		printer.println("            public " + classname + " findValueByNumber(int number) {");
		printer.println("              return " + classname + ".forNumber(number);");
		printer.println("            }");
		printer.println("          };");
		printer.println();

		// Reflection
		printer.println("    public final com.google.protobuf.Descriptors.EnumValueDescriptor");
		printer.println("        getValueDescriptor() {");
		// Simplified logic for proto3 UNRECOGNIZED check
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("      if (this == UNRECOGNIZED) {");
			printer.println("        throw new java.lang.IllegalStateException(");
			printer.println("            \"Can't get the descriptor of an unrecognized enum value.\");");
			printer.println("      }");
		}
		printer.println("      return getDescriptor().getValues().get(ordinal());");
		printer.println("    }");

		printer.println("    public final com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println("        getDescriptorForType() {");
		printer.println("      return getDescriptor();");
		printer.println("    }");
		printer.println("    public static com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println("        getDescriptor() {");
		if (descriptor.getContainingType() == null)
		{
			String packageName = context.getNameResolver().getFileJavaPackage(descriptor.getFile());
			String fileClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
			String outerClassName = packageName.isEmpty() ? fileClassName : packageName + "." + fileClassName;
			printer.println("      return " + outerClassName + ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentMessage = context.getNameResolver().getImmutableClassName(descriptor.getContainingType());
			printer.println("      return " + parentMessage + ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");");
		}
		printer.println("    }");
		printer.println();

		printer.println("    private static final " + classname + "[] VALUES = values();");
		printer.println();

		printer.println("    public static " + classname + " valueOf(");
		printer.println("        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {");
		printer.println("      if (desc.getType() != getDescriptor()) {");
		printer.println("        throw new java.lang.IllegalArgumentException(");
		printer.println("          \"EnumValueDescriptor is not for this type.\");");
		printer.println("      }");
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("      if (desc.getNumber() == -1) {");
			printer.println("        return UNRECOGNIZED;");
			printer.println("      }");
		}
		printer.println("      return VALUES[desc.getIndex()];");
		printer.println("    }");
		printer.println();

		printer.println("    private final int value;");
		printer.println();

		if (ordinalIsIndex)
		{
			printer.println("    private " + classname + "(int value) {");
			printer.println("      this.value = value;");
			printer.println("    }");
		}
		else
		{
			printer.println("    private final int index;");
			printer.println();
			printer.println("    private " + classname + "(int index, int value) {");
			printer.println("      this.index = index;");
			printer.println("      this.value = value;");
			printer.println("    }");
		}
		printer.println();

		printer.println("    // @@protoc_insertion_point(enum_scope:" + descriptor.getFullName() + ")");
		printer.println("  }");
		printer.println();
	}
}
