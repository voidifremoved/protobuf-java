package com.rubberjam.protobuf.compiler.java.proto3.size;

import com.google.protobuf.InternalHelpers;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.ContextVariables;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.JavaCompilerOptions;
import com.rubberjam.protobuf.compiler.java.Helpers;

import java.io.PrintWriter;



public class MessageFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final JavaContext context;
	private final int fieldNumber;
	private final ContextVariables variables;

	public MessageFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, JavaContext context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
		this.variables = new ContextVariables();
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
			FieldGeneratorInfo<JavaCompilerOptions> info,
			ContextVariables variables,
			JavaContext context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		variables.setType( context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
		variables.setGroupOrMessage(
				descriptor.getType() == FieldDescriptor.Type.GROUP ? "Group" : "Message");
		variables.setDeprecation( descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
		variables.setGetParser( "parser()");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = com.rubberjam.protobuf.compiler.java.StringUtils
					.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.setOneofName( oneofName);
			variables.setOneofCaseVariable( oneofName + "Case_");
			variables.setOneofFieldVariable( oneofName + "_");
			variables.setSetHasFieldBitToLocal( "");
			variables.setIsFieldPresentMessage( oneofName + "Case_ == " + descriptor.getNumber());
		}
		else if (InternalHelpers.hasHasbit(descriptor))
		{
			variables.setSetHasFieldBitToLocal( Helpers.generateSetBitToLocal(messageBitIndex));
			variables.setIsFieldPresentMessage( Helpers.generateGetBit(messageBitIndex));
		}
		else
		{
			variables.setSetHasFieldBitToLocal( "");
			variables.setIsFieldPresentMessage( variables.getName() + "_ != null");
		}

		variables.setGetMutableBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setSetMutableBitBuilder( Helpers.generateSetBit(builderBitIndex));
		variables.setClearMutableBitBuilder( Helpers.generateClearBit(builderBitIndex));
		variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
		variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
		variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
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
		printer.println("    " + variables.getDeprecation() + "boolean has" + variables.getCapitalizedName() + "();");
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
		printer.println("    " + variables.getDeprecation() + variables.getType() + " get"
				+ variables.getCapitalizedName() + "();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    " + variables.getDeprecation() + variables.getType() + "OrBuilder get"
				+ variables.getCapitalizedName() + "OrBuilder();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;
		if (!isRealOneof)
		{
			printer.println("    private " + variables.getType() + " " + variables.getName() + "_;");
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
		printer.println("    " + variables.getDeprecation() + "public boolean has"
				+ variables.getCapitalizedName() + "() {");
		printer.println("      return " + variables.getIsFieldPresentMessage() + ";");
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
		printer.println("    " + variables.getDeprecation() + "public " + variables.getType() + " get"
				+ variables.getCapitalizedName() + "() {");
		if (isRealOneof)
		{
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("         return (" + variables.getType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("      }");
			printer.println("      return " + variables.getType() + ".getDefaultInstance();");
		}
		else
		{
			printer.println("      return " + variables.getName() + "_ == null ? " + variables.getType()
					+ ".getDefaultInstance() : " + variables.getName() + "_;");
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
		printer.println("    " + variables.getDeprecation() + "public " + variables.getType() + "OrBuilder get"
				+ variables.getCapitalizedName() + "OrBuilder() {");
		if (isRealOneof)
		{
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("         return (" + variables.getType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("      }");
			printer.println("      return " + variables.getType() + ".getDefaultInstance();");
		}
		else
		{
			printer.println("      return " + variables.getName() + "_ == null ? " + variables.getType()
					+ ".getDefaultInstance() : " + variables.getName() + "_;");
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
			printer.println("      private " + variables.getType() + " " + variables.getName() + "_;");
		}
		printer.println("      private com.google.protobuf.SingleFieldBuilder<");
		printer.println("          " + variables.getType() + ", " + variables.getType() + ".Builder, "
				+ variables.getType() + "OrBuilder> " + variables.getName() + "Builder_;");

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
		printer.println("      " + variables.getDeprecation() + "public boolean has"
				+ variables.getCapitalizedName() + "() {");
		if (isRealOneof)
		{
			printer.println("        return " + variables.getIsFieldPresentMessage() + ";");
		}
		else
		{
			printer.println("        return " + variables.getGetHasFieldBitBuilder() + ";");
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
		printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + " get"
				+ variables.getCapitalizedName() + "() {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            return (" + variables.getType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("          }");
			printer.println("          return " + variables.getType() + ".getDefaultInstance();");
			printer.println("        } else {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            return " + variables.getName() + "Builder_.getMessage();");
			printer.println("          }");
			printer.println("          return " + variables.getType() + ".getDefaultInstance();");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          return " + variables.getName() + "_ == null ? " + variables.getType()
					+ ".getDefaultInstance() : " + variables.getName() + "_;");
			printer.println("        } else {");
			printer.println("          return " + variables.getName() + "Builder_.getMessage();");
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
		printer.println("      " + variables.getDeprecation() + "public Builder set"
				+ variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          " + variables.getOneofFieldVariable() + " = value;");
			printer.println("          " + "onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(value);");
			printer.println("        }");
			printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          " + variables.getName() + "_ = value;");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(value);");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
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
		printer.println("      " + variables.getDeprecation() + "public Builder set"
				+ variables.getCapitalizedName() + "(");
		printer.println("          " + variables.getType() + ".Builder builderForValue) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          " + variables.getOneofFieldVariable() + " = builderForValue.build();");
			printer.println("          " + "onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(builderForValue.build());");
			printer.println("        }");
			printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          " + variables.getName() + "_ = builderForValue.build();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(builderForValue.build());");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
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
		printer.println("      " + variables.getDeprecation() + "public Builder merge"
				+ variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + " &&");
			printer.println("              " + variables.getOneofFieldVariable() + " != " + variables.getType()
					+ ".getDefaultInstance()) {");
			printer.println("            " + variables.getOneofFieldVariable() + " = " + variables.getType()
					+ ".newBuilder((" + variables.getType() + ") " + variables.getOneofFieldVariable() + ")");
			printer.println("                .mergeFrom(value).buildPartial();");
			printer.println("          } else {");
			printer.println("            " + variables.getOneofFieldVariable() + " = value;");
			printer.println("          }");
			printer.println("          " + "onChanged();");
			printer.println("        } else {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            " + variables.getName() + "Builder_.mergeFrom(value);");
			printer.println("          } else {");
			printer.println("            " + variables.getName() + "Builder_.setMessage(value);");
			printer.println("          }");
			printer.println("        }");
			printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (" + variables.getGetHasFieldBitBuilder() + " &&");
			printer.println("            " + variables.getName() + "_ != null &&");
			printer.println("            " + variables.getName() + "_ != " + variables.getType()
					+ ".getDefaultInstance()) {");
			printer.println("            get" + variables.getCapitalizedName() + "Builder().mergeFrom(value);");
			printer.println("          } else {");
			printer.println("            " + variables.getName() + "_ = value;");
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.mergeFrom(value);");
			printer.println("        }");
			printer.println("        if (" + variables.getName() + "_ != null) {");
			printer.println("          " + variables.getSetHasFieldBitBuilder());
			printer.println("          " + "onChanged();");
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
		printer.println("      " + variables.getDeprecation() + "public Builder clear"
				+ variables.getCapitalizedName() + "() {");
		if (isRealOneof)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            " + variables.getOneofCaseVariable() + " = 0;");
			printer.println("            " + variables.getOneofFieldVariable() + " = null;");
			printer.println("            " + "onChanged();");
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            " + variables.getOneofCaseVariable() + " = 0;");
			printer.println("            " + variables.getOneofFieldVariable() + " = null;");
			printer.println("          }");
			printer.println("          " + variables.getName() + "Builder_.clear();");
			printer.println("        }");
		}
		else
		{
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			printer.println("        " + variables.getName() + "_ = null;");
			printer.println("        if (" + variables.getName() + "Builder_ != null) {");
			printer.println("          " + variables.getName() + "Builder_.dispose();");
			printer.println("          " + variables.getName() + "Builder_ = null;");
			printer.println("        }");
			printer.println("        " + "onChanged();");
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
		printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + ".Builder get"
				+ variables.getCapitalizedName() + "Builder() {");
		if (!isRealOneof)
		{
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
		}
		printer.println("        return internalGet" + variables.getCapitalizedName()
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
		printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + "OrBuilder get"
				+ variables.getCapitalizedName() + "OrBuilder() {");
		if (isRealOneof)
		{
			printer.println("        if ((" + variables.getIsFieldPresentMessage() + ") && (" + variables.getName()
					+ "Builder_ != null)) {");
			printer.println("          return " + variables.getName() + "Builder_.getMessageOrBuilder();");
			printer.println("        } else {");
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            return (" + variables.getType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("          }");
			printer.println("          return " + variables.getType() + ".getDefaultInstance();");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ != null) {");
			printer.println("          return " + variables.getName() + "Builder_.getMessageOrBuilder();");
			printer.println("        } else {");
			printer.println("          return " + variables.getName() + "_ == null ?");
			printer.println("              " + variables.getType() + ".getDefaultInstance() : " + variables.getName() + "_;");
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
		printer.println("          " + variables.getType() + ", " + variables.getType() + ".Builder, "
				+ variables.getType() + "OrBuilder> ");
		printer.println("          internalGet" + variables.getCapitalizedName() + "FieldBuilder() {");
		printer.println("        if (" + variables.getName() + "Builder_ == null) {");
		if (isRealOneof)
		{
			printer.println("          if (!(" + variables.getIsFieldPresentMessage() + ")) {");
			printer.println("            " + variables.getOneofFieldVariable() + " = " + variables.getType()
					+ ".getDefaultInstance();");
			printer.println("          }");
			printer.println("          " + variables.getName() + "Builder_ = new com.google.protobuf.SingleFieldBuilder<");
			printer.println("              " + variables.getType() + ", " + variables.getType() + ".Builder, "
					+ variables.getType() + "OrBuilder>(");
			printer.println("                  (" + variables.getType() + ") " + variables.getOneofFieldVariable() + ",");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.getOneofFieldVariable() + " = null;");
		}
		else
		{
			printer.println("          " + variables.getName() + "Builder_ = new com.google.protobuf.SingleFieldBuilder<");
			printer.println("              " + variables.getType() + ", " + variables.getType() + ".Builder, "
					+ variables.getType() + "OrBuilder>(");
			printer.println("                  get" + variables.getCapitalizedName() + "(),");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.getName() + "_ = null;");
		}
		printer.println("        }");
		if (isRealOneof)
		{
			printer.println("        " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
			printer.println("        " + "onChanged();");
		}
		printer.println("        return " + variables.getName() + "Builder_;");
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
				printer.println("        " + variables.getName() + "_ = null;");
			}
			printer.println("        if (" + variables.getName() + "Builder_ != null) {");
			printer.println("          " + variables.getName() + "Builder_.dispose();");
			printer.println("          " + variables.getName() + "Builder_ = null;");
			printer.println("        }");
		}
		else
		{
			printer.println("        if (" + variables.getName() + "Builder_ != null) {");
			printer.println("          " + variables.getName() + "Builder_.clear();");
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
			printer.println("            merge" + variables.getCapitalizedName() + "(other.get"
					+ variables.getCapitalizedName() + "());");
		}
		else
		{
			printer.println("        if (other.has" + variables.getCapitalizedName() + "()) {");
			printer.println("          merge" + variables.getCapitalizedName() + "(other.get"
					+ variables.getCapitalizedName() + "());");
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
		printer.println("        if (" + variables.getGetHasFieldBitFromLocal() + ") {");
		printer.println("          result." + variables.getName() + "_ = " + variables.getName() + "Builder_ == null");
		printer.println("              ? " + variables.getName() + "_");
		printer.println("              : " + variables.getName() + "Builder_.build();");
		if (getNumBitsForMessage() > 0)
		{
			printer.println("          " + variables.getSetHasFieldBitToLocal() + ";");
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
			printer.println("                input.readGroup(" + variables.getNumber() + ",");
			printer.println(
					"                    internalGet" + variables.getCapitalizedName() + "FieldBuilder().getBuilder(),");
			printer.println("                    extensionRegistry);");
		}
		else
		{
			printer.println("                input.readMessage(");
			printer.println(
					"                    internalGet" + variables.getCapitalizedName() + "FieldBuilder().getBuilder(),");
			printer.println("                    extensionRegistry);");
		}

		if (isRealOneof)
		{
			printer.println("                " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
		}
		else
		{
			printer.println("                " + variables.getSetHasFieldBitBuilder());
		}
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;

		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		
		if (isRealOneof)
		{
			printer.println("          .compute" + variables.getGroupOrMessage() + "Size(" + variables.getNumber() + ", ("
					+ variables.getType() + ") " + variables.getOneofFieldVariable() + ");");
		}
		else
		{
			printer.println("          .compute" + variables.getGroupOrMessage() + "Size(" + variables.getNumber() + ", get"
					+ variables.getCapitalizedName() + "());");
		}

		
		

		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		boolean isSyntheticOneof = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		boolean isRealOneof = descriptor.getContainingOneof() != null && !isSyntheticOneof;

		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");

		if (isRealOneof)
		{
			printer.println("        output.write" + variables.getGroupOrMessage() + "(" + variables.getNumber() + ", ("
					+ variables.getType() + ") " + variables.getOneofFieldVariable() + ");");
		}
		else
		{
			printer.println("        output.write" + variables.getGroupOrMessage() + "(" + variables.getNumber() + ", get"
					+ variables.getCapitalizedName() + "());");
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
		printer.println("          internalGet" + variables.getCapitalizedName() + "FieldBuilder();");
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.getCapitalizedName() + "() != other.has"
					+ variables.getCapitalizedName() + "()) return false;");
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
			printer.println("        if (!get" + variables.getCapitalizedName() + "()");
			printer.println("            .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
			printer.println("      }");
		}
		else
		{
			printer.println("      if (!get" + variables.getCapitalizedName() + "()");
			printer.println("          .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
		}
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
		printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
		printer.println("        hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
		printer.println("      }");
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
		printer.println("          hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		printer.println("        output.write" + variables.getGroupOrMessage() + "(" + variables.getNumber() + ", get"
				+ variables.getCapitalizedName() + "());");
		printer.println("      }");
	}

	@Override
	public String getBoxedType()
	{
		return variables.getType();
	}

	public static class RepeatedMessageFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final JavaContext context;
		private final ContextVariables variables;
		private final int fieldNumber;

		public RepeatedMessageFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, JavaContext context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.fieldNumber = descriptor.getNumber();
			this.variables = new ContextVariables();
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
				FieldGeneratorInfo<JavaCompilerOptions> info,
				ContextVariables variables,
				JavaContext context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			variables.setType( context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
			variables.setGroupOrMessage(
					descriptor.getType() == FieldDescriptor.Type.GROUP ? "Group" : "Message");
			variables.setDeprecation( descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
			variables.setGetMutableBitBuilder( Helpers.generateGetBit(builderBitIndex));
			variables.setSetMutableBitBuilder( Helpers.generateSetBit(builderBitIndex));
			variables.setClearMutableBitBuilder( Helpers.generateClearBit(builderBitIndex));
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
			printer.println("    " + variables.getDeprecation() + "java.util.List<" + variables.getType() + "> ");
			printer.println("        get" + variables.getCapitalizedName() + "List();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.getDeprecation() + variables.getType() + " get"
					+ variables.getCapitalizedName() + "(int index);");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.getDeprecation() + "int get" + variables.getCapitalizedName() + "Count();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.getDeprecation() + "java.util.List<? extends " + variables.getType()
					+ "OrBuilder> ");
			printer.println("        get" + variables.getCapitalizedName() + "OrBuilderList();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    " + variables.getDeprecation() + variables.getType() + "OrBuilder get"
					+ variables.getCapitalizedName() + "OrBuilder(");
			printer.println("        int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private java.util.List<" + variables.getType() + "> " + variables.getName() + "_;");
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
			printer.println("    " + variables.getDeprecation() + "public java.util.List<" + variables.getType() + "> get"
					+ variables.getCapitalizedName() + "List() {");
			printer.println("      return " + variables.getName() + "_;");
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
			printer.println("    " + variables.getDeprecation() + "public java.util.List<? extends "
					+ variables.getType() + "OrBuilder> ");
			printer.println("        get" + variables.getCapitalizedName() + "OrBuilderList() {");
			printer.println("      return " + variables.getName() + "_;");
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
					"    " + variables.getDeprecation() + "public int get" + variables.getCapitalizedName() + "Count() {");
			printer.println("      return " + variables.getName() + "_.size();");
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
			printer.println("    " + variables.getDeprecation() + "public " + variables.getType() + " get"
					+ variables.getCapitalizedName() + "(int index) {");
			printer.println("      return " + variables.getName() + "_.get(index);");
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
			printer.println("    " + variables.getDeprecation() + "public " + variables.getType() + "OrBuilder get"
					+ variables.getCapitalizedName() + "OrBuilder(");
			printer.println("        int index) {");
			printer.println("      return " + variables.getName() + "_.get(index);");
			printer.println("    }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("      private java.util.List<" + variables.getType() + "> " + variables.getName() + "_ =");
			printer.println("        java.util.Collections.emptyList();");
			printer.println("      private void ensure" + variables.getCapitalizedName() + "IsMutable() {");
			printer.println("        if (!" + variables.getGetMutableBitBuilder() + ") {");
			printer.println("          " + variables.getName() + "_ = new java.util.ArrayList<" + variables.getType() + ">("
					+ variables.getName() + "_);");
			printer.println("          " + variables.getSetMutableBitBuilder() + ";");
			printer.println("         }");
			printer.println("      }");
			printer.println();

			printer.println("      private com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("          " + variables.getType() + ", " + variables.getType() + ".Builder, "
					+ variables.getType() + "OrBuilder> " + variables.getName() + "Builder_;");

			printer.println();
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.getDeprecation() + "public java.util.List<" + variables.getType() + "> get"
					+ variables.getCapitalizedName() + "List() {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          return java.util.Collections.unmodifiableList(" + variables.getName() + "_);");
			printer.println("        } else {");
			printer.println("          return " + variables.getName() + "Builder_.getMessageList();");
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
			printer.println("      " + variables.getDeprecation() + "public int get"
					+ variables.getCapitalizedName() + "Count() {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          return " + variables.getName() + "_.size();");
			printer.println("        } else {");
			printer.println("          return " + variables.getName() + "Builder_.getCount();");
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
			printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + " get"
					+ variables.getCapitalizedName() + "(int index) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          return " + variables.getName() + "_.get(index);");
			printer.println("        } else {");
			printer.println("          return " + variables.getName() + "Builder_.getMessage(index);");
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
			printer.println("      " + variables.getDeprecation() + "public Builder set"
					+ variables.getCapitalizedName() + "(");
			printer.println("          int index, " + variables.getType() + " value) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.set(index, value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(index, value);");
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
			printer.println("      " + variables.getDeprecation() + "public Builder set"
					+ variables.getCapitalizedName() + "(");
			printer.println("          int index, " + variables.getType() + ".Builder builderForValue) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.set(index, builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.setMessage(index, builderForValue.build());");
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
			printer.println("      " + variables.getDeprecation() + "public Builder add"
					+ variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.add(value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.addMessage(value);");
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
			printer.println("      " + variables.getDeprecation() + "public Builder add"
					+ variables.getCapitalizedName() + "(");
			printer.println("          int index, " + variables.getType() + " value) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (value == null) {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.add(index, value);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.addMessage(index, value);");
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
			printer.println("      " + variables.getDeprecation() + "public Builder add"
					+ variables.getCapitalizedName() + "(");
			printer.println("          " + variables.getType() + ".Builder builderForValue) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.add(builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.addMessage(builderForValue.build());");
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
			printer.println("      " + variables.getDeprecation() + "public Builder add"
					+ variables.getCapitalizedName() + "(");
			printer.println("          int index, " + variables.getType() + ".Builder builderForValue) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.add(index, builderForValue.build());");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.addMessage(index, builderForValue.build());");
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
			printer.println("      " + variables.getDeprecation() + "public Builder addAll"
					+ variables.getCapitalizedName() + "(");
			printer.println("          java.lang.Iterable<? extends " + variables.getType() + "> values) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          com.google.protobuf.AbstractMessageLite.Builder.addAll(");
			printer.println("              values, " + variables.getName() + "_);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.addAllMessages(values);");
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
			printer.println("      " + variables.getDeprecation() + "public Builder clear"
					+ variables.getCapitalizedName() + "() {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          " + variables.getName() + "_ = java.util.Collections.emptyList();");
			printer.println("          " + variables.getClearMutableBitBuilder() + ";");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.clear();");
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
			printer.println("      " + variables.getDeprecation() + "public Builder remove"
					+ variables.getCapitalizedName() + "(int index) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("          " + variables.getName() + "_.remove(index);");
			printer.println("          onChanged();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "Builder_.remove(index);");
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
			printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + ".Builder get"
					+ variables.getCapitalizedName() + "Builder(");
			printer.println("          int index) {");
			printer.println(
					"        return internalGet" + variables.getCapitalizedName() + "FieldBuilder().getBuilder(index);");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + "OrBuilder get"
					+ variables.getCapitalizedName() + "OrBuilder(");
			printer.println("          int index) {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          return " + variables.getName() + "_.get(index);  } else {");
			printer.println("          return " + variables.getName() + "Builder_.getMessageOrBuilder(index);");
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
			printer.println("      " + variables.getDeprecation() + "public java.util.List<? extends "
					+ variables.getType() + "OrBuilder> ");
			printer.println("           get" + variables.getCapitalizedName() + "OrBuilderList() {");
			printer.println("        if (" + variables.getName() + "Builder_ != null) {");
			printer.println("          return " + variables.getName() + "Builder_.getMessageOrBuilderList();");
			printer.println("        } else {");
			printer.println("          return java.util.Collections.unmodifiableList(" + variables.getName() + "_);");
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
			printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + ".Builder add"
					+ variables.getCapitalizedName() + "Builder() {");
			printer.println("        return internalGet" + variables.getCapitalizedName() + "FieldBuilder().addBuilder(");
			printer.println("            " + variables.getType() + ".getDefaultInstance());");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + ".Builder add"
					+ variables.getCapitalizedName() + "Builder(");
			printer.println("          int index) {");
			printer.println("        return internalGet" + variables.getCapitalizedName() + "FieldBuilder().addBuilder(");
			printer.println("            index, " + variables.getType() + ".getDefaultInstance());");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      " + variables.getDeprecation() + "public java.util.List<" + variables.getType()
					+ ".Builder> ");
			printer.println("           get" + variables.getCapitalizedName() + "BuilderList() {");
			printer.println(
					"        return internalGet" + variables.getCapitalizedName() + "FieldBuilder().getBuilderList();");
			printer.println("      }");

			printer.println("      private com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("          " + variables.getType() + ", " + variables.getType() + ".Builder, "
					+ variables.getType() + "OrBuilder> ");
			printer.println("          internalGet" + variables.getCapitalizedName() + "FieldBuilder() {");
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          " + variables.getName() + "Builder_ = new com.google.protobuf.RepeatedFieldBuilder<");
			printer.println("              " + variables.getType() + ", " + variables.getType() + ".Builder, "
					+ variables.getType() + "OrBuilder>(");
			printer.println("                  " + variables.getName() + "_,");
			printer.println("                  " + variables.getGetMutableBitBuilder() + ",");
			printer.println("                  getParentForChildren(),");
			printer.println("                  isClean());");
			printer.println("          " + variables.getName() + "_ = null;");
			printer.println("        }");
			printer.println("        return " + variables.getName() + "Builder_;");
			printer.println("      }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("      " + variables.getName() + "_ = java.util.Collections.emptyList();");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          " + variables.getName() + "_ = java.util.Collections.emptyList();");
			printer.println("        } else {");
			printer.println("          " + variables.getName() + "_ = null;");
			printer.println("          " + variables.getName() + "Builder_.clear();");
			printer.println("        }");
			printer.println("        " + variables.getClearMutableBitBuilder() + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (!other." + variables.getName() + "_.isEmpty()) {");
			printer.println("            if (" + variables.getName() + "_.isEmpty()) {");
			printer.println("              " + variables.getName() + "_ = other." + variables.getName() + "_;");
			printer.println("              " + variables.getClearMutableBitBuilder() + ";");
			printer.println("            } else {");
			printer.println("              ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("              " + variables.getName() + "_.addAll(other." + variables.getName() + "_);");
			printer.println("            }");
			printer.println("            onChanged();");
			printer.println("          }");
			printer.println("        } else {");
			printer.println("          if (!other." + variables.getName() + "_.isEmpty()) {");
			printer.println("            if (" + variables.getName() + "Builder_.isEmpty()) {");
			printer.println("              " + variables.getName() + "Builder_.dispose();");
			printer.println("              " + variables.getName() + "Builder_ = null;");
			printer.println("              " + variables.getName() + "_ = other." + variables.getName() + "_;");
			printer.println("              " + variables.getClearMutableBitBuilder() + ";");
			printer.println("              " + variables.getName() + "Builder_ = ");
			printer.println("                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?");
			printer.println("                   internalGet" + variables.getCapitalizedName() + "FieldBuilder() : null;");
			printer.println("            } else {");
			printer.println(
					"              " + variables.getName() + "Builder_.addAllMessages(other." + variables.getName() + "_);");
			printer.println("            }");
			printer.println("          }");
			printer.println("        }");
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("        if (" + variables.getName() + "Builder_ == null) {");
			printer.println("          if (" + variables.getGetMutableBitBuilder() + ") {");
			printer.println("            " + variables.getName() + "_ = java.util.Collections.unmodifiableList("
					+ variables.getName() + "_);");
			printer.println("            " + variables.getClearMutableBitBuilder() + ";");
			printer.println("          }");
			printer.println("          result." + variables.getName() + "_ = " + variables.getName() + "_;");
			printer.println("        } else {");
			printer.println("          result." + variables.getName() + "_ = " + variables.getName() + "Builder_.build();");
			printer.println("        }");
		}

		@Override
		public void generateBuilderParsingCode(PrintWriter printer)
		{
			printer.println("                " + variables.getType() + " m =");
			if (descriptor.getType() == FieldDescriptor.Type.GROUP)
			{
				printer.println("                    input.readGroup(" + variables.getNumber() + ",");
				printer.println("                        " + variables.getType() + ".parser(),");
				printer.println("                        extensionRegistry);");
			}
			else
			{
				printer.println("                    input.readMessage(");
				printer.println("                        " + variables.getType() + ".parser(),");
				printer.println("                        extensionRegistry);");
			}
			printer.println("                if (" + variables.getName() + "Builder_ == null) {");
			printer.println("                  ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("                  " + variables.getName() + "_.add(m);");
			printer.println("                } else {");
			printer.println("                  " + variables.getName() + "Builder_.addMessage(m);");
			printer.println("                }");
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.getGroupOrMessage() + "Size(" + variables.getNumber() + ", "
					+ variables.getName() + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("        output.write" + variables.getGroupOrMessage() + "(" + variables.getNumber() + ", "
					+ variables.getName() + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public void generateFieldBuilderInitializationCode(PrintWriter printer)
		{
			printer.println("          internalGet" + variables.getCapitalizedName() + "FieldBuilder();");
		}

		@Override
		public void generateEqualsCode(PrintWriter printer)
		{
			printer.println("      if (!get" + variables.getCapitalizedName() + "List()");
			printer.println("          .equals(other.get" + variables.getCapitalizedName() + "List())) return false;");
		}

		@Override
		public void generateHashCode(PrintWriter printer)
		{
			printer.println("      if (get" + variables.getCapitalizedName() + "Count() > 0) {");
			printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
			printer.println("        hash = (53 * hash) + get" + variables.getCapitalizedName() + "List().hashCode();");
			printer.println("      }");
		}



		@Override
		public void generateSerializationCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("        output.write" + variables.getGroupOrMessage() + "(" + variables.getNumber() + ", "
					+ variables.getName() + "_.get(i));");
			printer.println("      }");
		}

		@Override
		public String getBoxedType()
		{
			return variables.getType();
		}
	}
}
