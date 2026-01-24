package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.StringUtils;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.Helpers;

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
	private final int fieldNumber;

	public EnumFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
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
	public int getFieldNumber()
	{
		return this.fieldNumber;
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
		variables.put("default", variables.get("type") + "."
				+ ((com.google.protobuf.Descriptors.EnumValueDescriptor) descriptor.getDefaultValue()).getName());
		variables.put("default_number", String.valueOf(descriptor.getEnumType().getValues().get(0).getNumber()));

		if (descriptor.hasPresence())
		{
			variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
			variables.put("is_other_field_present_message", "other.has" + variables.get("capitalized_name") + "()");
			variables.put("set_has_field_bit_to_local", Helpers.generateSetBitToLocal(messageBitIndex) + ";");
			variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
			variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
		}
		else
		{
			variables.put("is_field_present_message", variables.get("name") + "_ != " + variables.get("default_number"));
			variables.put("is_other_field_present_message", "other.get" + variables.get("capitalized_name") + "Value() != " + variables.get("default_number"));
			variables.put("set_has_field_bit_builder", "");
			variables.put("clear_has_field_bit_builder", "");
		}
		variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));

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
		if (descriptor.hasPresence())
		{
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.HAZZER,
							context,
							false,
							false,
							false));
			printer.println("    boolean has" + variables.get("capitalized_name") + "();");
		}
		if (supportUnknownEnumValue(descriptor))
		{
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.GETTER,
							context,
							false,
							false,
							false));
			printer.println("    int get" + variables.get("capitalized_name") + "Value();");
		}
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		printer.println("    " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("  private int " + variables.get("name") + "_ = " + variables.get("default_number") + ";");

		if (descriptor.hasPresence())
		{
			Helpers.writeDocComment(
					printer,
					"  ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.HAZZER,
							context,
							false,
							false,
							false));
			printer.println("  @java.lang.Override public boolean has" + variables.get("capitalized_name") + "() {");
			printer.println("    return " + variables.get("is_field_present_message") + ";");
			printer.println("  }");
		}

		if (supportUnknownEnumValue(descriptor))
		{
			Helpers.writeDocComment(
					printer,
					"  ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.GETTER,
							context,
							false,
							false,
							false));
			printer.println("  @java.lang.Override public int get" + variables.get("capitalized_name") + "Value() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");
		}

		Helpers.writeDocComment(
				printer,
				"  ",
				commentWriter -> DocComment.writeFieldAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		printer.println("  @java.lang.Override public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("type") + " result = " + variables.get("type") + ".forNumber("
				+ variables.get("name") + "_);");
		printer.println("    return result == null ? " + variables.get("unknown") + " : result;");
		printer.println("  }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		if (descriptor.getContainingOneof() == null)
		{
			printer.println("  private int " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		}

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
		if (descriptor.getContainingOneof() == null)
		{
			printer.println("    " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		}
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		printer.println("        if (" + variables.get("is_other_field_present_message") + ") {");
		printer.println("          set" + variables.get("capitalized_name") + "(other.get"
				+ variables.get("capitalized_name") + "());");
		printer.println("        }");
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("      if (" + variables.get("get_has_field_bit_from_local") + ") {");
			printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
			if (getNumBitsForMessage() > 0)
			{
				printer.println("        " + variables.get("set_has_field_bit_to_local"));
			}
			printer.println("      }");
		}
		else
		{
			printer.println("      result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
		}
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		if (supportUnknownEnumValue(descriptor))
		{
			printer.println("                " + variables.get("name") + "_ = input.readEnum();");
			printer.println("                " + variables.get("set_has_field_bit_builder"));
		}
		else
		{
			printer.println("                int tmpRaw = input.readEnum();");
			printer.println("                " + variables.get("type") + " tmpValue =");
			printer.println("                    " + variables.get("type") + ".forNumber(tmpRaw);");
			printer.println("                if (tmpValue == null) {");
			printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
			printer.println("                } else {");
			printer.println("                  " + variables.get("name") + "_ = tmpRaw;");
			printer.println("                  " + variables.get("set_has_field_bit_builder"));
			printer.println("                }");
		}
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
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.get("capitalized_name") + "() != other.has" + variables.get("capitalized_name") + "()) return false;");
			printer.println("      if (has" + variables.get("capitalized_name") + "()) {");
			printer.println("        if (" + variables.get("name") + "_ != other." + variables.get("name") + "_) return false;");
			printer.println("      }");
		}
		else
		{
			printer.println("      if (" + variables.get("name") + "_ != other." + variables.get("name") + "_) return false;");
		}
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.get("capitalized_name") + "()) {");
		}
		printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("        hash = (53 * hash) + " + variables.get("name") + "_;");
		if (descriptor.hasPresence())
		{
			printer.println("      }");
		}
	}

	@Override
	public void generateOneofEqualsCode(PrintWriter printer)
	{
		printer.println("          if (" + variables.get("name") + "_ != other." + variables.get("name") + "_) return false;");
	}

	@Override
	public void generateOneofHashCode(PrintWriter printer)
	{
		printer.println("          hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("          hash = (53 * hash) + " + variables.get("name") + "_;");
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
