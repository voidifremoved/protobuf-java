package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.ContextVariables;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.IndentPrinter;
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;



public class MapFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final int fieldNumber;
	private final ContextVariables variables;

	public MapFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
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
		return fieldNumber;
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
			ContextVariables variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		variables.setType( context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
		FieldDescriptor key = descriptor.getMessageType().findFieldByName("key");
		FieldDescriptor value = descriptor.getMessageType().findFieldByName("value");

		// Simplified map logic for now, generating as a MapField
		variables.setKeyType( StringUtils.getPrimitiveTypeName(StringUtils.getJavaType(key)));
		variables.setBoxedKeyType( StringUtils.boxedPrimitiveTypeName(StringUtils.getJavaType(key)));
		variables.setValueType( StringUtils.getPrimitiveTypeName(StringUtils.getJavaType(value))); // Need
																										// to
																										// handle
																										// Objects
		variables.setBoxedValueType( StringUtils.boxedPrimitiveTypeName(StringUtils.getJavaType(value)));

		if (StringUtils.getJavaType(value) == JavaType.MESSAGE)
		{
			String messageType = context.getNameResolver().getImmutableClassName(value.getMessageType());
			variables.setValueType( messageType);
			variables.setBoxedValueType( messageType);
			variables.setValueOrBuilderType( messageType + "OrBuilder");
			variables.setValueBuilderType( messageType + ".Builder");
			variables.setUseBuildMethod( "true");
		}
		else
		{
			variables.setUseBuildMethod( "false");
		}

		if (Helpers.isReferenceType(StringUtils.getJavaType(value)))
		{
			variables.setIsValueNullable( "true");
		}
		else
		{
			variables.setIsValueNullable( "false");
		}

		if (StringUtils.getJavaType(value) == JavaType.ENUM)
		{
			variables.setValueType( context.getNameResolver().getImmutableClassName(value.getEnumType()));
			variables.setBoxedValueType( context.getNameResolver().getImmutableClassName(value.getEnumType()));
		}

		variables.setTypeParameters( variables.getBoxedKeyType() + ", " + variables.getBoxedValueType());

		if (StringUtils.getJavaType(value) == JavaType.ENUM)
		{
			variables.setWireValueType( "java.lang.Integer");
			variables.setWireTypeParameters( variables.getBoxedKeyType() + ", java.lang.Integer");
		}
		else
		{
			variables.setWireValueType( variables.getBoxedValueType());
			variables.setWireTypeParameters( variables.getTypeParameters());
		}

		FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
		FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");

		variables.setKeyWireType( "com.google.protobuf.WireFormat.FieldType." + keyField.getLiteType().name());
		variables.setValueWireType( "com.google.protobuf.WireFormat.FieldType." + valueField.getLiteType().name());

		variables.setKeyDefaultValue( Helpers.defaultValue(keyField, context.getNameResolver(), context.getOptions(), true));
		variables.setValueDefaultValue( Helpers.defaultValue(valueField, context.getNameResolver(), context.getOptions(), true));

		if (Helpers.isReferenceType(StringUtils.getJavaType(keyField)))
		{
			variables.setNullCheck(true);
		}
		if (Helpers.isReferenceType(StringUtils.getJavaType(valueField)) && StringUtils.getJavaType(valueField) != JavaType.ENUM)
		{
			variables.setValueNullCheck( "if (value == null) { throw new NullPointerException(\"map value\"); }");
		}

		String mapEntryProtoName = descriptor.getMessageType().getFullName();
		String mapEntryDescriptorName = "internal_static_" + mapEntryProtoName.replace('.', '_') + "_descriptor";
		String outerClassName = context.getNameResolver().getImmutableClassName(descriptor.getFile());
		variables.setDescriptorCall( outerClassName + "." + mapEntryDescriptorName);

		JavaType keyJavaType = StringUtils.getJavaType(keyField);
		String capitalizedKeyType;
		switch (keyJavaType) {
			case INT: capitalizedKeyType = "Integer"; break;
			case LONG: capitalizedKeyType = "Long"; break;
			case BOOLEAN: capitalizedKeyType = "Boolean"; break;
			case STRING: capitalizedKeyType = "String"; break;
			default: capitalizedKeyType = "String"; // Fallback
		}
		variables.setCapitalizedKeyType( capitalizedKeyType);
		variables.setGetHasFieldBitBuilder( Helpers.generateGetBit(builderBitIndex));
		variables.setGetHasFieldBitFromLocal( Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.setSetHasFieldBitBuilder( Helpers.generateSetBit(builderBitIndex) + ";");
		variables.setClearHasFieldBitBuilder( Helpers.generateClearBit(builderBitIndex) + ";");
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
		printer.println("    int get" + variables.getCapitalizedName() + "Count();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    boolean contains" + variables.getCapitalizedName() + "(");
		printer.println("        " + variables.getKeyType() + " key);");
		printer.println("    /**");
		printer.println("     * Use {@link #get" + variables.getCapitalizedName() + "Map()} instead.");
		printer.println("     */");
		printer.println("    @java.lang.Deprecated");
		printer.println("    java.util.Map<" + variables.getTypeParameters() + ">");
		printer.println("    get" + variables.getCapitalizedName() + "();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    java.util.Map<" + variables.getTypeParameters() + ">");
		printer.println("    get" + variables.getCapitalizedName() + "Map();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("    /* nullable */");
			if (printer instanceof IndentPrinter) {
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(\n");
			} else {
				printer.println(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
			}
		} else {
			printer.println("    " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
		}
		printer.println("        " + variables.getKeyType() + " key,");
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("        /* nullable */");
			if (printer instanceof IndentPrinter) {
				FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");
				boolean isEnum = valueField.getJavaType() == FieldDescriptor.JavaType.ENUM;
				String separator = isEnum ? "         " : " ";
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + separator + "defaultValue);\n");
			} else {
				printer.println(variables.getValueType() + "         defaultValue);");
			}
		} else {
			printer.println("        " + variables.getValueType() + " defaultValue);");
		}
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrThrow(");
		printer.println("        " + variables.getKeyType() + " key);");

		if (StringUtils.getJavaType(descriptor.getMessageType().findFieldByName("value")) == JavaType.ENUM) {
			printer.println("    /**");
			printer.println("     * Use {@link #get" + variables.getCapitalizedName() + "ValueMap()} instead.");
			printer.println("     */");
			printer.println("    @java.lang.Deprecated");
			printer.println("    java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "Value();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "ValueMap();");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    int get" + variables.getCapitalizedName() + "ValueOrDefault(");
			printer.println("        " + variables.getKeyType() + " key,");
			printer.println("        int defaultValue);");
			Helpers.writeDocComment(
					printer,
					"    ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("    int get" + variables.getCapitalizedName() + "ValueOrThrow(");
			printer.println("        " + variables.getKeyType() + " key);");
		}
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("    private static final class " + variables.getCapitalizedName() + "DefaultEntryHolder {");
		printer.println("      static final com.google.protobuf.MapEntry<");
		printer.println("          " + variables.getWireTypeParameters() + "> defaultEntry =");
		printer.println("              com.google.protobuf.MapEntry");
		printer.println("              .<" + variables.getWireTypeParameters() + ">newDefaultInstance(");
		printer.println("                  " + variables.getDescriptorCall() + ", ");
		printer.println("                  " + variables.getKeyWireType() + ",");
		printer.println("                  " + variables.getKeyDefaultValue() + ",");
		printer.println("                  " + variables.getValueWireType() + ",");
		if (StringUtils.getJavaType(descriptor.getMessageType().findFieldByName("value")) == JavaType.ENUM) {
			printer.println("                  " + variables.getValueDefaultValue() + ".getNumber());");
		} else {
			printer.println("                  " + variables.getValueDefaultValue() + ");");
		}
		printer.println("    }");
		printer.println("    @SuppressWarnings(\"serial\")");
		printer.println("    private com.google.protobuf.MapField<");
		printer.println("        " + variables.getWireTypeParameters() + "> " + variables.getName() + "_;");
		printer.println("    private com.google.protobuf.MapField<" + variables.getWireTypeParameters() + ">");
		printer.println("    internalGet" + variables.getCapitalizedName() + "() {");
		printer.println("      if (" + variables.getName() + "_ == null) {");
		printer.println("        return com.google.protobuf.MapField.emptyMapField(");
		printer.println("            " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry);");
		printer.println("      }");
		printer.println("      return " + variables.getName() + "_;");
		printer.println("    }");

		boolean isEnum = StringUtils.getJavaType(descriptor.getMessageType().findFieldByName("value")) == JavaType.ENUM;

		if (isEnum) {
			String valueType = variables.getValueType();
			printer.println("    private static final");
			printer.println("    com.google.protobuf.Internal.MapAdapter.Converter<");
			printer.println("        java.lang.Integer, " + valueType + "> " + variables.getName() + "ValueConverter =");
			printer.println("            com.google.protobuf.Internal.MapAdapter.newEnumConverter(");
			printer.println("                " + valueType + ".internalGetValueMap(),");
			printer.println("                " + valueType + ".UNRECOGNIZED);");
			printer.println("    private static final java.util.Map<" + variables.getTypeParameters() + ">");
			printer.println("    internalGetAdapted" + variables.getCapitalizedName() + "Map(");
			printer.println("        java.util.Map<" + variables.getWireTypeParameters() + "> map) {");
			printer.println("      return new com.google.protobuf.Internal.MapAdapter<");
			printer.println("          " + variables.getTypeParameters() + ", java.lang.Integer>(");
			printer.println("              map, " + variables.getName() + "ValueConverter);");
			printer.println("    }");
		}

		printer.println("    public int get" + variables.getCapitalizedName() + "Count() {");
		printer.println("      return internalGet" + variables.getCapitalizedName() + "().getMap().size();");
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
		printer.println("    public boolean contains" + variables.getCapitalizedName() + "(");
		printer.println("        " + variables.getKeyType() + " key) {");
		if (variables.isNullCheck()) { // Reference types need null check?
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		printer.println("      return internalGet" + variables.getCapitalizedName() + "().getMap().containsKey(key);");
		printer.println("    }");

		printer.println("    /**");
		printer.println("     * Use {@link #get" + variables.getCapitalizedName() + "Map()} instead.");
		printer.println("     */");
		printer.println("    @java.lang.Override");
		printer.println("    @java.lang.Deprecated");
		if (isEnum) {
			printer.println("    public java.util.Map<" + variables.getTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "() {");
		} else {
			printer.println("    public java.util.Map<" + variables.getTypeParameters() + "> get" + variables.getCapitalizedName()
					+ "() {");
		}
		printer.println("      return get" + variables.getCapitalizedName() + "Map();");
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
		if (isEnum) {
			printer.println("    public java.util.Map<" + variables.getTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "Map() {");
		} else {
			printer.println("    public java.util.Map<" + variables.getTypeParameters() + "> get" + variables.getCapitalizedName()
					+ "Map() {");
		}
		if (isEnum) {
			printer.println("      return internalGetAdapted" + variables.getCapitalizedName() + "Map(");
			printer.println("          internalGet" + variables.getCapitalizedName() + "().getMap());}");
		} else {
			printer.println("      return internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("    }");
		}

		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    @java.lang.Override");
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("    public /* nullable */");
			if (printer instanceof IndentPrinter) {
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(\n");
			} else {
				printer.println(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
			}
		} else {
			printer.println("    public " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
		}
		printer.println("        " + variables.getKeyType() + " key,");
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("        /* nullable */");
			if (printer instanceof IndentPrinter) {
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + " defaultValue) {\n");
			} else {
				printer.println(variables.getValueType() + " defaultValue) {");
			}
		} else {
			printer.println("        " + variables.getValueType() + " defaultValue) {");
		}
		if (variables.isNullCheck()) {
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		printer.println("      java.util.Map<" + variables.getWireTypeParameters() + "> map =");
		printer.println("          internalGet" + variables.getCapitalizedName() + "().getMap();");
		if (isEnum) {
			printer.println("      return map.containsKey(key)");
			printer.println("             ? " + variables.getName() + "ValueConverter.doForward(map.get(key))");
			printer.println("             : defaultValue;");
		} else {
			printer.println("      return map.containsKey(key) ? map.get(key) : defaultValue;");
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
		printer.println("    public " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrThrow(");
		printer.println("        " + variables.getKeyType() + " key) {");
		if (variables.isNullCheck()) {
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		printer.println("      java.util.Map<" + variables.getWireTypeParameters() + "> map =");
		printer.println("          internalGet" + variables.getCapitalizedName() + "().getMap();");
		printer.println("      if (!map.containsKey(key)) {");
		printer.println("        throw new java.lang.IllegalArgumentException();");
		printer.println("      }");
		if (isEnum) {
			printer.println("      return " + variables.getName() + "ValueConverter.doForward(map.get(key));");
		} else {
			printer.println("      return map.get(key);");
		}
		printer.println("    }");

		if (isEnum) {
			printer.println("    /**");
			printer.println("     * Use {@link #getStringToEnumValueMap()} instead.");
			printer.println("     */");
			printer.println("    @java.lang.Override");
			printer.println("    @java.lang.Deprecated");
			printer.println("    public java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "Value() {");
			printer.println("      return get" + variables.getCapitalizedName() + "ValueMap();");
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
			printer.println("    public java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("    get" + variables.getCapitalizedName() + "ValueMap() {");
			printer.println("      return internalGet" + variables.getCapitalizedName() + "().getMap();");
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
			printer.println("    public int get" + variables.getCapitalizedName() + "ValueOrDefault(");
			printer.println("        " + variables.getKeyType() + " key,");
			printer.println("        int defaultValue) {");
			if (variables.isNullCheck()) {
				printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
			}
			printer.println("      java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("          internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("      return map.containsKey(key) ? map.get(key) : defaultValue;");
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
			printer.println("    public int get" + variables.getCapitalizedName() + "ValueOrThrow(");
			printer.println("        " + variables.getKeyType() + " key) {");
			if (variables.isNullCheck()) {
				printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
			}
			printer.println("      java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("          internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("      if (!map.containsKey(key)) {");
			printer.println("        throw new java.lang.IllegalArgumentException();");
			printer.println("      }");
			printer.println("      return map.get(key);");
			printer.println("    }");
		}
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		boolean useBuildMethod = Boolean.parseBoolean(variables.getUseBuildMethod());
		boolean isEnum = StringUtils.getJavaType(descriptor.getMessageType().findFieldByName("value")) == JavaType.ENUM;

		if (useBuildMethod) {
			String converterClass = variables.getCapitalizedName() + "Converter";
			String converterInstance = variables.getName() + "Converter";
			String keyType = variables.getBoxedKeyType();
			String valueOrBuilderType = variables.getValueOrBuilderType();
			String valueType = variables.getValueType();
			String valueBuilderType = variables.getValueBuilderType();
			String defaultEntryHolder = variables.getCapitalizedName() + "DefaultEntryHolder";

			printer.println("      private static final class " + converterClass + " implements com.google.protobuf.MapFieldBuilder.Converter<" + keyType + ", " + valueOrBuilderType + ", " + valueType + "> {");
			printer.println("        @java.lang.Override");
			printer.println("        public " + valueType + " build(" + valueOrBuilderType + " val) {");
			printer.println("          if (val instanceof " + valueType + ") { return (" + valueType + ") val; }");
			printer.println("          return ((" + valueBuilderType + ") val).build();");
			printer.println("        }");
			printer.println();
			printer.println("        @java.lang.Override");
			printer.println("        public com.google.protobuf.MapEntry<" + keyType + ", " + valueType + "> defaultEntry() {");
			printer.println("          return " + defaultEntryHolder + ".defaultEntry;");
			printer.println("        }");
			printer.println("      };");
			printer.println("      private static final " + converterClass + " " + converterInstance + " = new " + converterClass + "();");

			printer.println();
			printer.println("      private com.google.protobuf.MapFieldBuilder<");
			printer.println("          " + keyType + ", " + valueOrBuilderType + ", " + valueType + ", " + valueBuilderType + "> " + variables.getName() + "_;");

			printer.println("      private com.google.protobuf.MapFieldBuilder<" + keyType + ", " + valueOrBuilderType + ", " + valueType + ", " + valueBuilderType + ">");
			printer.println("          internalGet" + variables.getCapitalizedName() + "() {");
			printer.println("        if (" + variables.getName() + "_ == null) {");
			printer.println("          return new com.google.protobuf.MapFieldBuilder<>(" + converterInstance + ");");
			printer.println("        }");
			printer.println("        return " + variables.getName() + "_;");
			printer.println("      }");

			printer.println("      private com.google.protobuf.MapFieldBuilder<" + keyType + ", " + valueOrBuilderType + ", " + valueType + ", " + valueBuilderType + ">");
			printer.println("          internalGetMutable" + variables.getCapitalizedName() + "() {");
			printer.println("        if (" + variables.getName() + "_ == null) {");
			printer.println("          " + variables.getName() + "_ = new com.google.protobuf.MapFieldBuilder<>(" + converterInstance + ");");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        onChanged();");
			printer.println("        return " + variables.getName() + "_;");
			printer.println("      }");
		} else {
			printer.println("      private com.google.protobuf.MapField<");
			printer.println("          " + variables.getWireTypeParameters() + "> " + variables.getName() + "_;");

			printer.println("      private com.google.protobuf.MapField<" + variables.getWireTypeParameters() + ">");
			printer.println("          internalGet" + variables.getCapitalizedName() + "() {");
			printer.println("        if (" + variables.getName() + "_ == null) {");
			printer.println("          return com.google.protobuf.MapField.emptyMapField(");
			printer.println("              " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry);");
			printer.println("        }");
			printer.println("        return " + variables.getName() + "_;");
			printer.println("      }");

			printer.println("      private com.google.protobuf.MapField<" + variables.getWireTypeParameters() + ">");
			printer.println("          internalGetMutable" + variables.getCapitalizedName() + "() {");
			printer.println("        if (" + variables.getName() + "_ == null) {");
			printer.println("          " + variables.getName() + "_ = com.google.protobuf.MapField.newMapField(");
			printer.println("              " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry);");
			printer.println("        }");
			printer.println("        if (!" + variables.getName() + "_.isMutable()) {");
			printer.println("          " + variables.getName() + "_ = " + variables.getName() + "_.copy();");
			printer.println("        }");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        onChanged();");
			printer.println("        return " + variables.getName() + "_;");
			printer.println("      }");
		}

		printer.println("      public int get" + variables.getCapitalizedName() + "Count() {");
		if (useBuildMethod) {
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().ensureBuilderMap().size();");
		} else {
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().getMap().size();");
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
		printer.println("      @java.lang.Override");
		printer.println("      public boolean contains" + variables.getCapitalizedName() + "(");
		printer.println("          " + variables.getKeyType() + " key) {");
		if (variables.isNullCheck()) {
			printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		if (useBuildMethod) {
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().ensureBuilderMap().containsKey(key);");
		} else {
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().getMap().containsKey(key);");
		}
		printer.println("      }");

		printer.println("      /**");
		printer.println("       * Use {@link #get" + variables.getCapitalizedName() + "Map()} instead.");
		printer.println("       */");
		printer.println("      @java.lang.Override");
		printer.println("      @java.lang.Deprecated");
		if (isEnum) {
			printer.println("      public java.util.Map<" + variables.getTypeParameters() + ">");
			printer.println("      get" + variables.getCapitalizedName() + "() {");
		} else {
			printer.println("      public java.util.Map<" + variables.getTypeParameters() + "> get" + variables.getCapitalizedName()
					+ "() {");
		}
		printer.println("        return get" + variables.getCapitalizedName() + "Map();");
		printer.println("      }");

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      @java.lang.Override");
		if (isEnum) {
			printer.println("      public java.util.Map<" + variables.getTypeParameters() + ">");
			printer.println("      get" + variables.getCapitalizedName() + "Map() {");
		} else {
			printer.println("      public java.util.Map<" + variables.getTypeParameters() + "> get" + variables.getCapitalizedName()
					+ "Map() {");
		}
		if (useBuildMethod) {
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().getImmutableMap();");
			printer.println("      }");
		} else {
			if (isEnum) {
				printer.println("        return internalGetAdapted" + variables.getCapitalizedName() + "Map(");
				printer.println("            internalGet" + variables.getCapitalizedName() + "().getMap());}");
			} else {
				printer.println("        return internalGet" + variables.getCapitalizedName() + "().getMap();");
				printer.println("      }");
			}
		}

		Helpers.writeDocComment(
				printer,
				"      ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("      @java.lang.Override");
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("      public /* nullable */");
			if (printer instanceof IndentPrinter) {
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(\n");
			} else {
				printer.println(variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
			}
		} else {
			printer.println("      public " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrDefault(");
		}
		printer.println("          " + variables.getKeyType() + " key,");
		if (Boolean.parseBoolean(variables.getIsValueNullable())) {
			printer.println("          /* nullable */");
			if (printer instanceof IndentPrinter) {
				((IndentPrinter) printer).printNoIndent(variables.getValueType() + " defaultValue) {\n");
			} else {
				printer.println(variables.getValueType() + " defaultValue) {");
			}
		} else {
			printer.println("          " + variables.getValueType() + " defaultValue) {");
		}
		if (variables.isNullCheck()) {
			printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		if (useBuildMethod) {
			printer.println("        java.util.Map<" + variables.getBoxedKeyType() + ", " + variables.getValueOrBuilderType() + "> map = internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap();");
			printer.println("        return map.containsKey(key) ? " + variables.getName() + "Converter.build(map.get(key)) : defaultValue;");
		} else {
			printer.println("        java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("            internalGet" + variables.getCapitalizedName() + "().getMap();");
			if (isEnum) {
				printer.println("        return map.containsKey(key)");
				printer.println("               ? " + variables.getName() + "ValueConverter.doForward(map.get(key))");
				printer.println("               : defaultValue;");
			} else {
				printer.println("        return map.containsKey(key) ? map.get(key) : defaultValue;");
			}
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
		printer.println("      @java.lang.Override");
		printer.println("      public " + variables.getValueType() + " get" + variables.getCapitalizedName() + "OrThrow(");
		printer.println("          " + variables.getKeyType() + " key) {");
		if (variables.isNullCheck()) {
			printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		if (useBuildMethod) {
			printer.println("        java.util.Map<" + variables.getBoxedKeyType() + ", " + variables.getValueOrBuilderType() + "> map = internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap();");
			printer.println("        if (!map.containsKey(key)) {");
			printer.println("          throw new java.lang.IllegalArgumentException();");
			printer.println("        }");
			printer.println("        return " + variables.getName() + "Converter.build(map.get(key));");
		} else {
			printer.println("        java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("            internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("        if (!map.containsKey(key)) {");
			printer.println("          throw new java.lang.IllegalArgumentException();");
			printer.println("        }");
			if (isEnum) {
				printer.println("        return " + variables.getName() + "ValueConverter.doForward(map.get(key));");
			} else {
				printer.println("        return map.get(key);");
			}
		}
		printer.println("      }");

		if (isEnum) {
			printer.println("      /**");
			printer.println("       * Use {@link #get" + variables.getCapitalizedName() + "ValueMap()} instead.");
			printer.println("       */");
			printer.println("      @java.lang.Override");
			printer.println("      @java.lang.Deprecated");
			printer.println("      public java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("      get" + variables.getCapitalizedName() + "Value() {");
			printer.println("        return get" + variables.getCapitalizedName() + "ValueMap();");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      @java.lang.Override");
			printer.println("      public java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("      get" + variables.getCapitalizedName() + "ValueMap() {");
			printer.println("        return internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      @java.lang.Override");
			printer.println("      public int get" + variables.getCapitalizedName() + "ValueOrDefault(");
			printer.println("          " + variables.getKeyType() + " key,");
			printer.println("          int defaultValue) {");
			if (variables.isNullCheck()) {
				printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
			}
			printer.println("        java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("            internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("        return map.containsKey(key) ? map.get(key) : defaultValue;");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      @java.lang.Override");
			printer.println("      public int get" + variables.getCapitalizedName() + "ValueOrThrow(");
			printer.println("          " + variables.getKeyType() + " key) {");
			if (variables.isNullCheck()) {
				printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
			}
			printer.println("        java.util.Map<" + variables.getWireTypeParameters() + "> map =");
			printer.println("            internalGet" + variables.getCapitalizedName() + "().getMap();");
			printer.println("        if (!map.containsKey(key)) {");
			printer.println("          throw new java.lang.IllegalArgumentException();");
			printer.println("        }");
			printer.println("        return map.get(key);");
			printer.println("      }");
		}

		printer.println("      public Builder clear" + variables.getCapitalizedName() + "() {");
		printer.println("        " + variables.getClearHasFieldBitBuilder());
		if (useBuildMethod) {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().clear();");
		} else {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
			printer.println("            .clear();");
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
		printer.println("      public Builder remove" + variables.getCapitalizedName() + "(");
		printer.println("          " + variables.getKeyType() + " key) {");
		if (variables.isNullCheck()) {
			printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		if (useBuildMethod) {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap()");
			printer.println("            .remove(key);");
		} else {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
			printer.println("            .remove(key);");
		}
		printer.println("        return this;");
		printer.println("      }");

		printer.println("      /**");
		printer.println("       * Use alternate mutation accessors instead.");
		printer.println("       */");
		printer.println("      @java.lang.Deprecated");
		printer.println("      public java.util.Map<" + variables.getTypeParameters() + ">");
		printer.println("          getMutable" + variables.getCapitalizedName() + "() {");
		printer.println("        " + variables.getSetHasFieldBitBuilder());
		if (useBuildMethod) {
			printer.println("        return internalGetMutable" + variables.getCapitalizedName() + "().ensureMessageMap();");
		} else {
			if (isEnum) {
				printer.println("        return internalGetAdapted" + variables.getCapitalizedName() + "Map(");
				printer.println("             internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap());");
			} else {
				printer.println("        return internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap();");
			}
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
		printer.println("      public Builder put" + variables.getCapitalizedName() + "(");
		printer.println("          " + variables.getKeyType() + " key,");
		printer.println("          " + variables.getValueType() + " value) {");
		if (variables.isNullCheck()) {
			printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
		} else {
			printer.println();
		}
		if (variables.getValueNullCheck() != null) {
			printer.println("        " + variables.getValueNullCheck());
		} else {
			printer.println();
		}
		if (useBuildMethod) {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap()");
			printer.println("            .put(key, value);");
		} else {
			if (isEnum) {
				printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
				printer.println("            .put(key, " + variables.getName() + "ValueConverter.doBackward(value));");
			} else {
				printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
				printer.println("            .put(key, value);");
			}
		}
		printer.println("        " + variables.getSetHasFieldBitBuilder());
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
		printer.println("      public Builder putAll" + variables.getCapitalizedName() + "(");
		printer.println("          java.util.Map<" + variables.getTypeParameters() + "> values) {");
		if (useBuildMethod) {
			printer.println("        for (java.util.Map.Entry<" + variables.getTypeParameters() + "> e : values.entrySet()) {");
			printer.print("          if (");
			boolean needOr = false;
			if (variables.isNullCheck()) {
				printer.print("e.getKey() == null");
				needOr = true;
			}
			if (variables.getValueNullCheck() != null) {
				if (needOr) {
					printer.print(" || ");
				}
				printer.print("e.getValue() == null");
			}
			printer.println(") {");
			printer.println("            throw new NullPointerException();");
			printer.println("          }");
			printer.println("        }");
		}
		if (useBuildMethod) {
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap()");
			printer.println("            .putAll(values);");
		} else {
			if (isEnum) {
				printer.println("        internalGetAdapted" + variables.getCapitalizedName() + "Map(");
				printer.println("            internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap())");
				printer.println("                .putAll(values);");
			} else {
				printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
				printer.println("            .putAll(values);");
			}
		}
		printer.println("        " + variables.getSetHasFieldBitBuilder());
		printer.println("        return this;");
		printer.println("      }");

		if (useBuildMethod) {
			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      public " + variables.getValueBuilderType() + " put" + variables.getCapitalizedName() + "BuilderIfAbsent(");
			printer.println("          " + variables.getKeyType() + " key) {");
			printer.println("        java.util.Map<" + variables.getBoxedKeyType() + ", " + variables.getValueOrBuilderType() + "> builderMap = internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap();");
			printer.println("        " + variables.getValueOrBuilderType() + " entry = builderMap.get(key);");
			printer.println("        if (entry == null) {");
			printer.println("          entry = " + variables.getValueType() + ".newBuilder();");
			printer.println("          builderMap.put(key, entry);");
			printer.println("        }");
			printer.println("        if (entry instanceof " + variables.getValueType() + ") {");
			printer.println("          entry = ((" + variables.getValueType() + ") entry).toBuilder();");
			printer.println("          builderMap.put(key, entry);");
			printer.println("        }");
			printer.println("        return (" + variables.getValueBuilderType() + ") entry;");
			printer.println("      }");
		}

		if (isEnum) {
			printer.println("      /**");
			printer.println("       * Use alternate mutation accessors instead.");
			printer.println("       */");
			printer.println("      @java.lang.Deprecated");
			printer.println("      public java.util.Map<" + variables.getWireTypeParameters() + ">");
			printer.println("      getMutable" + variables.getCapitalizedName() + "Value() {");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        return internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap();");
			printer.println("      }");

			Helpers.writeDocComment(
					printer,
					"      ",
					commentWriter -> DocComment.writeFieldDocComment(
							commentWriter,
							descriptor,
							context,
							false));
			printer.println("      public Builder put" + variables.getCapitalizedName() + "Value(");
			printer.println("          " + variables.getKeyType() + " key,");
			printer.println("          int value) {");
			if (variables.isNullCheck()) {
				printer.println("        if (key == null) { throw new NullPointerException(\"map key\"); }");
			}
			printer.println();
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
			printer.println("            .put(key, value);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
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
			printer.println("      public Builder putAll" + variables.getCapitalizedName() + "Value(");
			printer.println("          java.util.Map<" + variables.getWireTypeParameters() + "> values) {");
			printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap()");
			printer.println("            .putAll(values);");
			printer.println("        " + variables.getSetHasFieldBitBuilder());
			printer.println("        return this;");
			printer.println("      }");
		}
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// No-op
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().clear();");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		printer.println("        internalGetMutable" + variables.getCapitalizedName() + "().mergeFrom(");
		printer.println("            other.internalGet" + variables.getCapitalizedName() + "());");
		printer.println("        " + variables.getSetHasFieldBitBuilder());
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("        if (" + variables.getGetHasFieldBitFromLocal() + ") {");
		if (Boolean.parseBoolean(variables.getUseBuildMethod())) {
			printer.println("          result." + variables.getName() + "_ = internalGet" + variables.getCapitalizedName() + "().build(" + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry);");
		} else {
			printer.println("          result." + variables.getName() + "_ = internalGet" + variables.getCapitalizedName() + "();");
			printer.println("          result." + variables.getName() + "_.makeImmutable();");
		}
		printer.println("        }");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		printer.println("                com.google.protobuf.MapEntry<" + variables.getTypeParameters() + ">");
		printer.println("                " + variables.getName() + "__ = input.readMessage(");
		printer.println("                    " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);");
		if (Boolean.parseBoolean(variables.getUseBuildMethod())) {
			printer.println("                internalGetMutable" + variables.getCapitalizedName() + "().ensureBuilderMap().put(");
		} else {
			printer.println("                internalGetMutable" + variables.getCapitalizedName() + "().getMutableMap().put(");
		}
		printer.println("                    " + variables.getName() + "__.getKey(), " + variables.getName() + "__.getValue());");
		printer.println("                " + variables.getSetHasFieldBitBuilder());
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      for (java.util.Map.Entry<" + variables.getTypeParameters() + "> entry");
		printer.println("           : internalGet" + variables.getCapitalizedName() + "().getMap().entrySet()) {");
		printer.println("        com.google.protobuf.MapEntry<" + variables.getTypeParameters() + ">");
		printer.println("        " + variables.getName() + "__ = " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry.newBuilderForType()");
		printer.println("            .setKey(entry.getKey())");
		printer.println("            .setValue(entry.getValue())");
		printer.println("            .build();");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("            .computeMessageSize(" + variables.getNumber() + ", " + variables.getName() + "__);");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      com.google.protobuf.GeneratedMessage");
		printer.println("        .serialize" + variables.getCapitalizedKeyType() + "MapTo(");
		printer.println("          output,");
		printer.println("          internalGet" + variables.getCapitalizedName() + "(),");
		printer.println("          " + variables.getCapitalizedName() + "DefaultEntryHolder.defaultEntry,");
		printer.println("          " + variables.getNumber() + ");");
	}

	@Override
	public void generateFieldBuilderInitializationCode(PrintWriter printer)
	{
		// no-op
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		printer.println("      if (!internalGet" + variables.getCapitalizedName() + "().equals(");
		printer.println("          other.internalGet" + variables.getCapitalizedName() + "())) return false;");
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		printer.println("      if (!internalGet" + variables.getCapitalizedName() + "().getMap().isEmpty()) {");
		printer.println("        hash = (37 * hash) + " + variables.getConstantName() + ";");
		printer.println("        hash = (53 * hash) + internalGet" + variables.getCapitalizedName() + "().hashCode();");
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
		return "java.util.Map"; // Placeholder
	}
}
