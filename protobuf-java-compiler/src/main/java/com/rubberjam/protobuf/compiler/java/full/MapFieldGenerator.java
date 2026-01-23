package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
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
	private final Map<String, String> variables;

	public MapFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
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
		printer.println("  int get" + variables.get("capitalized_name") + "Count();");
		printer.println("  boolean contains" + variables.get("capitalized_name") + "(");
		printer.println("      " + variables.get("key_type") + " key);");
		printer.println("  @Deprecated");
		printer.println("  java.util.Map<" + variables.get("type_parameters") + ">");
		printer.println("  get" + variables.get("capitalized_name") + "();");
		printer.println("  java.util.Map<" + variables.get("type_parameters") + ">");
		printer.println("  get" + variables.get("capitalized_name") + "Map();");
		printer.println("  " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrDefault(");
		printer.println("      " + variables.get("key_type") + " key,");
		printer.println("      " + variables.get("value_type") + " defaultValue);");
		printer.println("  " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrThrow(");
		printer.println("      " + variables.get("key_type") + " key);");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + "> "
				+ variables.get("name") + "_;");
		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + ">");
		printer.println("  internalGet" + variables.get("capitalized_name") + "() {");
		printer.println("    if (" + variables.get("name") + "_ == null) {");
		printer.println("      return com.google.protobuf.MapField.emptyMapField(");
		printer.println("          com.google.protobuf.MapEntry.newDefaultInstance(");
		printer.println("              null, null, null, null, null)); // Placeholder descriptor/types");
		printer.println("    }");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
		printer.println("    return internalGet" + variables.get("capitalized_name") + "().getMap().size();");
		printer.println("  }");

		printer.println("  public boolean contains" + variables.get("capitalized_name") + "(");
		printer.println("      " + variables.get("key_type") + " key) {");
		// Null check
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

		printer.println("  public " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrDefault(");
		printer.println("      " + variables.get("key_type") + " key,");
		printer.println("      " + variables.get("value_type") + " defaultValue) {");
		printer.println("    java.util.Map<" + variables.get("type_parameters") + "> map =");
		printer.println("        internalGet" + variables.get("capitalized_name") + "().getMap();");
		printer.println("    return map.containsKey(key) ? map.get(key) : defaultValue;");
		printer.println("  }");

		printer.println("  public " + variables.get("value_type") + " get" + variables.get("capitalized_name") + "OrThrow(");
		printer.println("      " + variables.get("key_type") + " key) {");
		printer.println("    java.util.Map<" + variables.get("type_parameters") + "> map =");
		printer.println("        internalGet" + variables.get("capitalized_name") + "().getMap();");
		printer.println("    if (!map.containsKey(key)) {");
		printer.println("      throw new java.lang.IllegalArgumentException();");
		printer.println("    }");
		printer.println("    return map.get(key);");
		printer.println("  }");
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
		printer.println("          com.google.protobuf.MapEntry.newDefaultInstance(null, null, null, null, null));");
		printer.println("    }");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  private com.google.protobuf.MapField<" + variables.get("type_parameters") + ">");
		printer.println("  internalGetMutable" + variables.get("capitalized_name") + "() {");
		printer.println("    if (" + variables.get("name") + "_ == null) {");
		printer.println("      " + variables.get("name") + "_ = com.google.protobuf.MapField.newMapField(");
		printer.println("          com.google.protobuf.MapEntry.newDefaultInstance(null, null, null, null, null));");
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
		printer.println("    internalGetMutable" + variables.get("capitalized_name") + "().clear();");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("      result." + variables.get("name") + "_ = internalGet" + variables.get("capitalized_name") + "();");
		printer.println("      result." + variables.get("name") + "_.makeImmutable();");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        size += com.google.protobuf.CodedOutputStream");
		printer.println("          .computeMessageSize(" + variables.get("number") + ", get"
				+ variables.get("capitalized_name") + "());");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        output.writeMessage(" + variables.get("number") + ", get"
				+ variables.get("capitalized_name") + "());");
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
		// Placeholder
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
