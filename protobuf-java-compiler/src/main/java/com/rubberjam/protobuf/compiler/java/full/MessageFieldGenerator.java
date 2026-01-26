package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.InternalHelpers;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.Helpers;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MessageFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final int fieldNumber;
	private final Map<String, String> variables;

	public MessageFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
		this.variables = new HashMap<>();
		setMessageVariables(
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

	private void setMessageVariables(
			FieldDescriptor descriptor,
			int messageBitIndex,
			int builderBitIndex,
			FieldGeneratorInfo info,
			Map<String, String> variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		variables.put("type", context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
		variables.put("group_or_message",
				descriptor.getType() == FieldDescriptor.Type.GROUP ? "Group" : "Message");
		variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
		variables.put("on_changed", "onChanged();");
		variables.put("get_parser", "parser()");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils
					.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.put("oneof_name", oneofName);
			variables.put("oneof_case_variable", oneofName + "Case_");
			variables.put("oneof_field_variable", oneofName + "_");
			variables.put("set_has_field_bit_to_local", "");
			variables.put("is_field_present_message", oneofName + "Case_ == " + descriptor.getNumber());
		}
		else if (InternalHelpers.hasHasbit(descriptor))
		{
			variables.put("set_has_field_bit_to_local", Helpers.generateSetBitToLocal(messageBitIndex));
			variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
		}
		else
		{
			variables.put("set_has_field_bit_to_local", "");
			variables.put("is_field_present_message", variables.get("name") + "_ != null");
		}

		variables.put("get_mutable_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("set_mutable_bit_builder", Helpers.generateSetBit(builderBitIndex));
		variables.put("clear_mutable_bit_builder", Helpers.generateClearBit(builderBitIndex));
		variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
		variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
		variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
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
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    " + variables.get("deprecation") + variables.get("type") + "OrBuilder get"
				+ variables.get("capitalized_name") + "OrBuilder();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (!isRealOneof)
		{
			printer.println("    private " + variables.get("type") + " " + variables.get("name") + "_;");
			FieldCommon.printExtraFieldInfo(variables, printer);
		}

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
		if (isRealOneof)
		{
			printer.println("      if (" + variables.get("is_field_present_message") + ") {");
			printer.println("         return (" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ";");
			printer.println("      }");
			printer.println("      return " + variables.get("type") + ".getDefaultInstance();");
		}
		else
		{
			printer.println("      return " + variables.get("name") + "_ == null ? " + variables.get("type")
					+ ".getDefaultInstance() : " + variables.get("name") + "_;");
		}
		printer.println("    }");

		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    @java.lang.Override");
		printer.println("    " + variables.get("deprecation") + "public " + variables.get("type") + "OrBuilder get"
				+ variables.get("capitalized_name") + "OrBuilder() {");
		if (isRealOneof)
		{
			printer.println("      if (" + variables.get("is_field_present_message") + ") {");
			printer.println("         return (" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ";");
			printer.println("      }");
			printer.println("      return " + variables.get("type") + ".getDefaultInstance();");
		}
		else
		{
			printer.println("      return " + variables.get("name") + "_ == null ? " + variables.get("type")
					+ ".getDefaultInstance() : " + variables.get("name") + "_;");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (!isRealOneof)
		{
			printer.println("      private " + variables.get("type") + " " + variables.get("name") + "_;");
		}
		printer.println("      private com.google.protobuf.SingleFieldBuilder<");
		printer.println("          " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
				+ variables.get("type") + "OrBuilder> " + variables.get("name") + "Builder_;");

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
		if (isRealOneof)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.get("deprecation") + "public boolean has"
				+ variables.get("capitalized_name") + "() {");
		if (isRealOneof)
		{
			printer.println("        return " + variables.get("is_field_present_message") + ";");
		}
		else
		{
			printer.println("        return " + variables.get("get_has_field_bit_builder") + ";");
		}
		printer.println("      }");

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
		if (isRealOneof)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + " get"
				+ variables.get("capitalized_name") + "() {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            return (" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ";");
			printer.println("          }");
			printer.println("          return " + variables.get("type") + ".getDefaultInstance();");
			printer.println("        } else {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            return " + variables.get("name") + "Builder_.getMessage();");
			printer.println("          }");
			printer.println("          return " + variables.get("type") + ".getDefaultInstance();");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          return " + variables.get("name") + "_ == null ? " + variables.get("type")
					+ ".getDefaultInstance() : " + variables.get("name") + "_;");
			printer.println("        } else {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessage();");
			printer.println("        }");
		}
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      " + variables.get("deprecation") + "public Builder set"
				+ variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          " + variables.get("oneof_field_variable") + " = value;");
			printer.println("          " + variables.get("on_changed"));
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(value);");
			printer.println("        }");
			printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          " + variables.get("name") + "_ = value;");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(value);");
			printer.println("        }");
			printer.println("        " + variables.get("set_has_field_bit_builder"));
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      " + variables.get("deprecation") + "public Builder set"
				+ variables.get("capitalized_name") + "(");
		printer.println("          " + variables.get("type") + ".Builder builderForValue) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          " + variables.get("oneof_field_variable") + " = builderForValue.build();");
			printer.println("          " + variables.get("on_changed"));
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(builderForValue.build());");
			printer.println("        }");
			printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          " + variables.get("name") + "_ = builderForValue.build();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(builderForValue.build());");
			printer.println("        }");
			printer.println("        " + variables.get("set_has_field_bit_builder"));
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      " + variables.get("deprecation") + "public Builder merge"
				+ variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (" + variables.get("is_field_present_message") + " &&");
			printer.println("              " + variables.get("oneof_field_variable") + " != " + variables.get("type")
					+ ".getDefaultInstance()) {");
			printer.println("            " + variables.get("oneof_field_variable") + " = " + variables.get("type")
					+ ".newBuilder((" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ")");
			printer.println("                .mergeFrom(value).buildPartial();");
			printer.println("          } else {");
			printer.println("            " + variables.get("oneof_field_variable") + " = value;");
			printer.println("          }");
			printer.println("          " + variables.get("on_changed"));
			printer.println("        } else {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            " + variables.get("name") + "Builder_.mergeFrom(value);");
			printer.println("          } else {");
			printer.println("            " + variables.get("name") + "Builder_.setMessage(value);");
			printer.println("          }");
			printer.println("        }");
			printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (" + variables.get("get_has_field_bit_builder") + " &&");
			printer.println("            " + variables.get("name") + "_ != null &&");
			printer.println("            " + variables.get("name") + "_ != " + variables.get("type")
					+ ".getDefaultInstance()) {");
			printer.println("            get" + variables.get("capitalized_name") + "Builder().mergeFrom(value);");
			printer.println("          } else {");
			printer.println("            " + variables.get("name") + "_ = value;");
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.mergeFrom(value);");
			printer.println("        }");
			printer.println("        if (" + variables.get("name") + "_ != null) {");
			printer.println("          " + variables.get("set_has_field_bit_builder"));
			printer.println("          " + variables.get("on_changed"));
			printer.println("        }");
		}
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      " + variables.get("deprecation") + "public Builder clear"
				+ variables.get("capitalized_name") + "() {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            " + variables.get("oneof_case_variable") + " = 0;");
			printer.println("            " + variables.get("oneof_field_variable") + " = null;");
			printer.println("            " + variables.get("on_changed"));
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            " + variables.get("oneof_case_variable") + " = 0;");
			printer.println("            " + variables.get("oneof_field_variable") + " = null;");
			printer.println("          }");
			printer.println("          " + variables.get("name") + "Builder_.clear();");
			printer.println("        }");
		}
		else
		{
			printer.println("        " + variables.get("clear_has_field_bit_builder"));
			printer.println("        " + variables.get("name") + "_ = null;");
			printer.println("        if (" + variables.get("name") + "Builder_ != null) {");
			printer.println("          " + variables.get("name") + "Builder_.dispose();");
			printer.println("          " + variables.get("name") + "Builder_ = null;");
			printer.println("        }");
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + ".Builder get"
				+ variables.get("capitalized_name") + "Builder() {");
		if (!isRealOneof)
		{
			printer.println("        " + variables.get("set_has_field_bit_builder"));
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return internalGet" + variables.get("capitalized_name")
				+ "FieldBuilder().getBuilder();");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		if (isRealOneof)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + "OrBuilder get"
				+ variables.get("capitalized_name") + "OrBuilder() {");
		if (isRealOneof)
		{
			printer.println("        if ((" + variables.get("is_field_present_message") + ") && (" + variables.get("name")
					+ "Builder_ != null)) {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessageOrBuilder();");
			printer.println("        } else {");
			printer.println("          if (" + variables.get("is_field_present_message") + ") {");
			printer.println("            return (" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ";");
			printer.println("          }");
			printer.println("          return " + variables.get("type") + ".getDefaultInstance();");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ != null) {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessageOrBuilder();");
			printer.println("        } else {");
			printer.println("          return " + variables.get("name") + "_ == null ?");
			printer.println("              " + variables.get("type") + ".getDefaultInstance() : " + variables.get("name") + "_;");
			printer.println("        }");
		}
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      private com.google.protobuf.SingleFieldBuilder<");
		printer.println("          " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
				+ variables.get("type") + "OrBuilder> ");
		printer.println("          internalGet" + variables.get("capitalized_name") + "FieldBuilder() {");
		printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
		if (isRealOneof)
		{
			printer.println("          if (!(" + variables.get("is_field_present_message") + ")) {");
			printer.println("            " + variables.get("oneof_field_variable") + " = " + variables.get("type")
					+ ".getDefaultInstance();");
			printer.println("          }");
			printer.println("          " + variables.get("name") + "Builder_ = new com.google.protobuf.SingleFieldBuilder<");
			printer.println("              " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
					+ variables.get("type") + "OrBuilder>(");
			printer.println("                  (" + variables.get("type") + ") " + variables.get("oneof_field_variable") + ",");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.get("oneof_field_variable") + " = null;");
		}
		else
		{
			printer.println("          " + variables.get("name") + "Builder_ = new com.google.protobuf.SingleFieldBuilder<");
			printer.println("              " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
					+ variables.get("type") + "OrBuilder>(");
			printer.println("                  get" + variables.get("capitalized_name") + "(),");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.get("name") + "_ = null;");
		}
		printer.println("        }");
		if (isRealOneof)
		{
			printer.println("        " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
			printer.println("        " + variables.get("on_changed"));
		}
		printer.println("        return " + variables.get("name") + "Builder_;");
		printer.println("      }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// No-op for null initialization
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (!isRealOneof)
		{
			if (descriptor.getContainingOneof() == null)
			{
				printer.println("        " + variables.get("name") + "_ = null;");
			}
			printer.println("        if (" + variables.get("name") + "Builder_ != null) {");
			printer.println("          " + variables.get("name") + "Builder_.dispose();");
			printer.println("          " + variables.get("name") + "Builder_ = null;");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.get("name") + "Builder_ != null) {");
			printer.println("          " + variables.get("name") + "Builder_.clear();");
			printer.println("        }");
		}
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (isRealOneof)
		{
			printer.println("          merge" + variables.get("capitalized_name") + "(other.get"
					+ variables.get("capitalized_name") + "());");
		}
		else
		{
			printer.println("        if (other.has" + variables.get("capitalized_name") + "()) {");
			printer.println("          merge" + variables.get("capitalized_name") + "(other.get"
					+ variables.get("capitalized_name") + "());");
			printer.println("        }");
		}
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (isRealOneof)
		{
			return;
		}
		printer.println("        if (" + variables.get("get_has_field_bit_from_local") + ") {");
		printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "Builder_ == null");
		printer.println("              ? " + variables.get("name") + "_");
		printer.println("              : " + variables.get("name") + "Builder_.build();");
		if (getNumBitsForMessage() > 0)
		{
			printer.println("          " + variables.get("set_has_field_bit_to_local") + ";");
		}
		printer.println("        }");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;

		if (descriptor.getType() == FieldDescriptor.Type.GROUP)
		{
			printer.println("                input.readGroup(" + variables.get("number") + ",");
			printer.println(
					"                    internalGet" + variables.get("capitalized_name") + "FieldBuilder().getBuilder(),");
			printer.println("                    extensionRegistry);");
		}
		else
		{
			printer.println("                input.readMessage(");
			printer.println(
					"                    internalGet" + variables.get("capitalized_name") + "FieldBuilder().getBuilder(),");
			printer.println("                    extensionRegistry);");
		}

		if (isRealOneof)
		{
			printer.println("                " + variables.get("oneof_case_variable") + " = " + variables.get("number") + ";");
		}
		else
		{
			printer.println("                " + variables.get("set_has_field_bit_builder"));
		}
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;

		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		
		if (isRealOneof)
		{
			printer.println("          .compute" + variables.get("group_or_message") + "Size(" + variables.get("number") + ", ("
					+ variables.get("type") + ") " + variables.get("oneof_field_variable") + ");");
		}
		else
		{
			printer.println("          .compute" + variables.get("group_or_message") + "Size(" + variables.get("number") + ", get"
					+ variables.get("capitalized_name") + "());");
		}

		
		

		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;

		printer.println("      if (" + variables.get("is_field_present_message") + ") {");

		if (isRealOneof)
		{
			printer.println("        output.write" + variables.get("group_or_message") + "(" + variables.get("number") + ", ("
					+ variables.get("type") + ") " + variables.get("oneof_field_variable") + ");");
		}
		else
		{
			printer.println("        output.write" + variables.get("group_or_message") + "(" + variables.get("number") + ", get"
					+ variables.get("capitalized_name") + "());");
		}

		printer.println("      }");
	}

	@Override
	public void generateFieldBuilderInitializationCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (isRealOneof)
		{
			return;
		}
		printer.println("          internalGet" + variables.get("capitalized_name") + "FieldBuilder();");
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.get("capitalized_name") + "() != other.has"
					+ variables.get("capitalized_name") + "()) return false;");
			printer.println("      if (has" + variables.get("capitalized_name") + "()) {");
			printer.println("        if (!get" + variables.get("capitalized_name") + "()");
			printer.println("            .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
			printer.println("      }");
		}
		else
		{
			printer.println("      if (!get" + variables.get("capitalized_name") + "()");
			printer.println("          .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
		}
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		printer.println("      if (has" + variables.get("capitalized_name") + "()) {");
		printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("        hash = (53 * hash) + get" + variables.get("capitalized_name") + "().hashCode();");
		printer.println("      }");
	}

	@Override
	public void generateOneofEqualsCode(PrintWriter printer)
	{
		printer.println("          if (!get" + variables.get("capitalized_name") + "()");
		printer.println("              .equals(other.get" + variables.get("capitalized_name") + "())) return false;");
	}

	@Override
	public void generateOneofHashCode(PrintWriter printer)
	{
		printer.println("          hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("          hash = (53 * hash) + get" + variables.get("capitalized_name") + "().hashCode();");
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        output.write" + variables.get("group_or_message") + "(" + variables.get("number") + ", get"
				+ variables.get("capitalized_name") + "());");
		printer.println("      }");
	}

	@Override
	public String getBoxedType()
	{
		return variables.get("type");
	}

	public static class RepeatedMessageFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final Map<String, String> variables;
		private final int fieldNumber;

		public RepeatedMessageFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.fieldNumber = descriptor.getNumber();
			this.variables = new HashMap<>();
			setMessageVariables(
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

		private void setMessageVariables(
				FieldDescriptor descriptor,
				int messageBitIndex,
				int builderBitIndex,
				FieldGeneratorInfo info,
				Map<String, String> variables,
				Context context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			variables.put("type", context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
			variables.put("group_or_message",
					descriptor.getType() == FieldDescriptor.Type.GROUP ? "Group" : "Message");
			variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
			variables.put("get_mutable_bit_builder", Helpers.generateGetBit(builderBitIndex));
			variables.put("set_mutable_bit_builder", Helpers.generateSetBit(builderBitIndex));
			variables.put("clear_mutable_bit_builder", Helpers.generateClearBit(builderBitIndex));
		}

		@Override
		public int getMessageBitIndex()
		{
			return 0;
		}

		@Override
		public int getBuilderBitIndex()
		{
			return 0;
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
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.get("deprecation") + "java.util.List<" + variables.get("type") + "> ");
			printer.println("        get" + variables.get("capitalized_name") + "List();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.get("deprecation") + variables.get("type") + " get"
					+ variables.get("capitalized_name") + "(int index);");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.get("deprecation") + "int get" + variables.get("capitalized_name") + "Count();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.get("deprecation") + "java.util.List<? extends " + variables.get("type")
					+ "OrBuilder> ");
			printer.println("        get" + variables.get("capitalized_name") + "OrBuilderList();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.get("deprecation") + variables.get("type") + "OrBuilder get"
					+ variables.get("capitalized_name") + "OrBuilder(");
			printer.println("        int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private java.util.List<" + variables.get("type") + "> " + variables.get("name") + "_;");
			FieldCommon.printExtraFieldInfo(variables, printer);

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    " + variables.get("deprecation") + "public java.util.List<" + variables.get("type") + "> get"
					+ variables.get("capitalized_name") + "List() {");
			printer.println("      return " + variables.get("name") + "_;");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    " + variables.get("deprecation") + "public java.util.List<? extends "
					+ variables.get("type") + "OrBuilder> ");
			printer.println("        get" + variables.get("capitalized_name") + "OrBuilderList() {");
			printer.println("      return " + variables.get("name") + "_;");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    @java.lang.Override");
			printer.println(
					"    " + variables.get("deprecation") + "public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("      return " + variables.get("name") + "_.size();");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    " + variables.get("deprecation") + "public " + variables.get("type") + " get"
					+ variables.get("capitalized_name") + "(int index) {");
			printer.println("      return " + variables.get("name") + "_.get(index);");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    @java.lang.Override");
			printer.println("    " + variables.get("deprecation") + "public " + variables.get("type") + "OrBuilder get"
					+ variables.get("capitalized_name") + "OrBuilder(");
			printer.println("        int index) {");
			printer.println("      return " + variables.get("name") + "_.get(index);");
			printer.println("    }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("      private java.util.List<" + variables.get("type") + "> " + variables.get("name") + "_ =");
			printer.println("        java.util.Collections.emptyList();");
			printer.println("      private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("        if (!" + variables.get("get_mutable_bit_builder") + ") {");
			printer.println("          " + variables.get("name") + "_ = new java.util.ArrayList<" + variables.get("type") + ">("
					+ variables.get("name") + "_);");
			printer.println("          " + variables.get("set_mutable_bit_builder") + ";");
			printer.println("         }");
			printer.println("      }");
			printer.println();

			printer.println("      private com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("          " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
					+ variables.get("type") + "OrBuilder> " + variables.get("name") + "Builder_;");

			printer.println();
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public java.util.List<" + variables.get("type") + "> get"
					+ variables.get("capitalized_name") + "List() {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          return java.util.Collections.unmodifiableList(" + variables.get("name") + "_);");
			printer.println("        } else {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessageList();");
			printer.println("        }");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public int get"
					+ variables.get("capitalized_name") + "Count() {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          return " + variables.get("name") + "_.size();");
			printer.println("        } else {");
			printer.println("          return " + variables.get("name") + "Builder_.getCount();");
			printer.println("        }");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + " get"
					+ variables.get("capitalized_name") + "(int index) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          return " + variables.get("name") + "_.get(index);");
			printer.println("        } else {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessage(index);");
			printer.println("        }");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder set"
					+ variables.get("capitalized_name") + "(");
			printer.println("          int index, " + variables.get("type") + " value) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.set(index, value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(index, value);");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder set"
					+ variables.get("capitalized_name") + "(");
			printer.println("          int index, " + variables.get("type") + ".Builder builderForValue) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.set(index, builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.setMessage(index, builderForValue.build());");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder add"
					+ variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.add(value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.addMessage(value);");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder add"
					+ variables.get("capitalized_name") + "(");
			printer.println("          int index, " + variables.get("type") + " value) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.add(index, value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.addMessage(index, value);");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder add"
					+ variables.get("capitalized_name") + "(");
			printer.println("          " + variables.get("type") + ".Builder builderForValue) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.add(builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.addMessage(builderForValue.build());");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder add"
					+ variables.get("capitalized_name") + "(");
			printer.println("          int index, " + variables.get("type") + ".Builder builderForValue) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.add(index, builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.addMessage(index, builderForValue.build());");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder addAll"
					+ variables.get("capitalized_name") + "(");
			printer.println("          java.lang.Iterable<? extends " + variables.get("type") + "> values) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          com.google.protobuf.AbstractMessageLite.Builder.addAll(");
			printer.println("              values, " + variables.get("name") + "_);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.addAllMessages(values);");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder clear"
					+ variables.get("capitalized_name") + "() {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          " + variables.get("name") + "_ = java.util.Collections.emptyList();");
			printer.println("          " + variables.get("clear_mutable_bit_builder") + ";");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.clear();");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public Builder remove"
					+ variables.get("capitalized_name") + "(int index) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("          " + variables.get("name") + "_.remove(index);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "Builder_.remove(index);");
			printer.println("        }");
			printer.println("        return this;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + ".Builder get"
					+ variables.get("capitalized_name") + "Builder(");
			printer.println("          int index) {");
			printer.println(
					"        return internalGet" + variables.get("capitalized_name") + "FieldBuilder().getBuilder(index);");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + "OrBuilder get"
					+ variables.get("capitalized_name") + "OrBuilder(");
			printer.println("          int index) {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          return " + variables.get("name") + "_.get(index);  } else {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessageOrBuilder(index);");
			printer.println("        }");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public java.util.List<? extends "
					+ variables.get("type") + "OrBuilder> ");
			printer.println("           get" + variables.get("capitalized_name") + "OrBuilderList() {");
			printer.println("        if (" + variables.get("name") + "Builder_ != null) {");
			printer.println("          return " + variables.get("name") + "Builder_.getMessageOrBuilderList();");
			printer.println("        } else {");
			printer.println("          return java.util.Collections.unmodifiableList(" + variables.get("name") + "_);");
			printer.println("        }");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + ".Builder add"
					+ variables.get("capitalized_name") + "Builder() {");
			printer.println("        return internalGet" + variables.get("capitalized_name") + "FieldBuilder().addBuilder(");
			printer.println("            " + variables.get("type") + ".getDefaultInstance());");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public " + variables.get("type") + ".Builder add"
					+ variables.get("capitalized_name") + "Builder(");
			printer.println("          int index) {");
			printer.println("        return internalGet" + variables.get("capitalized_name") + "FieldBuilder().addBuilder(");
			printer.println("            index, " + variables.get("type") + ".getDefaultInstance());");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.get("deprecation") + "public java.util.List<" + variables.get("type")
					+ ".Builder> ");
			printer.println("           get" + variables.get("capitalized_name") + "BuilderList() {");
			printer.println(
					"        return internalGet" + variables.get("capitalized_name") + "FieldBuilder().getBuilderList();");
			printer.println("      }");

			printer.println("      private com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("          " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
					+ variables.get("type") + "OrBuilder> ");
			printer.println("          internalGet" + variables.get("capitalized_name") + "FieldBuilder() {");
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          " + variables.get("name") + "Builder_ = new com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("              " + variables.get("type") + ", " + variables.get("type") + ".Builder, "
					+ variables.get("type") + "OrBuilder>(");
			printer.println("                  " + variables.get("name") + "_,");
			printer.println("                  " + variables.get("get_mutable_bit_builder") + ",");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.get("name") + "_ = null;");
			printer.println("        }");
			printer.println("        return " + variables.get("name") + "Builder_;");
			printer.println("      }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("      " + variables.get("name") + "_ = java.util.Collections.emptyList();");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          " + variables.get("name") + "_ = java.util.Collections.emptyList();");
			printer.println("        } else {");
			printer.println("          " + variables.get("name") + "_ = null;");
			printer.println("          " + variables.get("name") + "Builder_.clear();");
			printer.println("        }");
			printer.println("        " + variables.get("clear_mutable_bit_builder") + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (!other." + variables.get("name") + "_.isEmpty()) {");
			printer.println("            if (" + variables.get("name") + "_.isEmpty()) {");
			printer.println("              " + variables.get("name") + "_ = other." + variables.get("name") + "_;");
			printer.println("              " + variables.get("clear_mutable_bit_builder") + ";");
			printer.println("            } else {");
			printer.println("              ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("              " + variables.get("name") + "_.addAll(other." + variables.get("name") + "_);");
			printer.println("            }");
			printer.println("            onChanged();");
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          if (!other." + variables.get("name") + "_.isEmpty()) {");
			printer.println("            if (" + variables.get("name") + "Builder_.isEmpty()) {");
			printer.println("              " + variables.get("name") + "Builder_.dispose();");
			printer.println("              " + variables.get("name") + "Builder_ = null;");
			printer.println("              " + variables.get("name") + "_ = other." + variables.get("name") + "_;");
			printer.println("              " + variables.get("clear_mutable_bit_builder") + ";");
			printer.println("              " + variables.get("name") + "Builder_ = ");
			printer.println("                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?");
			printer.println("                   internalGet" + variables.get("capitalized_name") + "FieldBuilder() : null;");
			printer.println("            } else {");
			printer.println(
					"              " + variables.get("name") + "Builder_.addAllMessages(other." + variables.get("name") + "_);");
			printer.println("            }");
			printer.println("          }");
			printer.println("        }");
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("          if (" + variables.get("get_mutable_bit_builder") + ") {");
			printer.println("            " + variables.get("name") + "_ = java.util.Collections.unmodifiableList("
					+ variables.get("name") + "_);");
			printer.println("            " + variables.get("clear_mutable_bit_builder") + ";");
			printer.println("          }");
			printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
			printer.println("        } else {");
			printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "Builder_.build();");
			printer.println("        }");
		}

		@Override
		public void generateBuilderParsingCode(PrintWriter printer)
		{
			printer.println("                " + variables.get("type") + " m =");
			if (descriptor.getType() == FieldDescriptor.Type.GROUP)
			{
				printer.println("                    input.readGroup(" + variables.get("number") + ",");
				printer.println("                        " + variables.get("type") + ".parser(),");
				printer.println("                        extensionRegistry);");
			}
			else
			{
				printer.println("                    input.readMessage(");
				printer.println("                        " + variables.get("type") + ".parser(),");
				printer.println("                        extensionRegistry);");
			}
			printer.println("                if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("                  ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("                  " + variables.get("name") + "_.add(m);");
			printer.println("                } else {");
			printer.println("                  " + variables.get("name") + "Builder_.addMessage(m);");
			printer.println("                }");
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.get("group_or_message") + "Size(" + variables.get("number") + ", "
					+ variables.get("name") + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("        output.write" + variables.get("group_or_message") + "(" + variables.get("number") + ", "
					+ variables.get("name") + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public void generateFieldBuilderInitializationCode(PrintWriter printer)
		{
			printer.println("          internalGet" + variables.get("capitalized_name") + "FieldBuilder();");
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
			printer.println("      for (int i = 0; i < " + variables.get("name") + "_.size(); i++) {");
			printer.println("        output.write" + variables.get("group_or_message") + "(" + variables.get("number") + ", "
					+ variables.get("name") + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public String getBoxedType()
		{
			return variables.get("type");
		}
	}
}
