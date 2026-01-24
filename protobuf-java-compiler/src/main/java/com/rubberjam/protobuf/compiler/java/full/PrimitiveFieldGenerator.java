package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.InternalHelpers;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final Map<String, String> variables;
	private final int fieldNumber;

	public PrimitiveFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
		this.variables = new HashMap<>();
		setPrimitiveVariables(
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

	private void setPrimitiveVariables(
			FieldDescriptor descriptor,
			int messageBitIndex,
			int builderBitIndex,
			FieldGeneratorInfo info,
			Map<String, String> variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		JavaType javaType = StringUtils.getJavaType(descriptor);

		variables.put("type", StringUtils.getPrimitiveTypeName(javaType));
		variables.put("boxed_type", StringUtils.boxedPrimitiveTypeName(javaType));
		variables.put("field_type", variables.get("type"));
		String defaultValue = Helpers.defaultValue(descriptor, context.getNameResolver(), context.getOptions(), true);
		variables.put("default", defaultValue);
		variables.put("default_init",
				Helpers.isDefaultValueJavaDefault(descriptor)
						? ""
						: "= " + defaultValue);
		variables.put("capitalized_type", Helpers.getCapitalizedType(descriptor));
		variables.put("tag", Integer.toString(Helpers.getTag(descriptor)));
		variables.put("tag_size", Integer.toString(Helpers.getTagSize(descriptor)));

		if (Helpers.isReferenceType(javaType))
		{
			variables.put("null_check", "if (value == null) { throw new NullPointerException(); }");
		}
		else
		{
			variables.put("null_check", "");
		}
		variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
		variables.put("on_changed", "onChanged();");

		if (descriptor.getContainingOneof() != null)
		{
			String oneofName = StringUtils.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.put("oneof_name", oneofName);
			variables.put("oneof_case_variable", oneofName + "Case_");
			variables.put("oneof_field_variable", oneofName + "_");
			variables.put("set_has_field_bit_to_local", "");
			variables.put("is_field_present_message", oneofName + "Case_ == " + descriptor.getNumber());
			variables.put("is_other_field_present_message", "other.has" + variables.get("capitalized_name") + "()");
		}
		else if (InternalHelpers.hasHasbit(descriptor))
		{
			variables.put("set_has_field_bit_to_local", Helpers.generateSetBitToLocal(messageBitIndex) + ";");
			variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
			variables.put("is_other_field_present_message", "other.has" + variables.get("capitalized_name") + "()");
		}
		else
		{
			variables.put("set_has_field_bit_to_local", "");
			switch (descriptor.getType())
			{
			case BYTES:
				variables.put("is_field_present_message", "!" + variables.get("name") + "_.isEmpty()");
				variables.put("is_other_field_present_message", "!other.get" + variables.get("capitalized_name") + "().isEmpty()");
				break;
			case FLOAT:
				variables.put("is_field_present_message",
						"java.lang.Float.floatToRawIntBits(" + variables.get("name") + "_) != 0");
				variables.put("is_other_field_present_message",
						"java.lang.Float.floatToRawIntBits(other.get" + variables.get("capitalized_name") + "()) != 0");
				break;
			case DOUBLE:
				variables.put("is_field_present_message",
						"java.lang.Double.doubleToRawLongBits(" + variables.get("name") + "_) != 0");
				variables.put("is_other_field_present_message",
						"java.lang.Double.doubleToRawLongBits(other.get" + variables.get("capitalized_name") + "()) != 0");
				break;
			default:
				variables.put("is_field_present_message", variables.get("name") + "_ != " + variables.get("default"));
				variables.put("is_other_field_present_message",
						"other.get" + variables.get("capitalized_name") + "() != " + variables.get("default"));
				break;
			}
		}

		variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
		variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");

		String capitalizedType = StringUtils.toProperCase(variables.get("type"));
		if (javaType == JavaType.INT || javaType == JavaType.LONG || javaType == JavaType.BOOLEAN
				|| javaType == JavaType.FLOAT || javaType == JavaType.DOUBLE)
		{
			variables.put("field_list_type", "com.google.protobuf.Internal." + capitalizedType + "List");
			variables.put("empty_list", "empty" + capitalizedType + "List()");
			variables.put("repeated_get", variables.get("name") + "_.get" + capitalizedType);
			variables.put("repeated_add", variables.get("name") + "_.add" + capitalizedType);
			variables.put("repeated_set", variables.get("name") + "_.set" + capitalizedType);
		}
		else
		{
			variables.put("field_list_type",
					"com.google.protobuf.Internal.ProtobufList<com.google.protobuf.ByteString>");
			variables.put("empty_list", "emptyList(com.google.protobuf.ByteString.class)");
			variables.put("repeated_get", variables.get("name") + "_.get");
			variables.put("repeated_add", variables.get("name") + "_.add");
			variables.put("repeated_set", variables.get("name") + "_.set");
		}
		variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
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
			printer.println("    " + variables.get("deprecation") + "boolean has" + variables.get("capitalized_name") + "();");
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
		printer.println("    " + variables.get("deprecation") + variables.get("type") + " get"
				+ variables.get("capitalized_name") + "();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		if (descriptor.getContainingOneof() == null)
		{
			printer.println("    private " + variables.get("field_type") + " " + variables.get("name") + "_ = "
					+ variables.get("default") + ";");
			FieldCommon.printExtraFieldInfo(variables, printer);
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
			printer.println("    @java.lang.Override");
			printer.println("    " + variables.get("deprecation") + "public boolean has"
					+ variables.get("capitalized_name") + "() {");
			printer.println("      return " + variables.get("is_field_present_message") + ";");
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
		printer.println("    @java.lang.Override");
		printer.println("    " + variables.get("deprecation") + "public " + variables.get("type") + " get"
				+ variables.get("capitalized_name") + "() {");
		if (descriptor.getContainingOneof() != null)
		{
			printer.println("      if (" + variables.get("is_field_present_message") + ") {");
			printer.println("        return (" + variables.get("boxed_type") + ") " + variables.get("oneof_field_variable") + ";");
			printer.println("      }");
			printer.println("      return " + variables.get("default") + ";");
		}
		else
		{
			printer.println("      return " + variables.get("name") + "_;");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		JavaType javaType = StringUtils.getJavaType(descriptor);
		if (descriptor.getContainingOneof() == null)
		{
			printer.println("      private " + variables.get("field_type") + " " + variables.get("name") + "_ "
					+ variables.get("default_init") + ";");
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
			printer.println("      @java.lang.Override");
			printer.println("      " + variables.get("deprecation") + "public boolean has"
					+ variables.get("capitalized_name") + "() {");
			printer.println("        return " + variables.get("get_has_field_bit_builder") + ";");
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
		printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + " get"
				+ variables.get("capitalized_name") + "() {");
		printer.println("        return " + variables.get("name") + "_;");
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
		printer.println("      " + variables.get("deprecation") + "public Builder set"
				+ variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		if (!variables.get("null_check").isEmpty())
		{
			printer.println("        " + variables.get("null_check"));
		}
		else
		{
			printer.println();
		}
		printer.println("        " + variables.get("name") + "_ = value;");
		printer.println("        " + variables.get("set_has_field_bit_builder"));
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
		printer.println("      " + variables.get("deprecation") + "public Builder clear"
				+ variables.get("capitalized_name") + "() {");
		printer.println("        " + variables.get("clear_has_field_bit_builder"));
		if (javaType == JavaType.STRING || javaType == JavaType.BYTES)
		{
			printer.println("        " + variables.get("name") + "_ = getDefaultInstance().get"
					+ variables.get("capitalized_name") + "();");
		}
		else
		{
			printer.println("        " + variables.get("name") + "_ = " + variables.get("default") + ";");
		}
		printer.println("        " + variables.get("on_changed"));
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
		if (!Helpers.isDefaultValueJavaDefault(descriptor))
		{
			printer.println("      " + variables.get("name") + "_ = " + variables.get("default") + ";");
		}
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		if (descriptor.getContainingOneof() == null)
		{
			printer.println("        " + variables.get("name") + "_ = " + variables.get("default") + ";");
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
		printer.println("                " + variables.get("name") + "_ = input.read"
				+ variables.get("capitalized_type") + "();");
		printer.println("                " + variables.get("set_has_field_bit_builder"));
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		String valueVar = variables.get("name") + "_";
		if (descriptor.getContainingOneof() != null)
		{
			valueVar = "(" + variables.get("type") + ")((" + variables.get("boxed_type") + ") " + variables.get("oneof_field_variable") + ")";
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.get("capitalized_type") + "Size(");
			printer.println("              " + variables.get("number") + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.get("capitalized_type") + "Size("
					+ variables.get("number") + ", " + valueVar + ");");
		}
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		String valueVar = variables.get("name") + "_";
		if (descriptor.getContainingOneof() != null)
		{
			valueVar = "(" + variables.get("type") + ")((" + variables.get("boxed_type") + ") " + variables.get("oneof_field_variable") + ")";
			printer.println("        output.write" + variables.get("capitalized_type") + "(");
			printer.println("            " + variables.get("number") + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        output.write" + variables.get("capitalized_type") + "("
					+ variables.get("number") + ", " + valueVar + ");");
		}
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
			switch (StringUtils.getJavaType(descriptor))
			{
			case INT:
			case LONG:
			case BOOLEAN:
				printer.println("        if (get" + variables.get("capitalized_name") + "()");
				printer.println("            != other.get" + variables.get("capitalized_name") + "()) return false;");
				break;
			case FLOAT:
				printer.println("        if (java.lang.Float.floatToIntBits(get" + variables.get("capitalized_name") + "())");
				printer.println("            != java.lang.Float.floatToIntBits(");
				printer.println("                other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			case DOUBLE:
				printer.println("        if (java.lang.Double.doubleToLongBits(get" + variables.get("capitalized_name") + "())");
				printer.println("            != java.lang.Double.doubleToLongBits(");
				printer.println("                other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			case STRING:
			case BYTES:
				printer.println("        if (!get" + variables.get("capitalized_name") + "()");
				printer.println("            .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			default:
				break;
			}
			printer.println("      }");
		}
		else
		{
			switch (StringUtils.getJavaType(descriptor))
			{
			case INT:
			case LONG:
			case BOOLEAN:
				printer.println("      if (get" + variables.get("capitalized_name") + "()");
				printer.println("          != other.get" + variables.get("capitalized_name") + "()) return false;");
				break;
			case FLOAT:
				printer.println("      if (java.lang.Float.floatToIntBits(get" + variables.get("capitalized_name") + "())");
				printer.println("          != java.lang.Float.floatToIntBits(");
				printer.println("              other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			case DOUBLE:
				printer.println("      if (java.lang.Double.doubleToLongBits(get" + variables.get("capitalized_name") + "())");
				printer.println("          != java.lang.Double.doubleToLongBits(");
				printer.println("              other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			case STRING:
			case BYTES:
				printer.println("      if (!get" + variables.get("capitalized_name") + "()");
				printer.println("          .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
				break;
			default:
				break;
			}
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
		switch (StringUtils.getJavaType(descriptor))
		{
		case INT:
			printer.println("        hash = (53 * hash) + get" + variables.get("capitalized_name") + "();");
			break;
		case LONG:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("            get" + variables.get("capitalized_name") + "());");
			break;
		case BOOLEAN:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(");
			printer.println("            get" + variables.get("capitalized_name") + "());");
			break;
		case FLOAT:
			printer.println("        hash = (53 * hash) + java.lang.Float.floatToIntBits(");
			printer.println("            get" + variables.get("capitalized_name") + "());");
			break;
		case DOUBLE:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("            java.lang.Double.doubleToLongBits(get" + variables.get("capitalized_name") + "()));");
			break;
		case STRING:
		case BYTES:
			printer.println("        hash = (53 * hash) + get" + variables.get("capitalized_name") + "().hashCode();");
			break;
		default:
			break;
		}
		if (descriptor.hasPresence())
		{
			printer.println("      }");
		}
	}

	@Override
	public void generateOneofEqualsCode(PrintWriter printer)
	{
		switch (StringUtils.getJavaType(descriptor))
		{
		case INT:
		case LONG:
		case BOOLEAN:
			printer.println("          if (get" + variables.get("capitalized_name") + "()");
			printer.println("              != other.get" + variables.get("capitalized_name") + "()) return false;");
			break;
		case FLOAT:
			printer.println("          if (java.lang.Float.floatToIntBits(get" + variables.get("capitalized_name") + "())");
			printer.println("              != java.lang.Float.floatToIntBits(");
			printer.println("                  other.get" + variables.get("capitalized_name") + "())) return false;");
			break;
		case DOUBLE:
			printer.println("          if (java.lang.Double.doubleToLongBits(get" + variables.get("capitalized_name") + "())");
			printer.println("              != java.lang.Double.doubleToLongBits(");
			printer.println("                  other.get" + variables.get("capitalized_name") + "())) return false;");
			break;
		case STRING:
		case BYTES:
			printer.println("          if (!get" + variables.get("capitalized_name") + "()");
			printer.println("              .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
			break;
		default:
			break;
		}
	}

	@Override
	public void generateOneofHashCode(PrintWriter printer)
	{
		printer.println("          hash = (37 * hash) + " + variables.get("constant_name") + ";");
		switch (StringUtils.getJavaType(descriptor))
		{
		case INT:
			printer.println("          hash = (53 * hash) + get" + variables.get("capitalized_name") + "();");
			break;
		case LONG:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("              get" + variables.get("capitalized_name") + "());");
			break;
		case BOOLEAN:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(");
			printer.println("              get" + variables.get("capitalized_name") + "());");
			break;
		case FLOAT:
			printer.println("          hash = (53 * hash) + java.lang.Float.floatToIntBits(");
			printer.println("              get" + variables.get("capitalized_name") + "());");
			break;
		case DOUBLE:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("              java.lang.Double.doubleToLongBits(get" + variables.get("capitalized_name") + "()));");
			break;
		case STRING:
		case BYTES:
			printer.println("          hash = (53 * hash) + get" + variables.get("capitalized_name") + "().hashCode();");
			break;
		default:
			break;
		}
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		String valueVar = variables.get("name") + "_";
		if (descriptor.getContainingOneof() != null)
		{
			valueVar = "(" + variables.get("type") + ")((" + variables.get("boxed_type") + ") " + variables.get("oneof_field_variable") + ")";
			printer.println("        output.write" + variables.get("capitalized_type") + "(");
			printer.println("            " + variables.get("number") + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        output.write" + variables.get("capitalized_type") + "(" + variables.get("number")
					+ ", " + valueVar + ");");
		}
		printer.println("      }");
	}

	@Override
	public String getBoxedType()
	{
		return variables.get("boxed_type");
	}

	public static class RepeatedPrimitiveFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final Map<String, String> variables;
		private final int fieldNumber;

		public RepeatedPrimitiveFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.fieldNumber = descriptor.getNumber();
			this.variables = new HashMap<>();
			setPrimitiveVariables(
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

		private void setPrimitiveVariables(
				FieldDescriptor descriptor,
				int messageBitIndex,
				int builderBitIndex,
				FieldGeneratorInfo info,
				Map<String, String> variables,
				Context context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			JavaType javaType = StringUtils.getJavaType(descriptor);

			variables.put("type", StringUtils.getPrimitiveTypeName(javaType));
			variables.put("boxed_type", StringUtils.boxedPrimitiveTypeName(javaType));

			String capitalizedType = StringUtils.toProperCase(variables.get("type")); // e.g.
																						// Int,
																						// Long
			if (javaType == JavaType.INT || javaType == JavaType.LONG || javaType == JavaType.BOOLEAN
					|| javaType == JavaType.FLOAT || javaType == JavaType.DOUBLE)
			{
				variables.put("field_list_type", "com.google.protobuf.Internal." + capitalizedType + "List");
				variables.put("empty_list", "empty" + capitalizedType + "List()");
				variables.put("repeated_get", variables.get("name") + "_.get" + capitalizedType);
				variables.put("repeated_add", variables.get("name") + "_.add" + capitalizedType);
				variables.put("repeated_set", variables.get("name") + "_.set" + capitalizedType);
			}
			else
			{
				// Fallback or byte string? For now primitive handles
				// numeric/bool
				// C++ logic: else { ... Internal.ProtobufList<ByteString> ... }
				// This class is PrimitiveFieldGenerator so we expect
				// numeric/bool.
			}

			variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
			variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
			variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
			variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
			variables.put("capitalized_type", Helpers.getCapitalizedType(descriptor));
			variables.put("capitalized_java_type", StringUtils.toProperCase(variables.get("type")));
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
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private " + variables.get("field_list_type") + " " + variables.get("name") + "_ =");
			printer.println("        " + variables.get("empty_list") + ";");

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
			printer.println("    public java.util.List<" + variables.get("boxed_type") + ">");
			printer.println("        get" + variables.get("capitalized_name") + "List() {");
			printer.println("      return " + variables.get("name") + "_;");
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
			printer.println("    public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("      return " + variables.get("repeated_get") + "(index);");
			printer.println("    }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("  private " + variables.get("field_list_type") + " " + variables.get("name") + "_ = "
					+ variables.get("empty_list") + ";");

			printer.println("  private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("    if (!" + variables.get("name") + ".isModifiable()) {");
			printer.println("      " + variables.get("name") + "_ = makeMutableCopy(" + variables.get("name") + "_);");
			printer.println("    }");
			printer.println("    " + variables.get("set_has_field_bit_builder"));
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public java.util.List<" + variables.get("boxed_type") + "> get" + variables.get("capitalized_name")
					+ "List() {");
			printer.println("    " + variables.get("name") + "_.makeImmutable();");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    return " + variables.get("name") + "_.size();");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("    return " + variables.get("repeated_get") + "(index);");
			printer.println("  }");

			printer.println("  public Builder set" + variables.get("capitalized_name") + "(int index, " + variables.get("type")
					+ " value) {");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("    " + variables.get("repeated_set") + "(index, value);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println(
					"  public Builder add" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("    " + variables.get("repeated_add") + "(value);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder addAll" + variables.get("capitalized_name") + "(java.lang.Iterable<? extends "
					+ variables.get("boxed_type") + "> values) {");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println(
					"    com.google.protobuf.AbstractMessageLite.Builder.addAll(values, " + variables.get("name") + "_);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
			printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
			printer.println("    return this;");
			printer.println("  }");
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
			// Placeholder
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("      if (" + variables.get("get_has_field_bit_from_local") + ") {");
			printer.println("        " + variables.get("name") + "_.makeImmutable();");
			printer.println("      }");
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
			printer.println("      {");
			printer.println("        int dataSize = 0;");
			printer.println("        for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("          dataSize += com.google.protobuf.CodedOutputStream");
			printer.println("            .compute" + variables.get("capitalized_type") + "SizeNoTag("
					+ variables.get("name") + "_.get" + variables.get("capitalized_java_type") + "(i));");
			printer.println("        }");
			printer.println("        size += dataSize;");
			printer.println("        size += " + Helpers.getTagSize(descriptor) + " * get" + variables.get("capitalized_name") + "List().size();");
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("        output.write" + variables.get("capitalized_type") + "("
					+ variables.get("number") + ", " + variables.get("name") + "_.get" + variables.get("capitalized_java_type") + "(i));");
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
			printer.println("      if (!get" + variables.get("capitalized_name") + "List()");
			printer.println("          .equals(other.get" + variables.get("capitalized_name") + "List())) return false;");
		}

		@Override
		public void generateHashCode(PrintWriter printer)
		{
			printer.println("      if (get" + variables.get("capitalized_name") + "Count() > 0) {");
			printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
			printer.println("        hash = (53 * hash) + get" + variables.get("capitalized_name") + "List().hashCode();");
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
