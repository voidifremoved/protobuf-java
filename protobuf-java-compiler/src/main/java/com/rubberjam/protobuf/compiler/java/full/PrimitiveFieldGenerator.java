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

public class PrimitiveFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final Map<String, String> variables;

	public PrimitiveFieldGenerator(
			FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
	{
		this.descriptor = descriptor;
		this.messageBitIndex = messageBitIndex;
		this.builderBitIndex = builderBitIndex;
		this.context = context;
		this.variables = new HashMap<>();
		setPrimitiveVariables(
				descriptor,
				messageBitIndex,
				builderBitIndex,
				context.getFieldGeneratorInfo(descriptor),
				variables,
				context);
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
		variables.put("default", StringUtils.defaultValue(descriptor));
		variables.put("default_init",
				StringUtils.isDefaultValueJavaDefault(descriptor)
						? ""
						: "= " + StringUtils.defaultValue(descriptor));

		// Bit handling (simplified for now)
		variables.put("is_field_present_message", "true"); // TODO: Implement
															// bit fields
		variables.put("set_has_field_bit_builder", "");
		variables.put("clear_has_field_bit_builder", "");

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
		return 0; // TODO
	}

	@Override
	public int getNumBitsForBuilder()
	{
		return 0; // TODO
	}

	@Override
	public void generateInterfaceMembers(PrintWriter printer)
	{
		printer.println("  boolean has" + variables.get("capitalized_name") + "();");
		printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("  private " + variables.get("field_type") + " " + variables.get("name") + "_;");

		printer.println("  @java.lang.Override");
		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		// TODO: Implement presence check logic
		printer.println("    return false;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		printer.println("  private " + variables.get("field_type") + " " + variables.get("name") + "_;");

		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		// TODO: Builder presence check
		printer.println("    return false;");
		printer.println("  }");

		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    return " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		printer.println("    " + variables.get("name") + "_ = value;");
		// printer.println(" onChanged();"); // onChanged not yet implemented
		printer.println("    return this;");
		printer.println("  }");

		printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
		// printer.println(" onChanged();");
		printer.println("    return this;");
		printer.println("  }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("    " + variables.get("name") + "_ = " + variables.get("default") + ";");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
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
		// Placeholder
	}

	@Override
	public void generateFieldBuilderInitializationCode(PrintWriter printer)
	{
		// no-op
	}

	@Override
	public void generateEqualsCode(PrintWriter printer)
	{
		// Placeholder
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
		return variables.get("boxed_type");
	}

	public static class RepeatedPrimitiveFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final Map<String, String> variables;

		public RepeatedPrimitiveFieldGenerator(
				FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context)
		{
			this.descriptor = descriptor;
			this.messageBitIndex = messageBitIndex;
			this.builderBitIndex = builderBitIndex;
			this.context = context;
			this.variables = new HashMap<>();
			setPrimitiveVariables(
					descriptor,
					messageBitIndex,
					builderBitIndex,
					context.getFieldGeneratorInfo(descriptor),
					variables,
					context);
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
			return 0;
		} // Assuming simpler implementation without bitsets for now

		@Override
		public void generateInterfaceMembers(PrintWriter printer)
		{
			printer.println(
					"  java.util.List<" + variables.get("boxed_type") + "> get" + variables.get("capitalized_name") + "List();");
			printer.println("  int get" + variables.get("capitalized_name") + "Count();");
			printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("  private " + variables.get("field_list_type") + " " + variables.get("name") + "_;");

			printer.println("  @java.lang.Override");
			printer.println("  public java.util.List<" + variables.get("boxed_type") + "> get" + variables.get("capitalized_name")
					+ "List() {");
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
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("  private " + variables.get("field_list_type") + " " + variables.get("name") + "_ = "
					+ variables.get("empty_list") + ";");

			printer.println("  private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("    if (!" + variables.get("name") + "_.isModifiable()) {");
			printer.println("      " + variables.get("name") + "_ = makeMutableCopy(" + variables.get("name") + "_);");
			printer.println("    }");
			printer.println("  }");

			printer.println("  public java.util.List<" + variables.get("boxed_type") + "> get" + variables.get("capitalized_name")
					+ "List() {");
			printer.println("    " + variables.get("name") + "_.makeImmutable();");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    return " + variables.get("name") + "_.size();");
			printer.println("  }");

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
			printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			// Placeholder
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("      if (true) { // TODO: check if mutable");
			printer.println("        " + variables.get("name") + "_.makeImmutable();");
			printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
			printer.println("      }");
		}

		@Override
		public void generateBuilderParsingCode(PrintWriter printer)
		{
			// Placeholder
		}

		@Override
		public void generateSerializedSizeCode(PrintWriter printer)
		{
			// Placeholder
		}

		@Override
		public void generateFieldBuilderInitializationCode(PrintWriter printer)
		{
			// no-op
		}

		@Override
		public void generateEqualsCode(PrintWriter printer)
		{
			// Placeholder
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
			return variables.get("boxed_type");
		}
	}
}
