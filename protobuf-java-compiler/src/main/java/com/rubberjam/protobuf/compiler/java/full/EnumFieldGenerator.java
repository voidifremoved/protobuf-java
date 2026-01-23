package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class EnumFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final Map<String, String> variables;

	public EnumFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.variables = new HashMap<>();
		setEnumVariables(
				descriptor,
				messageBitIndex,
				builderBitIndex,
				context.getFieldGeneratorInfo(descriptor),
				variables,
				context);
	}

	@Override
	public FieldDescriptor getDescriptor()
	{
		return descriptor;
	}

	private void setEnumVariables(
			FieldDescriptor descriptor,
			int messageBitIndex,
			int builderBitIndex,
			FieldGeneratorInfo info,
			Map<String, String> variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);

		variables.put("type", context.getNameResolver().getImmutableClassName(descriptor.getEnumType()));
		variables.put("default", StringUtils.defaultValue(descriptor));
		variables.put("default_number", String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber()));

		// Simplified bit logic
		variables.put("is_field_present_message", "true");
		variables.put("set_has_field_bit_builder", "");
		variables.put("clear_has_field_bit_builder", "");

		variables.put("unknown",
				// Logic for unknown enum value support (check syntax)
				com.google.protobuf.InternalHelpers.supportUnknownEnumValue(descriptor)
						? variables.get("type") + ".UNRECOGNIZED"
						: variables.get("default"));
	}

	@Override
	public int getMessageBitIndex()
	{
		return messageBitIndex;
	}

	@Override
	public int getBuilderBitIndex()
	{
		return builderBitIndex;
	}

	@Override
	public int getNumBitsForMessage()
	{
		return com.google.protobuf.InternalHelpers.hasHasbit(descriptor) ? 1 : 0;
	}

	@Override
	public int getNumBitsForBuilder()
	{
		return 1;
	}

	@Override
	public void generateInterfaceMembers(PrintWriter printer)
	{
		printer.println("  boolean has" + variables.get("capitalized_name") + "();");
		printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
		if (supportUnknownEnumValue(descriptor))
		{
			printer.println("  int get" + variables.get("capitalized_name") + "Value();");
		}
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("  private int " + variables.get("name") + "_;");

		printer.println("  @java.lang.Override");
		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		printer.println("    return false;"); // TODO
		printer.println("  }");

		if (supportUnknownEnumValue(descriptor))
		{
			printer.println("  @java.lang.Override");
			printer.println("  public int get" + variables.get("capitalized_name") + "Value() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");
		}

		printer.println("  @java.lang.Override");
		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("type") + " result = " + variables.get("type") + ".forNumber("
				+ variables.get("name") + "_);");
		printer.println("    return result == null ? " + variables.get("unknown") + " : result;");
		printer.println("  }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		printer.println("  private int " + variables.get("name") + "_ = " + variables.get("default_number") + ";");

		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		printer.println("    return false;"); // TODO
		printer.println("  }");

		if (supportUnknownEnumValue(descriptor))
		{
			printer.println("  public int get" + variables.get("capitalized_name") + "Value() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  public Builder set" + variables.get("capitalized_name") + "Value(int value) {");
			printer.println("    " + variables.get("name") + "_ = value;");
			printer.println("    return this;");
			printer.println("  }");
		}

		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("type") + " result = " + variables.get("type") + ".forNumber("
				+ variables.get("name") + "_);");
		printer.println("    return result == null ? " + variables.get("unknown") + " : result;");
		printer.println("  }");

		printer.println("  public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		printer.println("    if (value == null) { throw new NullPointerException(); }");
		printer.println("    " + variables.get("name") + "_ = value.getNumber();");
		printer.println("    return this;");
		printer.println("  }");

		printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		printer.println("    return this;");
		printer.println("  }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("      result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("          .computeEnumSize(" + variables.get("number") + ", " + variables.get("name") + "_);");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        output.writeEnum(" + variables.get("number") + ", " + variables.get("name") + "_);");
		printer.println("      }");
	}

	@Override
	public void generateFieldBuilderInitializationCode(PrintWriter printer)
	{
		// no-op
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public String getBoxedType()
	{
		return variables.get("type");
	}

	private boolean supportUnknownEnumValue(FieldDescriptor descriptor)
	{
		return com.google.protobuf.InternalHelpers.supportUnknownEnumValue(descriptor);
	}
}
