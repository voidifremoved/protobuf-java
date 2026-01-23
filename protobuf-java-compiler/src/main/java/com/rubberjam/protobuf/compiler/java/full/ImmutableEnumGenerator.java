package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
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
		// Match C++ WriteEnumDocComment behavior
		printer.println("  /**");
		printer.println("   * Protobuf enum {@code " + descriptor.getFullName() + "}");
		printer.println("   */");
		
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
			
			// Match C++ WriteEnumValueDocComment behavior
			printer.println("    /**");
			// Generate the enum value definition comment: <code>NAME = NUMBER;</code>
			String valueDef = value.getName() + " = " + value.getNumber() + ";";
			printer.println("     * <code>" + valueDef + "</code>");
			printer.println("     */");
			
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
			// Match C++ WriteEnumValueDocComment for aliases
			printer.println("    /**");
			String aliasDef = alias.value.getName() + " = " + alias.value.getNumber() + ";";
			printer.println("     * <code>" + aliasDef + "</code>");
			printer.println("     */");
			printer.println("    public static final " + classname + " " + alias.value.getName() + " = "
					+ alias.canonicalValue.getName() + ";");
		}

		// Value constants
		for (EnumValueDescriptor value : descriptor.getValues())
		{
			// Match C++ WriteEnumValueDocComment for value constants
			printer.println("    /**");
			String valueDef = value.getName() + " = " + value.getNumber() + ";";
			printer.println("     * <code>" + valueDef + "</code>");
			printer.println("     */");
			String deprecationComment = value.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";
			printer.println("    " + deprecationComment + "public static final int " + value.getName() + "_VALUE = " + value.getNumber() + ";");
		}
		printer.println();

		// Standard methods
		printer.println("  public final int getNumber() {");
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("    if (this == UNRECOGNIZED) {");
			printer.println("      throw new java.lang.IllegalArgumentException(");
			printer.println("          \"Can't get the number of an unknown enum value.\");");
			printer.println("    }");
		}
		printer.println("    return value;");
		printer.println("  }");

		printer.println("  public static " + classname + " forNumber(int value) {");
		printer.println("    switch (value) {");
		for (EnumValueDescriptor value : canonicalValues)
		{
			printer.println("      case " + value.getNumber() + ": return " + value.getName() + ";");
		}
		printer.println("      default: return null;");
		printer.println("    }");
		printer.println("  }");

		// Internal map
		printer.println("  public static com.google.protobuf.Internal.EnumLiteMap<" + classname + ">");
		printer.println("      internalGetValueMap() {");
		printer.println("    return internalValueMap;");
		printer.println("  }");
		printer.println("  private static final com.google.protobuf.Internal.EnumLiteMap<");
		printer.println("      " + classname + "> internalValueMap =");
		printer.println("        new com.google.protobuf.Internal.EnumLiteMap<" + classname + ">() {");
		printer.println("          public " + classname + " findValueByNumber(int number) {");
		printer.println("            return " + classname + ".forNumber(number);");
		printer.println("          }");
		printer.println("        };");

		// Reflection
		printer.println("  public final com.google.protobuf.Descriptors.EnumValueDescriptor");
		printer.println("      getValueDescriptor() {");
		// Simplified logic for proto3 UNRECOGNIZED check
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("    if (this == UNRECOGNIZED) {");
			printer.println("      throw new java.lang.IllegalStateException(");
			printer.println("          \"Can't get the descriptor of an unrecognized enum value.\");");
			printer.println("    }");
		}
		printer.println("    return getDescriptor().getValues().get(ordinal());");
		printer.println("  }");

		printer.println("  public final com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println("      getDescriptorForType() {");
		printer.println("    return getDescriptor();");
		printer.println("  }");

		printer.println("  public static final com.google.protobuf.Descriptors.EnumDescriptor");
		printer.println("      getDescriptor() {");
		// TODO: Handle nested vs top-level descriptor access correctly
		printer.println("    return " + context.getNameResolver().getFileClassName(descriptor.getFile(), true)
				+ ".getDescriptor().getEnumTypes().get(" + descriptor.getIndex() + ");");
		printer.println("  }");

		printer.println("  private static final " + classname + "[] VALUES = values();");

		printer.println("  public static " + classname + " valueOf(");
		printer.println("      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {");
		printer.println("    if (desc.getType() != getDescriptor()) {");
		printer.println("      throw new java.lang.IllegalArgumentException(");
		printer.println("        \"EnumValueDescriptor is not for this type.\");");
		printer.println("    }");
		if (!descriptor.getFile().toProto().getSyntax().equals("proto2"))
		{
			printer.println("    if (desc.getIndex() == -1) {");
			printer.println("      return UNRECOGNIZED;");
			printer.println("    }");
		}
		printer.println("    return VALUES[desc.getIndex()];");
		printer.println("  }");

		printer.println("  private final int value;");
		if (ordinalIsIndex)
		{
			printer.println("  private " + classname + "(int value) {");
			printer.println("    this.value = value;");
			printer.println("  }");
		}
		else
		{
			printer.println("  private final int index;");
			printer.println("  private " + classname + "(int index, int value) {");
			printer.println("    this.index = index;");
			printer.println("    this.value = value;");
			printer.println("  }");
		}

		printer.println("}");
	}
}
