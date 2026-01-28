package com.rubberjam.protobuf.compiler.java.proto2.speed;

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
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;



public class StringFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final JavaContext context;
	private final int fieldNumber;
	private final ContextVariables variables;

	public StringFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, JavaContext context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
		this.variables = new ContextVariables();
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
		return fieldNumber;
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
			FieldGeneratorInfo<JavaCompilerOptions> info,
			ContextVariables variables,
			JavaContext context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		String defaultValue = Helpers.defaultValue(descriptor, context.getNameResolver(), context.getOptions(), true);
		variables.setDefaultValue( defaultValue);
		variables.setDefaultInit( "= " + defaultValue);
		variables.setBoxedType( "java.lang.String");
		variables.setTag( Integer.toString(Helpers.getTag(descriptor)));
		variables.setTagSize( Integer.toString(Helpers.getTagSize(descriptor)));
		variables.setNullCheck(true);
		variables.setIsStringEmpty( "com.google.protobuf.GeneratedMessage.isStringEmpty");
		variables.setWriteString( "com.google.protobuf.GeneratedMessage.writeString");
		variables.setComputeStringSize( "com.google.protobuf.GeneratedMessage.computeStringSize");
		variables.setDeprecation( descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = StringUtils.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.setOneofName( oneofName);
			variables.setOneofCaseVariable( oneofName + "Case_");
			variables.setOneofFieldVariable( oneofName + "_");
			variables.setSetHasFieldBitToLocal( "");
			variables.setSetHasFieldBitMessage( "");
			variables.setIsFieldPresentMessage( oneofName + "Case_ == " + descriptor.getNumber());
		}
		else if (InternalHelpers.hasHasbit(descriptor) || isSynthetic)
		{
			variables.setSetHasFieldBitToLocal( Helpers.generateSetBitToLocal(messageBitIndex));
			variables.setSetHasFieldBitMessage( Helpers.generateSetBit(messageBitIndex) + ";");
			variables.setIsFieldPresentMessage( Helpers.generateGetBit(messageBitIndex));
		}
		else
		{
			variables.setSetHasFieldBitToLocal( "");
			variables.setSetHasFieldBitMessage( "");
			variables.setIsFieldPresentMessage(
					"!" + variables.getIsStringEmpty() + "(" + variables.getName() + "_)");
		}

		variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
		variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
		variables.setNameMakeImmutable( variables.getName() + "_.makeImmutable()");
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
		return (com.google.protobuf.InternalHelpers.hasHasbit(descriptor) || (descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional())) ? 1 : 0;
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
			printer.println("    " + variables.getDeprecation() + "boolean has"
					+ variables.getCapitalizedName() + "();");
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
		printer.println("    " + variables.getDeprecation() + "java.lang.String get"
				+ variables.getCapitalizedName() + "();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		printer.println("    " + variables.getDeprecation() + "com.google.protobuf.ByteString");
		printer.println("        get" + variables.getCapitalizedName() + "Bytes();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println(
					"    private volatile java.lang.Object " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
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
			// String oneof fields in message class should NOT have @Override
			// (unlike primitive oneof fields which do have @Override)
			if (descriptor.getContainingOneof() == null || isSynthetic)
			{
				printer.println("    @java.lang.Override");
			}
			printer.println("    " + variables.getDeprecation() + "public boolean has"
					+ variables.getCapitalizedName() + "() {");
			printer.println("      return " + variables.getIsFieldPresentMessage() + ";");
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
		// String oneof fields in message class should NOT have @Override
		// (unlike primitive oneof fields which do have @Override)
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    @java.lang.Override");
		}
		printer.println("    " + variables.getDeprecation() + "public java.lang.String get"
				+ variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      java.lang.Object ref = " + variables.getDefaultValue() + ";");
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("        ref = " + variables.getOneofFieldVariable() + ";");
			printer.println("      }");
			printer.println("      if (ref instanceof java.lang.String) {");
			printer.println("        return (java.lang.String) ref;");
			printer.println("      } else {");
			printer.println("        com.google.protobuf.ByteString bs = ");
			printer.println("            (com.google.protobuf.ByteString) ref;");
			printer.println("        java.lang.String s = bs.toStringUtf8();");
			printer.println("        if (bs.isValidUtf8() && (" + variables.getIsFieldPresentMessage() + ")) {");
			printer.println("          " + variables.getOneofFieldVariable() + " = s;");
			printer.println("        }");
			printer.println("        return s;");
			printer.println("      }");
		}
		else
		{
			printer.println("      java.lang.Object ref = " + variables.getName() + "_;");
			printer.println("      if (ref instanceof java.lang.String) {");
			printer.println("        return (java.lang.String) ref;");
			printer.println("      } else {");
			printer.println("        com.google.protobuf.ByteString bs = ");
			printer.println("            (com.google.protobuf.ByteString) ref;");
			printer.println("        java.lang.String s = bs.toStringUtf8();");
			printer.println("        if (bs.isValidUtf8()) {");
			printer.println("          " + variables.getName() + "_ = s;");
			printer.println("        }");
			printer.println("        return s;");
			printer.println("      }");
		}
		printer.println("    }");

		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    @java.lang.Override");
		}
		printer.println("    " + variables.getDeprecation() + "public com.google.protobuf.ByteString");
		printer.println("        get" + variables.getCapitalizedName() + "Bytes() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      java.lang.Object ref = " + variables.getDefaultValue() + ";");
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("        ref = " + variables.getOneofFieldVariable() + ";");
			printer.println("      }");
			printer.println("      if (ref instanceof java.lang.String) {");
			printer.println("        com.google.protobuf.ByteString b = ");
			printer.println("            com.google.protobuf.ByteString.copyFromUtf8(");
			printer.println("                (java.lang.String) ref);");
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          " + variables.getOneofFieldVariable() + " = b;");
			printer.println("        }");
			printer.println("        return b;");
			printer.println("      } else {");
			printer.println("        return (com.google.protobuf.ByteString) ref;");
			printer.println("      }");
		}
		else
		{
			printer.println("      java.lang.Object ref = " + variables.getName() + "_;");
			printer.println("      if (ref instanceof java.lang.String) {");
			printer.println("        com.google.protobuf.ByteString b = ");
			printer.println("            com.google.protobuf.ByteString.copyFromUtf8(");
			printer.println("                (java.lang.String) ref);");
			printer.println("        " + variables.getName() + "_ = b;");
			printer.println("        return b;");
			printer.println("      } else {");
			printer.println("        return (com.google.protobuf.ByteString) ref;");
			printer.println("      }");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("      private java.lang.Object " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
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
			// String oneof fields in builder class SHOULD have @Override
			if (descriptor.getContainingOneof() != null && !isSynthetic)
			{
				printer.println("      @java.lang.Override");
			}
			printer.println("      " + variables.getDeprecation() + "public boolean has"
					+ variables.getCapitalizedName() + "() {");
			// For oneof fields, use oneofCase_ check; for regular fields, use bitField0_ check
			String hasCheck = (descriptor.getContainingOneof() != null && !isSynthetic)
					? variables.getIsFieldPresentMessage()
					: variables.getGetHasFieldBitBuilder();
			printer.println("        return " + hasCheck + ";");
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
		// String oneof fields in builder class SHOULD have @Override
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.getDeprecation() + "public java.lang.String get"
				+ variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        java.lang.Object ref = " + variables.getDefaultValue() + ";");
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          ref = " + variables.getOneofFieldVariable() + ";");
			printer.println("        }");
		}
		else
		{
			printer.println("        java.lang.Object ref = " + variables.getName() + "_;");
		}
		printer.println("        if (!(ref instanceof java.lang.String)) {");
		printer.println("          com.google.protobuf.ByteString bs =");
		printer.println("              (com.google.protobuf.ByteString) ref;");
		printer.println("          java.lang.String s = bs.toStringUtf8();");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            if (bs.isValidUtf8()) {");
			printer.println("              " + variables.getOneofFieldVariable() + " = s;");
			printer.println("            }");
			printer.println("          }");
		}
		else
		{
			printer.println("          if (bs.isValidUtf8()) {");
			printer.println("            " + variables.getName() + "_ = s;");
			printer.println("          }");
		}
		printer.println("          return s;");
		printer.println("        } else {");
		printer.println("          return (java.lang.String) ref;");
		printer.println("        }");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.GETTER,
						context,
						false,
						false,
						false));
		// String oneof fields in builder class SHOULD have @Override
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.getDeprecation() + "public com.google.protobuf.ByteString");
		printer.println("          get" + variables.getCapitalizedName() + "Bytes() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        java.lang.Object ref = " + variables.getDefaultValue() + ";");
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          ref = " + variables.getOneofFieldVariable() + ";");
			printer.println("        }");
		}
		else
		{
			printer.println("        java.lang.Object ref = " + variables.getName() + "_;");
		}
		printer.println("        if (ref instanceof String) {");
		printer.println("          com.google.protobuf.ByteString b = ");
		printer.println("              com.google.protobuf.ByteString.copyFromUtf8(");
		printer.println("                  (java.lang.String) ref);");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("          if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("            " + variables.getOneofFieldVariable() + " = b;");
			printer.println("          }");
		}
		else
		{
			printer.println("          " + variables.getName() + "_ = b;");
		}
		printer.println("          return b;");
		printer.println("        } else {");
		printer.println("          return (com.google.protobuf.ByteString) ref;");
		printer.println("        }");
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
		printer.println("      " + variables.getDeprecation() + "public Builder set"
				+ variables.getCapitalizedName() + "(");
		printer.println("          " + variables.getBoxedType() + " value) {");
		printer.println("        if (value == null) { throw new NullPointerException(); }");
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
		printer.println("        " + "onChanged();");
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
		printer.println("      " + variables.getDeprecation() + "public Builder clear"
				+ variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          " + variables.getOneofCaseVariable() + " = 0;");
			printer.println("          " + variables.getOneofFieldVariable() + " = null;");
			printer.println("          " + "onChanged();");
			printer.println("        }");
		}
		else
		{
			printer.println("        " + variables.getName() + "_ = getDefaultInstance().get"
					+ variables.getCapitalizedName() + "();");
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
		}
		printer.println("        return this;");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
						commentWriter,
						descriptor,
						FieldAccessorType.SETTER,
						context,
						true,
						false,
						false));
		printer.println("      " + variables.getDeprecation() + "public Builder set"
				+ variables.getCapitalizedName() + "Bytes(");
		printer.println("          com.google.protobuf.ByteString value) {");
		printer.println("        if (value == null) { throw new NullPointerException(); }");
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
		printer.println("        " + "onChanged();");
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
		printer.println("      " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("        " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
		}
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("            " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
			printer.println("            " + variables.getOneofFieldVariable() + " = other." + variables.getOneofFieldVariable() + ";");
			printer.println("            " + "onChanged();");
		}
		else
		{
			if (descriptor.hasPresence())
			{
				printer.println("        if (other.has" + variables.getCapitalizedName() + "()) {");
				printer.println("          " + variables.getName() + "_ = other." + variables.getName() + "_;");
				printer.println("          " + variables.getSetHasFieldBitBuilder());
				printer.println("          " + "onChanged();");
				printer.println("        }");
			}
			else
			{
				printer.println("        if (!other.get" + variables.getCapitalizedName() + "().isEmpty()) {");
				printer.println("          " + variables.getName() + "_ = other." + variables.getName() + "_;");
				printer.println("          " + variables.getSetHasFieldBitBuilder());
				printer.println("          " + "onChanged();");
				printer.println("        }");
			}
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
			printer.println("          " + variables.getSetHasFieldBitToLocal() + ";");
		}
		printer.println("        }");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("                com.google.protobuf.ByteString bs = input.readBytes();");
			printer.println("                " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
			printer.println("                " + variables.getOneofFieldVariable() + " = bs;");
		}
		else
		{
			printer.println("                " + variables.getName() + "_ = input.readBytes();");
			printer.println("                " + variables.getSetHasFieldBitBuilder());
		}
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			String valueVar = (descriptor.getContainingOneof() != null && !isSynthetic) ? variables.getOneofFieldVariable() : variables.getName() + "_";
		printer.println("        size += com.google.protobuf.GeneratedMessage.computeStringSize("
					+ variables.getNumber() + ", " + valueVar + ");");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			String valueVar = (descriptor.getContainingOneof() != null && !isSynthetic) ? variables.getOneofFieldVariable() : variables.getName() + "_";
		printer.println("        com.google.protobuf.GeneratedMessage.writeString(output, "
					+ variables.getNumber() + ", " + valueVar + ");");
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
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
			printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
			printer.println("        hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
			printer.println("      }");
		}
		else
		{
			printer.println("      hash = (37 * hash) + " + variables.getConstantName() + ";");
			printer.println("      hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");			
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
		printer.println("          hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		printer.println("        com.google.protobuf.GeneratedMessage.writeString(output, "
				+ variables.getNumber() + ", " + variables.getName() + "_);");
		printer.println("      }");
	}

	@Override
	public String getBoxedType()
	{
		return "java.lang.String";
	}

	public static class RepeatedStringFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final JavaContext context;
		private final int fieldNumber;
		private final ContextVariables variables;

		public RepeatedStringFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, JavaContext context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.fieldNumber = descriptor.getNumber();
			this.variables = new ContextVariables();
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
			return fieldNumber;
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
				FieldGeneratorInfo<JavaCompilerOptions> info,
				ContextVariables variables,
				JavaContext context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			variables.setEmptyList( "com.google.protobuf.LazyStringArrayList.emptyList()");
			variables.setNameMakeImmutable( variables.getName() + "_.makeImmutable()");
			variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
			variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
			variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
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
			printer.println("    java.util.List<java.lang.String>");
			printer.println("        get" + variables.getCapitalizedName() + "List();");
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
			printer.println("    java.lang.String get" + variables.getCapitalizedName() + "(int index);");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    com.google.protobuf.ByteString");
			printer.println("        get" + variables.getCapitalizedName() + "Bytes(int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private com.google.protobuf.LazyStringArrayList " + variables.getName() + "_ =");
			printer.println("        " + variables.getEmptyList() + ";");

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
			printer.println("    public com.google.protobuf.ProtocolStringList");
			printer.println("        get" + variables.getCapitalizedName() + "List() {");
			printer.println("      return " + variables.getName() + "_;");
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
			printer.println("    public java.lang.String get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("      return " + variables.getName() + "_.get(index);");
			printer.println("    }");

			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("    public com.google.protobuf.ByteString");
			printer.println("        get" + variables.getCapitalizedName() + "Bytes(int index) {");
			printer.println("      return " + variables.getName() + "_.getByteString(index);");
			printer.println("    }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("      private com.google.protobuf.LazyStringArrayList " + variables.getName() + "_ =");
			printer.println("          " + variables.getEmptyList() + ";");

			printer.println("      private void ensure" + variables.getCapitalizedName() + "IsMutable() {");
			printer.println("        if (!" + variables.getName() + "_.isModifiable()) {");
			printer.println("          " + variables.getName() + "_ = new com.google.protobuf.LazyStringArrayList("
					+ variables.getName() + "_);");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("      }");

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
			printer.println("      public com.google.protobuf.ProtocolStringList");
			printer.println("          get" + variables.getCapitalizedName() + "List() {");
			printer.println("        " + variables.getName() + "_.makeImmutable();");
			printer.println("        return " + variables.getName() + "_;");
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
			printer.println("      public java.lang.String get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("        return " + variables.getName() + "_.get(index);");
			printer.println("      }");
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("      public com.google.protobuf.ByteString");
			printer.println("          get" + variables.getCapitalizedName() + "Bytes(int index) {");
			printer.println("        return " + variables.getName() + "_.getByteString(index);");
			printer.println("      }");
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
			printer.println("          int index, java.lang.String value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getName() + "_.set(index, value);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
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
			printer.println("      public Builder add" + variables.getCapitalizedName() + "(");
			printer.println("          java.lang.String value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getName() + "_.add(value);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
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
			printer.println("          java.lang.Iterable<java.lang.String> values) {");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        com.google.protobuf.AbstractMessageLite.Builder.addAll(");
			printer.println("            values, " + variables.getName() + "_);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
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
			printer.println("        " + variables.getName() + "_ =");
			printer.println("          " + variables.getEmptyList() + ";");
			printer.println("        " + variables.getClearHasFieldBitBuilder() + ";");
			printer.println("        " + "onChanged();");
			printer.println("        return this;");
			printer.println("      }");
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_ADDER,
							context,
							true,
							false,
							false));
			printer.println("      public Builder add" + variables.getCapitalizedName() + "Bytes(");
			printer.println("          com.google.protobuf.ByteString value) {");
			printer.println("        if (value == null) { throw new NullPointerException(); }");
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getName() + "_.add(value);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
			printer.println("        return this;");
			printer.println("      }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("      " + variables.getName() + "_ =");
			printer.println("          " + variables.getEmptyList() + ";");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("        " + variables.getName() + "_ =");
			printer.println("            " + variables.getEmptyList() + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			printer.println("        if (!other." + variables.getName() + "_.isEmpty()) {");
			printer.println("          if (" + variables.getName() + "_.isEmpty()) {");
			printer.println("            " + variables.getName() + "_ = other." + variables.getName() + "_;");
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
			printer.println("                com.google.protobuf.ByteString bs = input.readBytes();");
			printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("                " + variables.getName() + "_.add(bs);");
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			printer.println("      {");
			printer.println("        int dataSize = 0;");
			printer.println("        for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("          dataSize += computeStringSizeNoTag("
					+ variables.getName() + "_.getRaw(i));");
			printer.println("        }");
			printer.println("        size += dataSize;");
			printer.println("        size += " + Helpers.getTagSize(descriptor) + " * get" + variables.getCapitalizedName() + "List().size();");
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
			printer.println("        com.google.protobuf.GeneratedMessage.writeString(output, "
					+ variables.getNumber() + ", " + variables.getName() + "_.getRaw(i));");
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
			return "java.lang.String";
		}
	}
}
