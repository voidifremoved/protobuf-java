package com.rubberjam.protobuf.compiler.java.proto3.speed;

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
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;



public class PrimitiveFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final JavaContext context;
	private final ContextVariables variables;
	private final int fieldNumber;

	public PrimitiveFieldGenerator(
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
			FieldGeneratorInfo<JavaCompilerOptions> info,
			ContextVariables variables,
			JavaContext context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		JavaType javaType = StringUtils.getJavaType(descriptor);

		variables.setType( StringUtils.getPrimitiveTypeName(javaType));
		variables.setBoxedType( StringUtils.boxedPrimitiveTypeName(javaType));
		variables.setFieldType( variables.getType());
		String defaultValue = Helpers.defaultValue(descriptor, context.getNameResolver(), context.getOptions(), true);
		variables.setDefaultValue( defaultValue);
		variables.setDefaultInit(
				Helpers.isDefaultValueJavaDefault(descriptor)
						? ""
						: "= " + defaultValue);
		variables.setCapitalizedType( Helpers.getCapitalizedType(descriptor));
		variables.setTag( Integer.toString(Helpers.getTag(descriptor)));
		variables.setTagSize( Integer.toString(Helpers.getTagSize(descriptor)));

		if (Helpers.isReferenceType(javaType))
		{
			variables.setNullCheck(true);
		}

		variables.setDeprecation( descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");

		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			String oneofName = StringUtils.underscoresToCamelCase(descriptor.getContainingOneof().getName(), false);
			variables.setOneofName( oneofName);
			variables.setOneofCaseVariable( oneofName + "Case_");
			variables.setOneofFieldVariable( oneofName + "_");
			variables.setSetHasFieldBitToLocal( "");
			variables.setIsFieldPresentMessage( oneofName + "Case_ == " + descriptor.getNumber());
			variables.setIsOtherFieldPresentMessage( "other.has" + variables.getCapitalizedName() + "()");
		}
		else if (InternalHelpers.hasHasbit(descriptor) || isSynthetic)
		{
			variables.setSetHasFieldBitToLocal( Helpers.generateSetBitToLocal(messageBitIndex) + ";");
			variables.setIsFieldPresentMessage( Helpers.generateGetBit(messageBitIndex));
			variables.setIsOtherFieldPresentMessage( "other.has" + variables.getCapitalizedName() + "()");
		}
		else
		{
			variables.setSetHasFieldBitToLocal( "");
			switch (descriptor.getType())
			{
			case BYTES:
				variables.setIsFieldPresentMessage( "!" + variables.getName() + "_.isEmpty()");
				variables.setIsOtherFieldPresentMessage( "!other.get" + variables.getCapitalizedName() + "().isEmpty()");
				break;
			case FLOAT:
				variables.setIsFieldPresentMessage(
						"java.lang.Float.floatToRawIntBits(" + variables.getName() + "_) != 0");
				variables.setIsOtherFieldPresentMessage(
						"java.lang.Float.floatToRawIntBits(other.get" + variables.getCapitalizedName() + "()) != 0");
				break;
			case DOUBLE:
				variables.setIsFieldPresentMessage(
						"java.lang.Double.doubleToRawLongBits(" + variables.getName() + "_) != 0");
				variables.setIsOtherFieldPresentMessage(
						"java.lang.Double.doubleToRawLongBits(other.get" + variables.getCapitalizedName() + "()) != 0");
				break;
			default:
				variables.setIsFieldPresentMessage( variables.getName() + "_ != " + variables.getDefaultValue());
				variables.setIsOtherFieldPresentMessage(
						"other.get" + variables.getCapitalizedName() + "() != " + variables.getDefaultValue());
				break;
			}
		}

		variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
		variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");

		String capitalizedType = StringUtils.toProperCase(variables.getType());
		if (javaType == JavaType.INT || javaType == JavaType.LONG || javaType == JavaType.BOOLEAN
				|| javaType == JavaType.FLOAT || javaType == JavaType.DOUBLE)
		{
			variables.setFieldListType( "com.google.protobuf.Internal." + capitalizedType + "List");
			variables.setEmptyList( "empty" + capitalizedType + "List()");
			variables.setRepeatedGet( variables.getName() + "_.get" + capitalizedType);
			variables.setRepeatedAdd( variables.getName() + "_.add" + capitalizedType);
			variables.setRepeatedSet( variables.getName() + "_.set" + capitalizedType);
		}
		else
		{
			variables.setFieldListType(
					"com.google.protobuf.Internal.ProtobufList<com.google.protobuf.ByteString>");
			variables.setEmptyList( "emptyList(com.google.protobuf.ByteString.class)");
			variables.setRepeatedGet( variables.getName() + "_.get");
			variables.setRepeatedAdd( variables.getName() + "_.add");
			variables.setRepeatedSet( variables.getName() + "_.set");
		}
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
			printer.println("    " + variables.getDeprecation() + "boolean has" + variables.getCapitalizedName() + "();");
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
		printer.println("    " + variables.getDeprecation() + variables.getType() + " get"
				+ variables.getCapitalizedName() + "();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("    private " + variables.getFieldType() + " " + variables.getName() + "_ = "
					+ variables.getDefaultValue() + ";");
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
			// Primitive oneof fields in message class SHOULD have @Override
			// (unlike string oneof fields which don't have @Override)
			printer.println("    @java.lang.Override");
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
		// Primitive oneof fields in message class SHOULD have @Override
		// (unlike string oneof fields which don't have @Override)
		printer.println("    @java.lang.Override");
		printer.println("    " + variables.getDeprecation() + "public " + variables.getType() + " get"
				+ variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("        return (" + variables.getBoxedType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("      }");
			printer.println("      return " + variables.getDefaultValue() + ";");
		}
		else
		{
			printer.println("      return " + variables.getName() + "_;");
		}
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		JavaType javaType = StringUtils.getJavaType(descriptor);
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("      private " + variables.getFieldType() + " " + variables.getName() + "_ "
					+ variables.getDefaultInit() + ";");
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
			if (descriptor.getContainingOneof() == null || isSynthetic)
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
		if (descriptor.getContainingOneof() == null || isSynthetic)
		{
			printer.println("      @java.lang.Override");
		}
		printer.println("      " + variables.getDeprecation() + "public " + variables.getType() + " get"
				+ variables.getCapitalizedName() + "() {");
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			printer.println("        if (" + variables.getIsFieldPresentMessage() + ") {");
			printer.println("          return (" + variables.getBoxedType() + ") " + variables.getOneofFieldVariable() + ";");
			printer.println("        }");
			printer.println("        return " + variables.getDefaultValue() + ";");
		}
		else
		{
			printer.println("        return " + variables.getName() + "_;");
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
		printer.println("      " + variables.getDeprecation() + "public Builder set"
				+ variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
		if (variables.isNullCheck())
		{
			printer.println("        if (value == null) { throw new NullPointerException(); }");
		}
		else
		{
			printer.println();
		}
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
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			if (javaType == JavaType.STRING || javaType == JavaType.BYTES)
			{
				printer.println("        " + variables.getName() + "_ = getDefaultInstance().get"
						+ variables.getCapitalizedName() + "();");
			}
			else
			{
				printer.println("        " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
			}
			printer.println("        " + "onChanged();");
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
		if (!Helpers.isDefaultValueJavaDefault(descriptor))
		{
			printer.println("      " + variables.getName() + "_ = " + variables.getDefaultValue() + ";");
		}
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
			printer.println("                " + variables.getOneofFieldVariable() + " = input.read"
					+ variables.getCapitalizedType() + "();");
			printer.println("                " + variables.getOneofCaseVariable() + " = " + variables.getNumber() + ";");
		}
		else
		{
			printer.println("                " + variables.getName() + "_ = input.read"
					+ variables.getCapitalizedType() + "();");
			printer.println("                " + variables.getSetHasFieldBitBuilder());
		}
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		String valueVar = variables.getName() + "_";
		if (descriptor.getContainingOneof() != null)
		{
			if (variables.getType().equals(variables.getBoxedType()))
			{
				valueVar = "(" + variables.getType() + ") " + variables.getOneofFieldVariable();
			}
			else
			{
				valueVar = "(" + variables.getType() + ")((" + variables.getBoxedType() + ") " + variables.getOneofFieldVariable() + ")";
			}
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.getCapitalizedType() + "Size(");
			printer.println("              " + variables.getNumber() + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.getCapitalizedType() + "Size("
					+ variables.getNumber() + ", " + valueVar + ");");
		}
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
			if (variables.getType().equals(variables.getBoxedType()))
			{
				valueVar = "(" + variables.getType() + ") " + variables.getOneofFieldVariable();
			}
			else
			{
				valueVar = "(" + variables.getType() + ")((" + variables.getBoxedType() + ") " + variables.getOneofFieldVariable() + ")";
			}
			printer.println("        output.write" + variables.getCapitalizedType() + "(");
			printer.println("            " + variables.getNumber() + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        output.write" + variables.getCapitalizedType() + "("
					+ variables.getNumber() + ", " + valueVar + ");");
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
			printer.println("      if (has" + variables.getCapitalizedName() + "() != other.has" + variables.getCapitalizedName() + "()) return false;");
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
			switch (StringUtils.getJavaType(descriptor))
			{
			case INT:
			case LONG:
			case BOOLEAN:
				printer.println("        if (get" + variables.getCapitalizedName() + "()");
				printer.println("            != other.get" + variables.getCapitalizedName() + "()) return false;");
				break;
			case FLOAT:
				printer.println("        if (java.lang.Float.floatToIntBits(get" + variables.getCapitalizedName() + "())");
				printer.println("            != java.lang.Float.floatToIntBits(");
				printer.println("                other.get" + variables.getCapitalizedName() + "())) return false;");
				break;
			case DOUBLE:
				printer.println("        if (java.lang.Double.doubleToLongBits(get" + variables.getCapitalizedName() + "())");
				printer.println("            != java.lang.Double.doubleToLongBits(");
				printer.println("                other.get" + variables.getCapitalizedName() + "())) return false;");
				break;
			case STRING:
			case BYTES:
				printer.println("        if (!get" + variables.getCapitalizedName() + "()");
				printer.println("            .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
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
				printer.println("      if (get" + variables.getCapitalizedName() + "()");
				printer.println("          != other.get" + variables.getCapitalizedName() + "()) return false;");
				break;
			case FLOAT:
				printer.println("      if (java.lang.Float.floatToIntBits(get" + variables.getCapitalizedName() + "())");
				printer.println("          != java.lang.Float.floatToIntBits(");
				printer.println("              other.get" + variables.getCapitalizedName() + "())) return false;");
				break;
			case DOUBLE:
				printer.println("      if (java.lang.Double.doubleToLongBits(get" + variables.getCapitalizedName() + "())");
				printer.println("          != java.lang.Double.doubleToLongBits(");
				printer.println("              other.get" + variables.getCapitalizedName() + "())) return false;");
				break;
			case STRING:
			case BYTES:
				printer.println("      if (!get" + variables.getCapitalizedName() + "()");
				printer.println("          .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
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
			printer.println("      if (has" + variables.getCapitalizedName() + "()) {");
		}
		printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
		switch (StringUtils.getJavaType(descriptor))
		{
		case INT:
			printer.println("        hash = (53 * hash) + get" + variables.getCapitalizedName() + "();");
			break;
		case LONG:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("            get" + variables.getCapitalizedName() + "());");
			break;
		case BOOLEAN:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(");
			printer.println("            get" + variables.getCapitalizedName() + "());");
			break;
		case FLOAT:
			printer.println("        hash = (53 * hash) + java.lang.Float.floatToIntBits(");
			printer.println("            get" + variables.getCapitalizedName() + "());");
			break;
		case DOUBLE:
			printer.println("        hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("            java.lang.Double.doubleToLongBits(get" + variables.getCapitalizedName() + "()));");
			break;
		case STRING:
		case BYTES:
			printer.println("        hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
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
			printer.println("          if (get" + variables.getCapitalizedName() + "()");
			printer.println("              != other.get" + variables.getCapitalizedName() + "()) return false;");
			break;
		case FLOAT:
			printer.println("          if (java.lang.Float.floatToIntBits(get" + variables.getCapitalizedName() + "())");
			printer.println("              != java.lang.Float.floatToIntBits(");
			printer.println("                  other.get" + variables.getCapitalizedName() + "())) return false;");
			break;
		case DOUBLE:
			printer.println("          if (java.lang.Double.doubleToLongBits(get" + variables.getCapitalizedName() + "())");
			printer.println("              != java.lang.Double.doubleToLongBits(");
			printer.println("                  other.get" + variables.getCapitalizedName() + "())) return false;");
			break;
		case STRING:
		case BYTES:
			printer.println("          if (!get" + variables.getCapitalizedName() + "()");
			printer.println("              .equals(other.get" + variables.getCapitalizedName() + "())) return false;");
			break;
		default:
			break;
		}
	}

	@Override
	public void generateOneofHashCode(PrintWriter printer)
	{
		printer.println("          hash = (37 * hash) + " + variables.getConstantName() + ";");
		switch (StringUtils.getJavaType(descriptor))
		{
		case INT:
			printer.println("          hash = (53 * hash) + get" + variables.getCapitalizedName() + "();");
			break;
		case LONG:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("              get" + variables.getCapitalizedName() + "());");
			break;
		case BOOLEAN:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(");
			printer.println("              get" + variables.getCapitalizedName() + "());");
			break;
		case FLOAT:
			printer.println("          hash = (53 * hash) + java.lang.Float.floatToIntBits(");
			printer.println("              get" + variables.getCapitalizedName() + "());");
			break;
		case DOUBLE:
			printer.println("          hash = (53 * hash) + com.google.protobuf.Internal.hashLong(");
			printer.println("              java.lang.Double.doubleToLongBits(get" + variables.getCapitalizedName() + "()));");
			break;
		case STRING:
		case BYTES:
			printer.println("          hash = (53 * hash) + get" + variables.getCapitalizedName() + "().hashCode();");
			break;
		default:
			break;
		}
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		boolean isSynthetic = descriptor.toProto().hasProto3Optional() && descriptor.toProto().getProto3Optional();
		printer.println("      if (" + variables.getIsFieldPresentMessage() + ") {");
		String valueVar = variables.getName() + "_";
		if (descriptor.getContainingOneof() != null && !isSynthetic)
		{
			if (variables.getType().equals(variables.getBoxedType()))
			{
				valueVar = "(" + variables.getType() + ") " + variables.getOneofFieldVariable();
			}
			else
			{
				valueVar = "(" + variables.getType() + ")((" + variables.getBoxedType() + ") " + variables.getOneofFieldVariable() + ")";
			}
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.getCapitalizedType() + "Size(");
			printer.println("              " + variables.getNumber() + ", " + valueVar + ");");
		}
		else
		{
			printer.println("        size += com.google.protobuf.CodedOutputStream");
			printer.println("          .compute" + variables.getCapitalizedType() + "Size("
					+ variables.getNumber() + ", " + valueVar + ");");
		}
		printer.println("      }");
	}

	@Override
	public String getBoxedType()
	{
		return variables.getBoxedType();
	}

	public static class RepeatedPrimitiveFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final JavaContext context;
		private final ContextVariables variables;
		private final int fieldNumber;

		public RepeatedPrimitiveFieldGenerator(
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
				FieldGeneratorInfo<JavaCompilerOptions> info,
				ContextVariables variables,
				JavaContext context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			JavaType javaType = StringUtils.getJavaType(descriptor);

			variables.setType( StringUtils.getPrimitiveTypeName(javaType));
			variables.setBoxedType( StringUtils.boxedPrimitiveTypeName(javaType));

			String capitalizedType = StringUtils.toProperCase(variables.getType()); // e.g.
																						// Int,
																						// Long
			System.err.println("Debug: javaType=" + javaType + " capitalizedType=" + capitalizedType + " type=" + variables.getType());
			if (javaType == JavaType.INT || javaType == JavaType.LONG || javaType == JavaType.BOOLEAN
					|| javaType == JavaType.FLOAT || javaType == JavaType.DOUBLE)
			{
				variables.setFieldListType( "com.google.protobuf.Internal." + capitalizedType + "List");
				variables.setEmptyList( "empty" + capitalizedType + "List()");
				variables.setRepeatedGet( variables.getName() + "_.get" + capitalizedType);
				variables.setRepeatedAdd( variables.getName() + "_.add" + capitalizedType);
				variables.setRepeatedSet( variables.getName() + "_.set" + capitalizedType);
			}
			else
			{
				// Fallback or byte string? For now primitive handles
				// numeric/bool
				// C++ logic: else { ... Internal.ProtobufList<ByteString> ... }
				// This class is PrimitiveFieldGenerator so we expect
				// numeric/bool.
			}

			variables.setNameMakeImmutable( variables.getName() + "_.makeImmutable()");
			variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
			variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
			variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
			variables.setCapitalizedType( Helpers.getCapitalizedType(descriptor));
			variables.setCapitalizedJavaType( StringUtils.toProperCase(variables.getType()));
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
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("    @SuppressWarnings(\"serial\")");
			printer.println("    private " + variables.getFieldListType() + " " + variables.getName() + "_ =");
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
			printer.println("    @java.lang.Override");
			printer.println("    public java.util.List<" + variables.getBoxedType() + ">");
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
			printer.println("    public " + variables.getType() + " get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("      return " + variables.getRepeatedGet() + "(index);");
			printer.println("    }");

			if (descriptor.isPacked())
			{
				printer.println("    private int " + variables.getName() + "MemoizedSerializedSize = -1;");
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

			FieldDescriptor.Type type = descriptor.getType();
			boolean isFixedSizeOrBool = type == FieldDescriptor.Type.BOOL ||
					type == FieldDescriptor.Type.FIXED32 || type == FieldDescriptor.Type.SFIXED32 || type == FieldDescriptor.Type.FLOAT ||
					type == FieldDescriptor.Type.FIXED64 || type == FieldDescriptor.Type.SFIXED64 || type == FieldDescriptor.Type.DOUBLE;
			if (isFixedSizeOrBool)
			{
				printer.println("      private void ensure" + variables.getCapitalizedName() + "IsMutable(int capacity) {");
				printer.println("        if (!" + variables.getName() + "_.isModifiable()) {");
				printer.println("          " + variables.getName() + "_ = makeMutableCopy(" + variables.getName() + "_, capacity);");
				printer.println("        }");
				printer.println("        " + variables.getSetHasFieldBitBuilder());
				printer.println("      }");
			}

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
			printer.println("      public java.util.List<" + variables.getBoxedType() + ">");
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
			printer.println("      public " + variables.getType() + " get" + variables.getCapitalizedName() + "(int index) {");
			printer.println("        return " + variables.getRepeatedGet() + "(index);");
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
			printer.println("          int index, " + variables.getType() + " value) {");
			printer.println();
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getRepeatedSet() + "(index, value);");
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
			printer.println("      public Builder add" + variables.getCapitalizedName() + "(" + variables.getType() + " value) {");
			printer.println();
			printer.println("        ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("        " + variables.getRepeatedAdd() + "(value);");
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
			printer.println("          java.lang.Iterable<? extends " + variables.getBoxedType() + "> values) {");
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
			printer.println("        " + variables.getName() + "_ = " + variables.getEmptyList() + ";");
			printer.println("        " + variables.getClearHasFieldBitBuilder());
			printer.println("        " + "onChanged();");
			printer.println("        return this;");
			printer.println("      }");
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
			printer.println("                " + variables.getType() + " v = input.read" + variables.getCapitalizedType() + "();");
			printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable();");
			printer.println("                " + variables.getRepeatedAdd() + "(v);");
		}

		@Override
		public void generateBuilderParsingCodeFromPacked(PrintWriter printer)
		{
			printer.println("                int length = input.readRawVarint32();");
			printer.println("                int limit = input.pushLimit(length);");
			FieldDescriptor.Type type = descriptor.getType();
			boolean isFixedSizeOrBool = type == FieldDescriptor.Type.BOOL ||
					type == FieldDescriptor.Type.FIXED32 || type == FieldDescriptor.Type.SFIXED32 || type == FieldDescriptor.Type.FLOAT ||
					type == FieldDescriptor.Type.FIXED64 || type == FieldDescriptor.Type.SFIXED64 || type == FieldDescriptor.Type.DOUBLE;
			if (isFixedSizeOrBool)
			{
				int size = 0;
				switch (type)
				{
				case FIXED32:
				case SFIXED32:
				case FLOAT:
					size = 4;
					break;
				case FIXED64:
				case SFIXED64:
				case DOUBLE:
					size = 8;
					break;
				case BOOL:
					size = 1;
					break;
				default:
					throw new IllegalStateException("Should not reach here");
				}
				printer.println("                int alloc = length > 4096 ? 4096 : length;");
				printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable(alloc / " + size + ");");
			}
			else
			{
				printer.println("                ensure" + variables.getCapitalizedName() + "IsMutable();");
			}
			printer.println("                while (input.getBytesUntilLimit() > 0) {");
			printer.println("                  " + variables.getRepeatedAdd() + "(input.read" + variables.getCapitalizedType() + "());");
			printer.println("                }");
			printer.println("                input.popLimit(limit);");
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			printer.println("      {");
			printer.println("        int dataSize = 0;");
			FieldDescriptor.Type type = descriptor.getType();
			boolean isFixedSize = type == FieldDescriptor.Type.FIXED32 || type == FieldDescriptor.Type.SFIXED32
					|| type == FieldDescriptor.Type.FLOAT || type == FieldDescriptor.Type.FIXED64
					|| type == FieldDescriptor.Type.SFIXED64 || type == FieldDescriptor.Type.DOUBLE
					|| type == FieldDescriptor.Type.BOOL;
			if (isFixedSize)
			{
				int size = 0;
				switch (type)
				{
				case FIXED32:
				case SFIXED32:
				case FLOAT:
					size = 4;
					break;
				case FIXED64:
				case SFIXED64:
				case DOUBLE:
					size = 8;
					break;
				case BOOL:
					size = 1;
					break;
				default:
					throw new IllegalStateException("Should not reach here");
				}
				printer.println("        dataSize = " + size + " * get" + variables.getCapitalizedName() + "List().size();");
			}
			else
			{
				printer.println("        for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
				printer.println("          dataSize += com.google.protobuf.CodedOutputStream");
				printer.println("            .compute" + variables.getCapitalizedType() + "SizeNoTag("
						+ variables.getName() + "_.get" + variables.getCapitalizedJavaType() + "(i));");
				printer.println("        }");
			}
			printer.println("        size += dataSize;");
			if (descriptor.isPacked())
			{
				printer.println("        if (!get" + variables.getCapitalizedName() + "List().isEmpty()) {");
				printer.println("          size += " + Helpers.getTagSize(descriptor) + ";");
				printer.println("          size += com.google.protobuf.CodedOutputStream");
				printer.println("              .computeInt32SizeNoTag(dataSize);");
				printer.println("        }");
				printer.println("        " + variables.getName() + "MemoizedSerializedSize = dataSize;");
			}
			else
			{
				printer.println("        size += " + Helpers.getTagSize(descriptor) + " * get" + variables.getCapitalizedName() + "List().size();");
			}
			printer.println("      }");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			if (descriptor.isPacked())
			{
				printer.println("      if (get" + variables.getCapitalizedName() + "List().size() > 0) {");
				printer.println("        output.writeUInt32NoTag(" + Helpers.getTag(descriptor) + ");");
				printer.println("        output.writeUInt32NoTag(" + variables.getName() + "MemoizedSerializedSize);");
				printer.println("      }");
				printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
				printer.println("        output.write" + variables.getCapitalizedType() + "NoTag("
						+ variables.getName() + "_.get" + variables.getCapitalizedJavaType() + "(i));");
				printer.println("      }");
			}
			else
			{
				printer.println("      for (int i = 0; i < " + variables.getName() + "_.size(); i++) {");
				printer.println("        output.write" + variables.getCapitalizedType() + "("
						+ variables.getNumber() + ", " + variables.getName() + "_.get" + variables.getCapitalizedJavaType() + "(i));");
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
			return variables.getBoxedType();
		}
	}
}
