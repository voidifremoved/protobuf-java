package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.ContextVariables;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.StringUtils;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.Helpers;

import java.io.PrintWriter;



public class EnumFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final ContextVariables variables;
	private final int fieldNumber;

	public EnumFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
		this.variables = new ContextVariables();
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
			ContextVariables variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);

		variables.setType( context.getNameResolver().getImmutableClassName(descriptor.getEnumType()));
		variables.setDefaultValue( variables.getType() + "."
				+ ((com.google.protobuf.Descriptors.EnumValueDescriptor) descriptor.getDefaultValue()).getName());
		variables.setDefaultNumber( String.valueOf(
				((com.google.protobuf.Descriptors.EnumValueDescriptor) descriptor.getDefaultValue()).getNumber()));

		variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
		variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = StringUtils.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.setOneofName( oneofName);
			variables.setOneofCaseVariable( oneofName + "Case_");
			variables.setOneofFieldVariable( oneofName + "_");
			variables.setIsFieldPresentMessage( oneofName + "Case_ == " + descriptor.getNumber());
			variables.setIsOtherFieldPresentMessage( "other.has" + variables.getCapitalizedName() + "()");
			variables.setSetHasFieldBitToLocal( "");
		}
		else if (descriptor.hasPresence())
		{
			variables.setIsFieldPresentMessage( Helpers.generateGetBit(messageBitIndex));
			variables.setIsOtherFieldPresentMessage( "other.has" + variables.getCapitalizedName() + "()");
			variables.setSetHasFieldBitToLocal( Helpers.generateSetBitToLocal(messageBitIndex) + ";");
		}
		else
		{
			variables.setIsFieldPresentMessage( variables.getName() + "_ != " + variables.getDefaultNumber());
			variables.setIsOtherFieldPresentMessage( "other.get" + variables.getCapitalizedName() + "Value() != " + variables.getDefaultNumber());
			variables.setSetHasFieldBitToLocal( "");
		}
		variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.setOnChanged( "onChanged();");

		variables.setUnknown(
				// Logic for unknown enum value support (check syntax)
				Helpers.supportUnknownEnumValue(descriptor)
						? variables.getType() + ".UNRECOGNIZED"
						: variables.getDefaultValue());
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
			printer.println("    boolean has" + variables.getCapitalizedName() + "();");
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
			printer.println("    int get" + variables.getCapitalizedName() + "Value();");
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
		printer.println("    " + variables.getType() + " get" + variables.getCapitalizedName() + "();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    private int " + variables.getName() + "_ = " + variables.getDefaultNumber() + ";");
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
				printer.println("    @java.lang.Override public boolean has" + variables.getCapitalizedName() + "() {");
			}
			else
			{
				printer.println("    public boolean has" + variables.getCapitalizedName() + "() {");
			}
			printer.println("      return " + variables.getIsFieldPresentMessage() + ";");
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
				printer.println("    @java.lang.Override public int get" + variables.getCapitalizedName() + "Value() {");
			}
			else
			{
				printer.println("    public int get" + variables.getCapitalizedName() + "Value() {");
			}
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
				printer.println("        return (java.lang.Integer) " + variables.getOneofFieldVariable() + ";");
				printer.println("      }");
				printer.println("      return " + variables.getDefaultNumber() + ";");
			}
			else
			{
				printer.println("      return " + variables.getName() + "_;");
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
			printer.println("    @java.lang.Override public " + variables.getType() + " get" + variables.getCapitalizedName() + "() {");
		}
		else
		{
			printer.println("    public " + variables.getType() + " get" + variables.getCapitalizedName() + "() {");
		}
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("        " + variables.getType() + " result = " + variables.getType() + ".forNumber(");
			printer.println("            (java.lang.Integer) " + variables.getOneofFieldVariable() + ");");
			printer.println("        return result == null ? " + variables.getUnknown() + " : result;");
			printer.println("      }");
			printer.println("      return " + variables.getDefaultValue() + ";");
		}
		else
		{
			printer.println("      " + variables.getType() + " result = " + variables.getType() + ".forNumber("
					+ variables.getName() + "_);");
			printer.println("      return result == null ? " + variables.getUnknown() + " : result;");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("      private int " + variables.getName() + "_ = " + variables.getDefaultNumber() + ";");
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
				printer.println("      public boolean has" + variables.getCapitalizedName() + "() {");
				printer.println("        return " + variables.getIsFieldPresentMessage() + ";");
			}
			else
			{
				printer.println("      @java.lang.Override public boolean has" + variables.getCapitalizedName() + "() {");
				printer.println("        return " + variables.getGetHasFieldBitBuilder() + ";");
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
				printer.println("      public int get" + variables.getCapitalizedName() + "Value() {");
				printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
				printer.println("          return ((java.lang.Integer) " + variables.getOneofFieldVariable() + ").intValue();");
				printer.println("        }");
				printer.println("        return " + variables.getDefaultNumber() + ";");
				printer.println("      }");
			}
			else
			{
				printer.println("      @java.lang.Override public int get" + variables.getCapitalizedName() + "Value() {");
				printer.println("        return " + variables.getName() + "_;");
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
			printer.println("      public Builder set" + variables.getCapitalizedName() + "Value(int value) {");
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
				printer.println("        " + variables.getOneofFieldVariable() + " = value;");
			}
			else
			{
				printer.println("        " + variables.getName() + "_ = value;");
				printer.println("        " + variables.getSetHasFieldBitBuilder());
			}
			printer.println("        " + variables.getOnChanged());
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
		printer.println("      public " + variables.getType() + " get" + variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          " + variables.getType() + " result = " + variables.getType() + ".forNumber(");
			printer.println("              (java.lang.Integer) " + variables.getOneofFieldVariable() + ");");
			printer.println("          return result == null ? " + variables.getUnknown() + " : result;");
			printer.println("        }");
			printer.println("        return " + variables.getDefaultValue() + ";");
		}
		else
		{
			printer.println("        " + variables.getType() + " result = " + variables.getType() + ".forNumber("
					+ variables.getName() + "_);");
			printer.println("        return result == null ? " + variables.getUnknown() + " : result;");
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
		printer.println("      public Builder set" + variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
		printer.println("        if (value == null) { throw new NullPointerException(); }");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
			printer.println("        " + variables.getOneofFieldVariable() + " = value.getNumber();");
		}
		else
		{
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + variables.getName() + "_ = value.getNumber();");
		}
		printer.println("        " + variables.getOnChanged());
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
		printer.println("      public Builder clear" + variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          " + variables.getOneofCaseVariable() + " = 0;");
			printer.println("          " + variables.getOneofFieldVariable() + " = null;");
			printer.println("          " + variables.getOnChanged());
			printer.println("        }");
		}
		else
		{
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			printer.println("        " + variables.getName() + "_ = " + variables.getDefaultNumber() + ";");
			printer.println("        " + variables.getOnChanged());
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
		printer.println("      " + variables.getName() + "_ = " + variables.getDefaultNumber() + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("        " + variables.getName() + "_ = " + variables.getDefaultNumber() + ";");
		}
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("            set" + variables.getCapitalizedName() + "(other.get"
					+ variables.getCapitalizedName() + "());");
		}
		else
		{
			printer.println("        if (" + variables.getIsOtherFieldPresentMessage() + ") {");
			printer.println("          set" + variables.getCapitalizedName() + "(other.get"
					+ variables.getCapitalizedName() + "());");
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
		printer.println("        if (" + variables.getGetHasFieldBitFromLocal() + ") {");
		printer.println("          result." + variables.getName() + "_ = " + variables.getName() + "_;");
		if (getNumBitsForMessage() > 0)
		{
			printer.println("          " + variables.getSetHasFieldBitToLocal());
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
				printer.println("                " + variables.getOneofFieldVariable() + " = input.readEnum();");
				printer.println("                " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
			}
			else
			{
				printer.println("                int rawValue = input.readEnum();");
				printer.println("                " + variables.getType() + " value =");
				printer.println("                    " + variables.getType() + ".forNumber(rawValue);");
				printer.println("                if (value == null) {");
				printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", rawValue);");
				printer.println("                } else {");
				printer.println("                  " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
				printer.println("                  " + variables.getOneofFieldVariable() + " = rawValue;");
				printer.println("                }");
			}
		}
		else
		{
			if (supportUnknownEnumValue(descriptor))
			{
				printer.println("                " + variables.getName() + "_ = input.readEnum();");
				printer.println("                " + variables.getSetHasFieldBitBuilder());
			}
			else
			{
				printer.println("                int tmpRaw = input.readEnum();");
				printer.println("                " + variables.getType() + " tmpValue =");
				printer.println("                    " + variables.getType() + ".forNumber(tmpRaw);");
				printer.println("                if (tmpValue == null) {");
				printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
				printer.println("                } else {");
				printer.println("                  " + variables.getName() + "_ = tmpRaw;");
				printer.println("                  " + variables.getSetHasFieldBitBuilder());
				printer.println("                }");
			}
		}
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		String valueVar = variables.getName() + "_";
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			valueVar = "((java.lang.Integer) " + variables.getOneofFieldVariable() + ")";
		}
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("          .computeEnumSize(" + variables.getNumber() + ", " + valueVar + ");");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		String valueVar = variables.getName() + "_";
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			valueVar = "((java.lang.Integer) " + variables.getOneofFieldVariable() + ")";
		}
		printer.println("        output.writeEnum(" + variables.getNumber() + ", " + valueVar + ");");
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
			printer.println("      if (has" + variables.getCapitalizedName() + "() != other.has" + variables.getCapitalizedName() + "()) return false;");
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
			printer.println("        if (" + variables.getName() + "_ != other." + variables.getName() + "_) return false;");
			printer.println("      }");
		}
		else
		{
			printer.println("      if (" + variables.getName() + "_ != other." + variables.getName() + "_) return false;");
		}
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
		}
		printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
		printer.println("        hash = (53 * hash) + " + variables.getName() + "_;");
		if (descriptor.hasPresence())
		{
			printer.println("      }");
		}
	}

	@Override
	public void generateOneofEqualsCode(PrintWriter printer)
	{
		printer.println("          if (!get" + variables.getCapitalizedName() + "()");
		printer.println("              .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
	}

	@Override
	public void generateOneofHashCode(PrintWriter printer)
	{
		printer.println("          hash = (37 * hash) + " + variables.getConstantName() + ";");
		printer.println("          hash = (53 * hash) + get" + variables.getCapitalizedName() + "().getNumber();");
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public String getBoxedType()
	{
		return variables.getType();
	}

	private boolean supportUnknownEnumValue(FieldDescriptor descriptor)
	{
		return Helpers.supportUnknownEnumValue(descriptor);
	}

	public static class RepeatedEnumFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final ContextVariables variables;
		private final int fieldNumber;

		public RepeatedEnumFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.fieldNumber = descriptor.getNumber();
			this.variables = new ContextVariables();
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
				ContextVariables variables,
				Context context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			variables.setType( context.getNameResolver().getImmutableClassName(descriptor.getEnumType()));
			variables.setBoxedType( variables.getType());
			variables.setEmptyList( "emptyIntList()");
			variables.setFieldListType( "com.google.protobuf.Internal.IntList");
			variables.setRepeatedGet( variables.getName() + "_.getInt");
			variables.setRepeatedAdd( variables.getName() + "_.addInt");
			variables.setRepeatedSet( variables.getName() + "_.setInt");
			variables.setNameMakeImmutable( variables.getName() + "_.makeImmutable()");
			variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
			variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
			variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
			variables.setOnChanged( "onChanged();");
			variables.setCapitalizedType( Helpers.getCapitalizedType(descriptor));
			variables.setDefaultValue( variables.getType() + "."
					+ descriptor.getEnumType().getValues().get(0).getName());
			variables.setUnknown(
					Helpers.supportUnknownEnumValue(descriptor)
							? variables.getType() + ".UNRECOGNIZED"
							: variables.getDefaultValue());
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
					"    java.util.List<" + variables.getBoxedType() + "> get" + variables.getCapitalizedName() + "List();");
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
			printer.println("    int get" + variables.getCapitalizedName() + "Count();");
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
			printer.println("    " + variables.getType() + " get" + variables.getCapitalizedName() + "(int index);");
			if (Helpers.supportUnknownEnumValue(descriptor))
			{
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
				printer.println("    get" + variables.getCapitalizedName() + "ValueList();");
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
				printer.println("    int get" + variables.getCapitalizedName() + "Value(int index);");
			}
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private " + variables.getFieldListType() + " " + variables.getName() + "_ =");
			printer.println("        " + variables.getEmptyList() + ";");

			// Converter field
			printer.println("    private static final     com.google.protobuf.Internal.IntListAdapter.IntConverter<");
			printer.println("        " + variables.getType() + "> " + variables.getName() + "_converter_ =");
			printer.println("            new com.google.protobuf.Internal.IntListAdapter.IntConverter<");
			printer.println("                " + variables.getType() + ">() {");
			printer.println("              public " + variables.getType() + " convert(int from) {");
			printer.println("                " + variables.getType() + " result = " + variables.getType() + ".forNumber(from);");
			printer.println("                return result == null ? " + variables.getUnknown() + " : result;");
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
			printer.println("    public java.util.List<" + variables.getBoxedType() + "> get"
					+ variables.getCapitalizedName() + "List() {");
			printer.println("      return new com.google.protobuf.Internal.IntListAdapter<");
			printer.println("          " + variables.getType() + ">(" + variables.getName() + "_, " + variables.getName() + "_converter_);");
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
			printer.println("    public int get" + variables.getCapitalizedName() + "Count() {");
			printer.println("      return " + variables.getName() + "_.size();");
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
			printer.println("    public " + variables.getType() + " get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("      return " + variables.getName() + "_converter_.convert(" + variables.getName() + "_.getInt(index));");
			printer.println("    }");

			if (Helpers.supportUnknownEnumValue(descriptor))
			{
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
				printer.println("    get" + variables.getCapitalizedName() + "ValueList() {");
				printer.println("      return " + variables.getName() + "_;");
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
				printer.println("    public int get" + variables.getCapitalizedName() + "Value(int index) {");
				printer.println("      return " + variables.getName() + "_.getInt(index);");
				printer.println("    }");
			}

			if (descriptor.isPacked())
			{
				printer.println("    private int " + variables.getName() + "MemoizedSerializedSize;");
			}
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("      private " + variables.getFieldListType() + " " + variables.getName() + "_ = " + variables.getEmptyList() + ";");

			printer.println("      private void ensure" + variables.getCapitalizedName() + "IsMutable() {");
			printer.println("        if (!" + variables.getName() + "_.isModifiable()) {");
			printer.println("          " + variables.getName() + "_ = makeMutableCopy(" + variables.getName() + "_);");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
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
			printer.println("      public java.util.List<" + variables.getBoxedType() + "> get"
					+ variables.getCapitalizedName() + "List() {");
			printer.println("        return new com.google.protobuf.Internal.IntListAdapter<");
			printer.println("            " + variables.getType() + ">(" + variables.getName() + "_, " + variables.getName() + "_converter_);");
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
			printer.println("      public int get" + variables.getCapitalizedName() + "Count() {");
			printer.println("        return " + variables.getName() + "_.size();");
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
			printer.println("      public " + variables.getType() + " get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("        return " + variables.getName() + "_converter_.convert(" + variables.getName() + "_.getInt(index));");
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
			printer.println("      public Builder set" + variables.getCapitalizedName() + "(");
			printer.println("          int index, " + variables.getType() + " value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getName() + "_.setInt(index, value.getNumber());");
			printer.println("        " + variables.getOnChanged());
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
			printer.println("      public Builder add" + variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getName() + "_.addInt(value.getNumber());");
			printer.println("        " + variables.getOnChanged());
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
			printer.println("      public Builder addAll" + variables.getCapitalizedName() + "(");
			printer.println("          java.lang.Iterable<? extends " + variables.getBoxedType() + "> values) {");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        for (" + variables.getBoxedType() + " value : values) {");
			printer.println("          " + variables.getName() + "_.addInt(value.getNumber());");
			printer.println("        }");
			printer.println("        " + variables.getOnChanged());
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
			printer.println("      public Builder clear" + variables.getCapitalizedName() + "() {");
			printer.println("        " + variables.getName() + "_ = " + variables.getEmptyList() + ";");
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			printer.println("        " + variables.getOnChanged());
			printer.println("        return this;");
			printer.println("      }");

			if (Helpers.supportUnknownEnumValue(descriptor))
			{
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
				printer.println("      get" + variables.getCapitalizedName() + "ValueList() {");
				printer.println("        " + variables.getName() + "_.makeImmutable();");
				printer.println("        return " + variables.getName() + "_;");
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
				printer.println("      public int get" + variables.getCapitalizedName() + "Value(int index) {");
				printer.println("        return " + variables.getName() + "_.getInt(index);");
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
				printer.println("      public Builder set" + variables.getCapitalizedName() + "Value(");
				printer.println("          int index, int value) {");
				printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
				printer.println("        " + variables.getRepeatedSet() + "(index, value);");
				printer.println("        " + variables.getOnChanged());
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
				printer.println("      public Builder add" + variables.getCapitalizedName() + "Value(int value) {");
				printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
				printer.println("        " + variables.getRepeatedAdd() + "(value);");
				printer.println("        " + variables.getOnChanged());
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
				printer.println("      public Builder addAll" + variables.getCapitalizedName() + "Value(");
				printer.println("          java.lang.Iterable<java.lang.Integer> values) {");
				printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
				printer.println("        for (int value : values) {");
				printer.println("          " + variables.getRepeatedAdd() + "(value);");
				printer.println("        }");
				printer.println("        " + variables.getOnChanged());
				printer.println("        return this;");
				printer.println("      }");
			}
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("      " + variables.getName() + "_ = " + variables.getEmptyList() + ";");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("        " + variables.getName() + "_ = " + variables.getEmptyList() + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			printer.println("        if (!other." + variables.getName() + "_.isEmpty()) {");
			printer.println("          if (" + variables.getName() + "_.isEmpty()) {");
			printer.println("            " + variables.getName() + "_ = other." + variables.getName() + "_;");
			printer.println("            " + variables.getName() + "_.makeImmutable();");
			printer.println("            " + variables.getSetHasFieldBitBuilder());
			printer.println("          } else {");
			printer.println("            ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("            " + variables.getName() + "_.addAll(other." + variables.getName() + "_);");
			printer.println("          }");
			printer.println("          onChanged();");
			printer.println("        }");
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.getGetHasFieldBitFromLocal() + ") {");
			printer.println("          " + variables.getName() + "_.makeImmutable();");
			printer.println("          result." + variables.getName() + "_ = " + variables.getName() + "_;");
			printer.println("        }");
		}

		@Override
		public void generateBuilderParsingCode(PrintWriter printer)
		{
			if (Helpers.supportUnknownEnumValue(descriptor))
			{
				printer.println("                int tmpRaw = input.readEnum();");
				printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable();");
				printer.println("                " + variables.getRepeatedAdd() + "(tmpRaw);");
			}
			else
			{
				printer.println("                int tmpRaw = input.readEnum();");
				printer.println("                " + variables.getType() + " tmpValue =");
				printer.println("                    " + variables.getType() + ".forNumber(tmpRaw);");
				printer.println("                if (tmpValue == null) {");
				printer.println("                  mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
				printer.println("                } else {");
				printer.println("                  ensure" + variables.getCapitalizedName() + "IsMutable();");
				printer.println("                  " + variables.getRepeatedAdd() + "(tmpRaw);");
				printer.println("                }");
			}
		}

		@Override
		public void generateBuilderParsingCodeFromPacked(PrintWriter printer)
		{
			printer.println("                int length = input.readRawVarint32();");
			printer.println("                int limit = input.pushLimit(length);");
			printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("                while (input.getBytesUntilLimit() > 0) {");
			printer.println("                  int tmpRaw = input.readEnum();");
			if (Helpers.supportUnknownEnumValue(descriptor))
			{
				printer.println("                  " + variables.getRepeatedAdd() + "(tmpRaw);");
			}
			else
			{
				printer.println("                  " + variables.getType() + " tmpValue =");
				printer.println("                      " + variables.getType() + ".forNumber(tmpRaw);");
				printer.println("                  if (tmpValue == null) {");
				printer.println("                    mergeUnknownVarintField(" + fieldNumber + ", tmpRaw);");
				printer.println("                  } else {");
				printer.println("                    " + variables.getRepeatedAdd() + "(tmpRaw);");
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
			printer.println("        for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("          dataSize += com.google.protobuf.CodedOutputStream");
			printer.println("            .computeEnumSizeNoTag(" + variables.getName() + "_.getInt(i));");
			printer.println("        }");
			printer.println("        size += dataSize;");
			if (descriptor.isPacked())
			{
				printer.println("        if (!" + variables.getName() + "_.isEmpty()) {");
				printer.println("          size += " + Helpers.getTagSize(descriptor) + ";");
				printer.println("          size += com.google.protobuf.CodedOutputStream");
				printer.println("            .computeUInt32SizeNoTag(dataSize);");
				printer.println("        }");
				printer.println("        " + variables.getName() + "MemoizedSerializedSize = dataSize;");
			}
			else
			{
				printer.println("        size += " + Helpers.getTagSize(descriptor) + " * " + variables.getName() + "_.size();");
			}
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			if (descriptor.isPacked())
			{
				printer.println("      if (" + variables.getName() + "_.size() > 0) {");
				printer.println("        output.writeUInt32NoTag(" + Helpers.getTag(descriptor) + ");");
				printer.println("        output.writeUInt32NoTag(" + variables.getName() + "MemoizedSerializedSize);");
				printer.println("      }");
				printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
				printer.println("        output.writeEnumNoTag(" + variables.getName() + "_.getInt(i));");
				printer.println("      }");
			}
			else
			{
				printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
				printer.println("        output.writeEnum(" + variables.getNumber() + ", " + variables.getName() + "_.getInt(i));");
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
			printer.println("      if (!" + variables.getName() + "_.equals(other." + variables.getName()
					+ "_)) return false;");
		}

		@Override
		public void generateHashCode(PrintWriter printer)
		{
			printer.println("      if (get" + variables.getCapitalizedName() + "Count() > 0) {");
			printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
			printer.println("        hash = (53 * hash) + " + variables.getName() + "_.hashCode();");
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
			return variables.getBoxedType();
		}
	}
}
