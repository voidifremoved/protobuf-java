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
		variables.put("default_number", String.valueOf(
				((com.google.protobuf.Descriptors.EnumValueDescriptor) descriptor.getDefaultValue()).getNumber()));

		variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
		variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = StringUtils.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.put("oneof_name", oneofName);
			variables.put("oneof_case_variable", oneofName + "Case_");
			variables.put("oneof_field_variable", oneofName + "_");
			variables.put("is_field_present_message", oneofName + "Case_ == " + descriptor.getNumber());
			variables.put("is_other_field_present_message", "other.has" + variables.get("capitalized_name") + "()");
			variables.put("set_has_field_bit_to_local", "");
		}
		else if (descriptor.hasPresence())
		{
			variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
			variables.put("is_other_field_present_message", "other.has" + variables.get("capitalized_name") + "()");
			variables.put("set_has_field_bit_to_local", Helpers.generateSetBitToLocal(messageBitIndex) + ";");
		}
		else
		{
			variables.put("is_field_present_message", variables.get("name") + "_ != " + variables.get("default_number"));
			variables.put("is_other_field_present_message", "other.get" + variables.get("capitalized_name") + "Value() != " + variables.get("default_number"));
			variables.put("set_has_field_bit_to_local", "");
		}
		variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.put("on_changed", "onChanged();");

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
							FieldAccessorType.VALUE_GETTER,
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
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    private int " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		}

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
			if (descriptor.getContainingOneof() == null || isSynthetic)
			{
				printer.println("    @java.lang.Override public boolean has" + variables.get("capitalized_name") + "() {");
			}
			else
			{
				printer.println("    public boolean has" + variables.get("capitalized_name") + "() {");
			}
			printer.println("      return " + variables.get("is_field_present_message") + ";");
			printer.println("    }");
		}

		if (supportUnknownEnumValue(descriptor))
		{
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.VALUE_GETTER,
							context,
							false,
							false,
							false));
			if (descriptor.getContainingOneof() == null || isSynthetic)
			{
				printer.println("    @java.lang.Override public int get" + variables.get("capitalized_name") + "Value() {");
			}
			else
			{
				printer.println("    public int get" + variables.get("capitalized_name") + "Value() {");
			}
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("      if (" + variables.get("is_field_present_message") + ") {");
				printer.println("        return (java.lang.Integer) " + variables.get("oneof_field_variable") + ";");
				printer.println("      }");
				printer.println("      return " + variables.get("default_number") + ";");
			}
			else
			{
				printer.println("      return " + variables.get("name") + "_;");
			}
			printer.println("    }");
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
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    @java.lang.Override public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		}
		else
		{
			printer.println("    public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		}
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      if (" + variables.get("is_field_present_message") + ") {");
			printer.println("        " + variables.get("type") + " result = " + variables.get("type") + ".forNumber(");
			printer.println("            (java.lang.Integer) " + variables.get("oneof_field_variable") + ");");
			printer.println("        return result == null ? " + variables.get("unknown") + " : result;");
			printer.println("      }");
			printer.println("      return " + variables.get("default") + ";");
		}
		else
		{
			printer.println("      " + variables.get("type") + " result = " + variables.get("type") + ".forNumber("
					+ variables.get("name") + "_);");
			printer.println("      return result == null ? " + variables.get("unknown") + " : result;");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("      private int " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		}

		if (descriptor.hasPresence())
		{
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.HAZZER,
							context,
							false,
							false,
							false));
			

			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("      @java.lang.Override");
				printer.println("      public boolean has" + variables.get("capitalized_name") + "() {");
				printer.println("        return " + variables.get("is_field_present_message") + ";");
			}
			else
			{
				printer.println("      public boolean has" + variables.get("capitalized_name") + "() {");
				printer.println("        return " + variables.get("get_has_field_bit_builder") + ";");
			}
			printer.println("      }");
		}

		if (supportUnknownEnumValue(descriptor))
		{
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.VALUE_GETTER,
							context,
							false,
							false,
							false));
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("      @java.lang.Override");
				printer.println("      public int get" + variables.get("capitalized_name") + "Value() {");
				printer.println("        if (" + variables.get("is_field_present_message") + ") {");
				printer.println("          return ((java.lang.Integer) " + variables.get("oneof_field_variable") + ").intValue();");
				printer.println("        }");
				printer.println("        return " + variables.get("default_number") + ";");
				printer.println("      }");
			}
			else
			{
				printer.println("      @java.lang.Override public int get" + variables.get("capitalized_name") + "Value() {");
				printer.println("        return " + variables.get("name") + "_;");
				printer.println("      }");
			}

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.VALUE_SETTER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder set" + variables.get("capitalized_name") + "Value(int value) {");
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
				printer.println("        " + variables.get("oneof_field_variable") + " = value;");
			}
			else
			{
				printer.println("        " + variables.get("name") + "_ = value;");
				printer.println("        " + variables.get("set_has_field_bit_builder"));
			}
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");
		}

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		printer.println("      @java.lang.Override");
		printer.println("      public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.get("is_field_present_message") + ") {");
			printer.println("          " + variables.get("type") + " result = " + variables.get("type") + ".forNumber(");
			printer.println("              (java.lang.Integer) " + variables.get("oneof_field_variable") + ");");
			printer.println("          return result == null ? " + variables.get("unknown") + " : result;");
			printer.println("        }");
			printer.println("        return " + variables.get("default") + ";");
		}
		else
		{
			printer.println("        " + variables.get("type") + " result = " + variables.get("type") + ".forNumber("
					+ variables.get("name") + "_);");
			printer.println("        return result == null ? " + variables.get("unknown") + " : result;");
		}
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.SETTER,
						context,
						true,
						false,
						false));
		printer.println("      public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		printer.println("        if (value == null) { throw new NullPointerException(); }");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
			printer.println("        " + variables.get("oneof_field_variable") + " = value.getNumber();");
		}
		else
		{
			printer.println("        " + variables.get("set_has_field_bit_builder"));
			printer.println("        " + variables.get("name") + "_ = value.getNumber();");
		}
		printer.println("        " + variables.get("on_changed"));
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.CLEARER,
						context,
						true,
						false,
						false));
		printer.println("      public Builder clear" + variables.get("capitalized_name") + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.get("is_field_present_message") + ") {");
			printer.println("          " + variables.get("oneof_case_variable") + " = 0;");
			printer.println("          " + variables.get("oneof_field_variable") + " = null;");
			printer.println("          " + variables.get("on_changed"));
			printer.println("        }");
		}
		else
		{
			printer.println("        " + variables.get("clear_has_field_bit_builder"));
			printer.println("        " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return this;");
		printer.println("      }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (isRealOneof)
		{
			return;
		}
		printer.println("      " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("        " + variables.get("name") + "_ = " + variables.get("default_number") + ";");
		}
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("          set" + variables.get("capitalized_name") + "(other.get"
					+ variables.get("capitalized_name") + "());");
		}
		else
		{
			printer.println("        if (" + variables.get("is_other_field_present_message") + ") {");
			printer.println("          set" + variables.get("capitalized_name") + "(other.get"
					+ variables.get("capitalized_name") + "());");
			printer.println("        }");
		}
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			return;
		}
		printer.println("        if (" + variables.get("get_has_field_bit_from_local") + ") {");
		printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
		if (getNumBitsForMessage() > 0)
		{
			printer.println("          " + variables.get("set_has_field_bit_to_local"));
		}
		printer.println("        }");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			if (supportUnknownEnumValue(descriptor))
			{
				printer.println("                " + variables.get("oneof_field_variable") + " = input.readEnum();");
				printer.println("                " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
			}
			else
			{
				printer.println("                int rawValue = input.readEnum();");
				printer.println("                " + variables.get("type") + " value =");
				printer.println("                    " + variables.get("type") + ".forNumber(rawValue);");
				printer.println("                if (value == null) {");
				printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", rawValue);");
				printer.println("                } else {");
				printer.println("                  " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
				printer.println("                  " + variables.get("oneof_field_variable") + " = rawValue;");
				printer.println("                }");
			}
		}
		else
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
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		String valueVar = variables.get("name") + "_";
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			valueVar = "(java.lang.Integer) " + variables.get("oneof_field_variable");
		}
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("          .computeEnumSize(" + variables.get("number") + ", (" + valueVar + "));");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		String valueVar = variables.get("name") + "_";
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			valueVar = "(java.lang.Integer) " + variables.get("oneof_field_variable");
		}
		printer.println("        output.writeEnum(" + variables.get("number") + ", (" + valueVar + "));");
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

	public static class RepeatedEnumFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final Map<String, String> variables;
		private final int fieldNumber;

		public RepeatedEnumFieldGenerator(
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
			variables.put("boxed_type", variables.get("type"));
			variables.put("empty_list", "emptyIntList()");
			variables.put("field_list_type", "com.google.protobuf.Internal.IntList");
			variables.put("repeated_get", variables.get("name") + "_.getInt");
			variables.put("repeated_add", variables.get("name") + "_.addInt");
			variables.put("repeated_set", variables.get("name") + "_.setInt");
			variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
			variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
			variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
			variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
			variables.put("on_changed", "onChanged();");
			variables.put("capitalized_type", Helpers.getCapitalizedType(descriptor));
			variables.put("default", variables.get("type") + "."
					+ descriptor.getEnumType().getValues().get(0).getName());
			variables.put("unknown",
					com.google.protobuf.InternalHelpers.supportUnknownEnumValue(descriptor)
							? variables.get("type") + ".UNRECOGNIZED"
							: variables.get("default"));
		}

		@Override
		public int getMessageBitIndex()
		{
			return 0;
		}

		@Override
		public int getBuilderBitIndex()
		{
			return builderBitIndex;
		}

		@Override
		public int getNumBitsForMessage()
		{
			return 0;
		}

		@Override
		public int getNumBitsForBuilder()
		{
			return 1;
		}

		@Override
		public void generateInterfaceMembers(PrintWriter printer)
		{
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_GETTER,
							context,
							false,
							false,
							false));
			printer.println(
					"    java.util.List<" + variables.get("boxed_type") + "> get" + variables.get("capitalized_name") + "List();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_COUNT,
							context,
							false,
							false,
							false));
			printer.println("    int get" + variables.get("capitalized_name") + "Count();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index);");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println(
					"    java.util.List<java.lang.Integer>");
			printer.println("    get" + variables.get("capitalized_name") + "ValueList();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    int get" + variables.get("capitalized_name") + "Value(int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private " + variables.get("field_list_type") + " " + variables.get("name") + "_ =");
			printer.println("        " + variables.get("empty_list") + ";");

			// Converter field
			printer.println("    private static final     com.google.protobuf.Internal.IntListAdapter.IntConverter<");
			printer.println("        " + variables.get("type") + "> " + variables.get("name") + "_converter_ =");
			printer.println("            new com.google.protobuf.Internal.IntListAdapter.IntConverter<");
			printer.println("                " + variables.get("type") + ">() {");
			printer.println("              public " + variables.get("type") + " convert(int from) {");
			printer.println("                " + variables.get("type") + " result = " + variables.get("type") + ".forNumber(from);");
			printer.println("                return result == null ? " + variables.get("unknown") + " : result;");
			printer.println("              }");
			printer.println("            };");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    public java.util.List<" + variables.get("boxed_type") + "> get"
					+ variables.get("capitalized_name") + "List() {");
			printer.println("      return new com.google.protobuf.Internal.IntListAdapter<");
			printer.println("          " + variables.get("type") + ">(" + variables.get("name") + "_, " + variables.get("name") + "_converter_);");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_COUNT,
							context,
							false,
							false,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("      return " + variables.get("name") + "_.size();");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("      return " + variables.get("name") + "_converter_.convert(" + variables.get("name") + "_.getInt(index));");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    public java.util.List<java.lang.Integer>");
			printer.println("    get" + variables.get("capitalized_name") + "ValueList() {");
			printer.println("      return " + variables.get("name") + "_;");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    public int get" + variables.get("capitalized_name") + "Value(int index) {");
			printer.println("      return " + variables.get("name") + "_.getInt(index);");
			printer.println("    }");

			if (descriptor.isPacked())
			{
				printer.println("    private int " + variables.get("name") + "MemoizedSerializedSize;");
			}
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("      private " + variables.get("field_list_type") + " " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");

			printer.println("      private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("        if (!" + variables.get("name") + "_.isModifiable()) {");
			printer.println("          " + variables.get("name") + "_ = makeMutableCopy(" + variables.get("name") + "_);");
			printer.println("        }");
			printer.println("        " + variables.get("set_has_field_bit_builder"));
			printer.println("      }");

			// ... (Copy accessor logic from generateMembers but make it public and without Override)
			// Actually Builder accessors delegate to the list.

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_GETTER,
							context,
							false,
							false,
							false));
			printer.println("      public java.util.List<" + variables.get("boxed_type") + "> get"
					+ variables.get("capitalized_name") + "List() {");
			printer.println("        return new com.google.protobuf.Internal.IntListAdapter<");
			printer.println("            " + variables.get("type") + ">(" + variables.get("name") + "_, " + variables.get("name") + "_converter_);");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_COUNT,
							context,
							false,
							false,
							false));
			printer.println("      public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("        return " + variables.get("name") + "_.size();");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("      public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("        return " + variables.get("name") + "_converter_.convert(" + variables.get("name") + "_.getInt(index));");
			printer.println("      }");

			// Setters
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_SETTER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder set" + variables.get("capitalized_name") + "(");
			printer.println("          int index, " + variables.get("type") + " value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        " + variables.get("name") + "_.setInt(index, value.getNumber());");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_ADDER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder add" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        " + variables.get("name") + "_.addInt(value.getNumber());");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_MULTI_ADDER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder addAll" + variables.get("capitalized_name") + "(");
			printer.println("          java.lang.Iterable<? extends " + variables.get("boxed_type") + "> values) {");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        for (" + variables.get("boxed_type") + " value : values) {");
			printer.println("          " + variables.get("name") + "_.addInt(value.getNumber());");
			printer.println("        }");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.CLEARER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder clear" + variables.get("capitalized_name") + "() {");
			printer.println("        " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
			printer.println("        " + variables.get("clear_has_field_bit_builder"));
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println("      public java.util.List<java.lang.Integer>");
			printer.println("      get" + variables.get("capitalized_name") + "ValueList() {");
			printer.println("        " + variables.get("name") + "_.makeImmutable();");
			printer.println("        return " + variables.get("name") + "_;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_VALUE_GETTER,
							context,
							false,
							false,
							false));
			printer.println("      public int get" + variables.get("capitalized_name") + "Value(int index) {");
			printer.println("        return " + variables.get("name") + "_.getInt(index);");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_VALUE_SETTER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder set" + variables.get("capitalized_name") + "Value(");
			printer.println("          int index, int value) {");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        " + variables.get("repeated_set") + "(index, value);");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_VALUE_ADDER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder add" + variables.get("capitalized_name") + "Value(int value) {");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        " + variables.get("repeated_add") + "(value);");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_VALUE_MULTI_ADDER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder addAll" + variables.get("capitalized_name") + "Value(");
			printer.println("          java.lang.Iterable<java.lang.Integer> values) {");
			printer.println("        ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("        for (int value : values) {");
			printer.println("          " + variables.get("repeated_add") + "(value);");
			printer.println("        }");
			printer.println("        " + variables.get("on_changed"));
			printer.println("        return this;");
			printer.println("      }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("      " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("        " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			printer.println("        if (!other." + variables.get("name") + "_.isEmpty()) {");
			printer.println("          if (" + variables.get("name") + "_.isEmpty()) {");
			printer.println("            " + variables.get("name") + "_ = other." + variables.get("name") + "_;");
			printer.println("            " + variables.get("name") + "_.makeImmutable();");
			printer.println("            " + variables.get("set_has_field_bit_builder"));
			printer.println("          } else {");
			printer.println("            ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("            " + variables.get("name") + "_.addAll(other." + variables.get("name") + "_);");
			printer.println("          }");
			printer.println("          onChanged();");
			printer.println("        }");
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.get("get_has_field_bit_from_local") + ") {");
			printer.println("          " + variables.get("name") + "_.makeImmutable();");
			printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
			printer.println("        }");
		}

		@Override
		public void generateBuilderParsingCode(PrintWriter printer)
		{
			if (com.google.protobuf.InternalHelpers.supportUnknownEnumValue(descriptor))
			{
				printer.println("                int tmpRaw = input.readEnum();");
				printer.println("                ensure" + variables.get("capitalized_name") + "IsMutable();");
				printer.println("                " + variables.get("repeated_add") + "(tmpRaw);");
			}
			else
			{
				printer.println("                int tmpRaw = input.readEnum();");
				printer.println("                " + variables.get("type") + " tmpValue =");
				printer.println("                    " + variables.get("type") + ".forNumber(tmpRaw);");
				printer.println("                if (tmpValue == null) {");
				printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
				printer.println("                } else {");
				printer.println("                  ensure" + variables.get("capitalized_name") + "IsMutable();");
				printer.println("                  " + variables.get("repeated_add") + "(tmpRaw);");
				printer.println("                }");
			}
		}

		@Override
		public void generateBuilderParsingCodeFromPacked(PrintWriter printer)
		{
			printer.println("                int length = input.readRawVarint32();");
			printer.println("                int limit = input.pushLimit(length);");
			printer.println("                ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("                while (input.getBytesUntilLimit() > 0) {");
			printer.println("                  int tmpRaw = input.readEnum();");
			if (com.google.protobuf.InternalHelpers.supportUnknownEnumValue(descriptor))
			{
				printer.println("                  " + variables.get("repeated_add") + "(tmpRaw);");
			}
			else
			{
				printer.println("                  " + variables.get("type") + " tmpValue =");
				printer.println("                      " + variables.get("type") + ".forNumber(tmpRaw);");
				printer.println("                  if (tmpValue == null) {");
				printer.println("                    mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
				printer.println("                  } else {");
				printer.println("                    " + variables.get("repeated_add") + "(tmpRaw);");
				printer.println("                  }");
			}
			printer.println("                }");
			printer.println("                input.popLimit(limit);");
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			printer.println("      {");
			printer.println("        int dataSize = 0;");
			printer.println("        for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("          dataSize += com.google.protobuf.CodedOutputStream");
			printer.println("            .computeEnumSizeNoTag(" + variables.get("name") + "_.getInt(i));");
			printer.println("        }");
			printer.println("        size += dataSize;");
			if (descriptor.isPacked())
			{
				printer.println("        if (!" + variables.get("name") + "_.isEmpty()) {");
				printer.println("          size += " + Helpers.getTagSize(descriptor) + ";");
				printer.println("          size += com.google.protobuf.CodedOutputStream");
				printer.println("            .computeUInt32SizeNoTag(dataSize);");
				printer.println("        }");
				printer.println("        " + variables.get("name") + "MemoizedSerializedSize = dataSize;");
			}
			else
			{
				printer.println("        size += " + Helpers.getTagSize(descriptor) + " * " + variables.get("name") + "_.size();");
			}
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			if (descriptor.isPacked())
			{
				printer.println("      if (" + variables.get("name") + "_.size() > 0) {");
				printer.println("        output.writeUInt32NoTag(" + Helpers.getTag(descriptor) + ");");
				printer.println("        output.writeUInt32NoTag(" + variables.get("name") + "MemoizedSerializedSize);");
				printer.println("      }");
				printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
				printer.println("        output.writeEnumNoTag(" + variables.get("name") + "_.getInt(i));");
				printer.println("      }");
			}
			else
			{
				printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
				printer.println("        output.writeEnum(" + variables.get("number") + ", " + variables.get("name") + "_.getInt(i));");
				printer.println("      }");
			}
		}

		@Override
		public void generateFieldBuilderInitializationCode(PrintWriter printer)
		{
			// no-op
		}

		@Override
		public void generateEqualsCode(PrintWriter printer)
		{
			printer.println("      if (!" + variables.get("name") + "_.equals(other." + variables.get("name")
					+ "_)) return false;");
		}

		@Override
		public void generateHashCode(PrintWriter printer)
		{
			printer.println("      if (get" + variables.get("capitalized_name") + "Count() > 0) {");
			printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
			printer.println("        hash = (53 * hash) + " + variables.get("name") + "_.hashCode();");
			printer.println("      }");
		}

		@Override
		public void generateOneofEqualsCode(PrintWriter printer)
		{
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void generateOneofHashCode(PrintWriter printer)
		{
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void generateSerializationCode(PrintWriter printer)
		{
			// Placeholder
		}

		@Override
		public String getBoxedType()
		{
			return variables.get("boxed_type");
		}
	}
}
