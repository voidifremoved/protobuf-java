package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MapFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final int fieldNumber;
	private final Map<String, String> variables;

	public MapFieldGenerator(
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
			Map<String, String> variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		variables.put("type", context.getNameResolver().getImmutableClassName(descriptor.getMessageType()));
		FieldDescriptor key = descriptor.getMessageType().findFieldByName("key");
		FieldDescriptor value = descriptor.getMessageType().findFieldByName("value");

		// Simplified map logic for now, generating as a MapField
		variables.put("key_type", StringUtils.getPrimitiveTypeName(StringUtils.getJavaType(key)));
		variables.put("boxed_key_type", StringUtils.boxedPrimitiveTypeName(StringUtils.getJavaType(key)));
		variables.put("value_type", StringUtils.getPrimitiveTypeName(StringUtils.getJavaType(value))); // Need
																										// to
																										// handle
																										// Objects
		variables.put("boxed_value_type", StringUtils.boxedPrimitiveTypeName(StringUtils.getJavaType(value)));

		if (StringUtils.getJavaType(value) == JavaType.MESSAGE)
		{
			variables.put("value_type", context.getNameResolver().getImmutableClassName(value.getMessageType()));
			variables.put("boxed_value_type", context.getNameResolver().getImmutableClassName(value.getMessageType()));
		}
		else if (StringUtils.getJavaType(value) == JavaType.ENUM)
		{
			variables.put("value_type", context.getNameResolver().getImmutableClassName(value.getEnumType()));
			variables.put("boxed_value_type", context.getNameResolver().getImmutableClassName(value.getEnumType()));
		}

		variables.put("type_parameters", variables.get("boxed_key_type") + ", " + variables.get("boxed_value_type"));

		FieldDescriptor keyField = descriptor.getMessageType().findFieldByName("key");
		FieldDescriptor valueField = descriptor.getMessageType().findFieldByName("value");

		variables.put("key_wire_type", "com.google.protobuf.WireFormat.FieldType." + keyField.getLiteType().name());
		variables.put("value_wire_type", "com.google.protobuf.WireFormat.FieldType." + valueField.getLiteType().name());

		variables.put("key_default_value", Helpers.defaultValue(keyField, context.getNameResolver(), context.getOptions(), true));
		variables.put("value_default_value", Helpers.defaultValue(valueField, context.getNameResolver(), context.getOptions(), true));

		if (Helpers.isReferenceType(StringUtils.getJavaType(keyField)))
		{
			variables.put("null_check", "if (key == null) { throw new NullPointerException(\"map key\"); }");
		}

		String mapEntryProtoName = descriptor.getMessageType().getFullName();
		String mapEntryDescriptorName = "internal_static_" + mapEntryProtoName.replace('.', '_') + "_descriptor";
		String outerClassName = context.getNameResolver().getImmutableClassName(descriptor.getFile());
		variables.put("descriptor_call", outerClassName + "." + mapEntryDescriptorName);

		JavaType keyJavaType = StringUtils.getJavaType(keyField);
		String capitalizedKeyType;
		switch (keyJavaType) {
			case INT: capitalizedKeyType = "Integer"; break;
			case LONG: capitalizedKeyType = "Long"; break;
			case BOOLEAN: capitalizedKeyType = "Boolean"; break;
			case STRING: capitalizedKeyType = "String"; break;
			default: capitalizedKeyType = "String"; // Fallback
		}
		variables.put("capitalized_key_type", capitalizedKeyType);
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
		printer.println("    int get" + variables.get("capitalized_name") + "Count();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    boolean contains" + variables.get("capitalized_name") + "(");
		printer.println("        " + variables.get("key_type") + " key);");
		printer.println("    /**");
		printer.println("     * Use {@link #get" + variables.get("capitalized_name") + "Map()} instead.");
		printer.println("     */");
		printer.println("    @java.lang.Deprecated");
		printer.println("    java.util.Map<" + variables.get("type_parameters") + ">");
		printer.println("    get" + variables.get("capitalized_name") + "();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    java.util.Map<" + variables.get("type_parameters") + ">");
		printer.println("    get" + variables.get("capitalized_name") + "Map();");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrDefault(");
		printer.println("        " + variables.get("key_type") + " key,");
		printer.println("        " + variables.get("value_type") + " defaultValue);");
		Helpers.writeDocComment(
				printer,
				"    ",
				commentWriter -> DocComment.writeFieldDocComment(
						commentWriter,
						descriptor,
						context,
						false));
		printer.println("    " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrThrow(");
		printer.println("        " + variables.get("key_type") + " key);");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("    private static final class " + variables.get("capitalized_name") + "DefaultEntryHolder {");
		printer.println("      static final com.google.protobuf.MapEntry<");
		printer.println("          " + variables.get("type_parameters") + "> defaultEntry =");
		printer.println("              com.google.protobuf.MapEntry");
		printer.println("              .<" + variables.get("type_parameters") + ">newDefaultInstance(");
		printer.println("                  " + variables.get("descriptor_call") + ", ");
		printer.println("                  " + variables.get("key_wire_type") + ",");
		printer.println("                  " + variables.get("key_default_value") + ",");
		printer.println("                  " + variables.get("value_wire_type") + ",");
		printer.println("                  " + variables.get("value_default_value") + ");");
		printer.println("    }");
		printer.println("    @SuppressWarnings(\"serial\")");
		printer.println("    private com.google.protobuf.MapField<");
		printer.println("        " + variables.get("type_parameters") + "> " + variables.get("name") + "_;");
		printer.println("    private com.google.protobuf.MapField<" + variables.get("type_parameters") + ">");
		printer.println("    internalGet" + variables.get("capitalized_name") + "() {");
		printer.println("      if (" + variables.get("name") + "_ == null) {");
		printer.println("        return com.google.protobuf.MapField.emptyMapField(");
		printer.println("            " + variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry);");
		printer.println("      }");
		printer.println("      return " + variables.get("name") + "_;");
		printer.println("    }");

		printer.println("    public int get" + variables.get("capitalized_name") + "Count() {");
		printer.println("      return internalGet" + variables.get("capitalized_name") + "().getMap().size();");
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
		printer.println("    public boolean contains" + variables.get("capitalized_name") + "(");
		printer.println("        " + variables.get("key_type") + " key) {");
		if (variables.containsKey("null_check")) { // Reference types need null check?
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		}
		printer.println("      return internalGet" + variables.get("capitalized_name") + "().getMap().containsKey(key);");
		printer.println("    }");

		printer.println("    /**");
		printer.println("     * Use {@link #get" + variables.get("capitalized_name") + "Map()} instead.");
		printer.println("     */");
		printer.println("    @java.lang.Override");
		printer.println("    @java.lang.Deprecated");
		printer.println("    public java.util.Map<" + variables.get("type_parameters") + "> get" + variables.get("capitalized_name")
				+ "() {");
		printer.println("      return get" + variables.get("capitalized_name") + "Map();");
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
		printer.println("    public java.util.Map<" + variables.get("type_parameters") + "> get" + variables.get("capitalized_name")
				+ "Map() {");
		printer.println("      return internalGet" + variables.get("capitalized_name") + "().getMap();");
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
		printer.println("    public " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrDefault(");
		printer.println("        " + variables.get("key_type") + " key,");
		printer.println("        " + variables.get("value_type") + " defaultValue) {");
		if (variables.containsKey("null_check")) {
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		}
		printer.println("      java.util.Map<" + variables.get("type_parameters") + "> map =");
		printer.println("          internalGet" + variables.get("capitalized_name") + "().getMap();");
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
		printer.println("    public " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrThrow(");
		printer.println("        " + variables.get("key_type") + " key) {");
		if (variables.containsKey("null_check")) {
			printer.println("      if (key == null) { throw new NullPointerException(\"map key\"); }");
		}
		printer.println("      java.util.Map<" + variables.get("type_parameters") + "> map =");
		printer.println("          internalGet" + variables.get("capitalized_name") + "().getMap();");
		printer.println("      if (!map.containsKey(key)) {");
		printer.println("        throw new java.lang.IllegalArgumentException();");
		printer.println("      }");
		printer.println("      return map.get(key);");
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + "> "
				+ variables.get("name") + "_;");

		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + ">");
		printer.println("  internalGet" + variables.get("capitalized_name") + "() {");
		printer.println("    if (" + variables.get("name") + "_ == null) {");
		printer.println("      return com.google.protobuf.MapField.emptyMapField(");
		printer.println("          " + variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry);");
		printer.println("    }");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + ">");
		printer.println("  internalGetMutable" + variables.get("capitalized_name") + "() {");
		printer.println("    if (" + variables.get("name") + "_ == null) {");
		printer.println("      " + variables.get("name") + "_ = com.google.protobuf.MapField.newMapField(");
		printer.println("          " + variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry);");
		printer.println("    }");
		printer.println("    if (!" + variables.get("name") + "_.isMutable()) {");
		printer.println("      " + variables.get("name") + "_ = " + variables.get("name") + "_.copy();");
		printer.println("    }");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
		printer.println("    return internalGet" + variables.get("capitalized_name") + "().getMap().size();");
		printer.println("  }");

		printer.println("  public boolean contains" + variables.get("capitalized_name") + "(");
		printer.println("      " + variables.get("key_type") + " key) {");
		printer.println("    return internalGet" + variables.get("capitalized_name") + "().getMap().containsKey(key);");
		printer.println("  }");

		printer.println("  @Deprecated");
		printer.println("  public java.util.Map<" + variables.get("type_parameters") + "> get" + variables.get("capitalized_name")
				+ "() {");
		printer.println("    return get" + variables.get("capitalized_name") + "Map();");
		printer.println("  }");

		printer.println("  public java.util.Map<" + variables.get("type_parameters") + "> get" + variables.get("capitalized_name")
				+ "Map() {");
		printer.println("    return internalGet" + variables.get("capitalized_name") + "().getMap();");
		printer.println("  }");

		printer.println("  public java.util.Map<" + variables.get("type_parameters") + "> getMutable"
				+ variables.get("capitalized_name") + "() {");
		printer.println("    return internalGetMutable" + variables.get("capitalized_name") + "().getMutableMap();");
		printer.println("  }");

		printer.println("  public Builder put" + variables.get("capitalized_name") + "(");
		printer.println("      " + variables.get("key_type") + " key,");
		printer.println("      " + variables.get("value_type") + " value) {");
		printer.println("    internalGetMutable" + variables.get("capitalized_name") + "().getMutableMap()");
		printer.println("        .put(key, value);");
		printer.println("    return this;");
		printer.println("  }");

		printer.println("  public Builder putAll" + variables.get("capitalized_name") + "(");
		printer.println("      java.util.Map<" + variables.get("type_parameters") + "> values) {");
		printer.println("    internalGetMutable" + variables.get("capitalized_name") + "().getMutableMap()");
		printer.println("        .putAll(values);");
		printer.println("    return this;");
		printer.println("  }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// No-op
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("        internalGetMutable" + variables.get("capitalized_name") + "().clear();");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("        result." + variables.get("name") + "_ = internalGet" + variables.get("capitalized_name") + "();");
		printer.println("        result." + variables.get("name") + "_.makeImmutable();");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      for (java.util.Map.Entry<" + variables.get("type_parameters") + "> entry");
		printer.println("           : internalGet" + variables.get("capitalized_name") + "().getMap().entrySet()) {");
		printer.println("        com.google.protobuf.MapEntry<" + variables.get("type_parameters") + ">");
		printer.println("        " + variables.get("name") + "__ = " + variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry.newBuilderForType()");
		printer.println("            .setKey(entry.getKey())");
		printer.println("            .setValue(entry.getValue())");
		printer.println("            .build();");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("            .computeMessageSize(" + variables.get("number") + ", " + variables.get("name") + "__);");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      com.google.protobuf.GeneratedMessage");
		printer.println("        .serialize" + variables.get("capitalized_key_type") + "MapTo(");
		printer.println("          output,");
		printer.println("          internalGet" + variables.get("capitalized_name") + "(),");
		printer.println("          " + variables.get("capitalized_name") + "DefaultEntryHolder.defaultEntry,");
		printer.println("          " + variables.get("number") + ");");
	}

	@Override
	public void generateFieldBuilderInitializationCode(PrintWriter printer)
	{
		// no-op
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		printer.println("      if (!internalGet" + variables.get("capitalized_name") + "().equals(");
		printer.println("          other.internalGet" + variables.get("capitalized_name") + "())) return false;");
	}

	@Override
	public void generateHashCode(PrintWriter printer)
	{
		printer.println("      if (!internalGet" + variables.get("capitalized_name") + "().getMap().isEmpty()) {");
		printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("        hash = (53 * hash) + internalGet" + variables.get("capitalized_name") + "().hashCode();");
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
