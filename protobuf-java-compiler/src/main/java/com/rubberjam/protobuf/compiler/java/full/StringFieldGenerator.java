package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.InternalHelpers;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldAccessorType;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class StringFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final int fieldNumber;
	private final Map<String, String> variables;

	public StringFieldGenerator(
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
			FieldGeneratorInfo info,
			Map<String, String> variables,
			Context context)
	{
		FieldCommon.setCommonFieldVariables(descriptor, info, variables);
		String defaultValue = Helpers.defaultValue(descriptor, context.getNameResolver(), context.getOptions(), true);
		variables.put("default", defaultValue);
		variables.put("default_init", "= " + defaultValue);
		variables.put("boxed_type", "java.lang.String");
		variables.put("tag", Integer.toString(Helpers.getTag(descriptor)));
		variables.put("tag_size", Integer.toString(Helpers.getTagSize(descriptor)));
		variables.put("null_check", "if (value == null) { throw new NullPointerException(); }");
		variables.put("isStringEmpty", "com.google.protobuf.GeneratedMessage.isStringEmpty");
		variables.put("writeString", "com.google.protobuf.GeneratedMessage.writeString");
		variables.put("computeStringSize", "com.google.protobuf.GeneratedMessage.computeStringSize");
		variables.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "");
		variables.put("on_changed", "onChanged();");

		if (InternalHelpers.hasHasbit(descriptor))
		{
			variables.put("set_has_field_bit_to_local", Helpers.generateSetBitToLocal(messageBitIndex));
			variables.put("set_has_field_bit_message", Helpers.generateSetBit(messageBitIndex) + ";");
			variables.put("is_field_present_message", Helpers.generateGetBit(messageBitIndex));
		}
		else
		{
			variables.put("set_has_field_bit_to_local", "");
			variables.put("set_has_field_bit_message", "");
			variables.put("is_field_present_message",
					"!" + variables.get("isStringEmpty") + "(" + variables.get("name") + "_)");
		}

		variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
		variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
		variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
		variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
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
			printer.println("    " + variables.get("deprecation") + "boolean has"
					+ variables.get("capitalized_name") + "();");
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
		printer.println("    " + variables.get("deprecation") + "java.lang.String get"
				+ variables.get("capitalized_name") + "();");
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
		printer.println("    " + variables.get("deprecation") + "com.google.protobuf.ByteString");
		printer.println("        get" + variables.get("capitalized_name") + "Bytes();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("    @SuppressWarnings(\"serial\")");
		printer.println(
				"    private volatile java.lang.Object " + variables.get("name") + "_ = " + variables.get("default") + ";");
		FieldCommon.printExtraFieldInfo(variables, printer);

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
		printer.println("    " + variables.get("deprecation") + "public java.lang.String get"
				+ variables.get("capitalized_name") + "() {");
		printer.println("      java.lang.Object ref = " + variables.get("name") + "_;");
		printer.println("      if (ref instanceof java.lang.String) {");
		printer.println("        return (java.lang.String) ref;");
		printer.println("      } else {");
		printer.println("        com.google.protobuf.ByteString bs = ");
		printer.println("            (com.google.protobuf.ByteString) ref;");
		printer.println("        java.lang.String s = bs.toStringUtf8();");
		printer.println("        if (bs.isValidUtf8()) {");
		printer.println("          " + variables.get("name") + "_ = s;");
		printer.println("        }");
		printer.println("        return s;");
		printer.println("      }");
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
		printer.println("    @java.lang.Override");
		printer.println("    " + variables.get("deprecation") + "public com.google.protobuf.ByteString");
		printer.println("        get" + variables.get("capitalized_name") + "Bytes() {");
		printer.println("      java.lang.Object ref = " + variables.get("name") + "_;");
		printer.println("      if (ref instanceof java.lang.String) {");
		printer.println("        com.google.protobuf.ByteString b = ");
		printer.println("            com.google.protobuf.ByteString.copyFromUtf8(");
		printer.println("                (java.lang.String) ref);");
		printer.println("        " + variables.get("name") + "_ = b;");
		printer.println("        return b;");
		printer.println("      } else {");
		printer.println("        return (com.google.protobuf.ByteString) ref;");
		printer.println("      }");
		printer.println("    }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		printer.println("      private java.lang.Object " + variables.get("name") + "_ = " + variables.get("default") + ";");
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
		printer.println("      " + variables.get("deprecation") + "public java.lang.String get"
				+ variables.get("capitalized_name") + "() {");
		printer.println("        java.lang.Object ref = " + variables.get("name") + "_;");
		printer.println("        if (!(ref instanceof java.lang.String)) {");
		printer.println("          com.google.protobuf.ByteString bs =");
		printer.println("              (com.google.protobuf.ByteString) ref;");
		printer.println("          java.lang.String s = bs.toStringUtf8();");
		printer.println("          if (bs.isValidUtf8()) {");
		printer.println("            " + variables.get("name") + "_ = s;");
		printer.println("          }");
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
		printer.println("      " + variables.get("deprecation") + "public com.google.protobuf.ByteString");
		printer.println("          get" + variables.get("capitalized_name") + "Bytes() {");
		printer.println("        java.lang.Object ref = " + variables.get("name") + "_;");
		printer.println("        if (ref instanceof String) {");
		printer.println("          com.google.protobuf.ByteString b = ");
		printer.println("              com.google.protobuf.ByteString.copyFromUtf8(");
		printer.println("                  (java.lang.String) ref);");
		printer.println("          " + variables.get("name") + "_ = b;");
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
		printer.println("      " + variables.get("deprecation") + "public Builder set"
				+ variables.get("capitalized_name") + "(");
		printer.println("          " + variables.get("boxed_type") + " value) {");
		printer.println("        " + variables.get("null_check"));
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
		printer.println("        " + variables.get("name") + "_ = getDefaultInstance().get"
				+ variables.get("capitalized_name") + "();");
		printer.println("        " + variables.get("clear_has_field_bit_builder"));
		printer.println("        " + variables.get("on_changed"));
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
		printer.println("      " + variables.get("deprecation") + "public Builder set"
				+ variables.get("capitalized_name") + "Bytes(");
		printer.println("          com.google.protobuf.ByteString value) {");
		printer.println("        " + variables.get("null_check"));
		printer.println("        " + variables.get("name") + "_ = value;");
		printer.println("        " + variables.get("set_has_field_bit_builder"));
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
		printer.println("        " + variables.get("name") + "_ = " + variables.get("default") + ";");
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("        " + variables.get("name") + "_ = " + variables.get("default") + ";");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		if (descriptor.hasPresence())
		{
			printer.println("        if (other.has" + variables.get("capitalized_name") + "()) {");
			printer.println("          " + variables.get("name") + "_ = other." + variables.get("name") + "_;");
			printer.println("          " + variables.get("set_has_field_bit_builder"));
			printer.println("          " + variables.get("on_changed"));
			printer.println("        }");
		}
		else
		{
			printer.println("        if (!other.get" + variables.get("capitalized_name") + "().isEmpty()) {");
			printer.println("          " + variables.get("name") + "_ = other." + variables.get("name") + "_;");
			printer.println("          " + variables.get("set_has_field_bit_builder"));
			printer.println("          " + variables.get("on_changed"));
			printer.println("        }");
		}
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("        if (" + variables.get("get_has_field_bit_from_local") + ") {");
		printer.println("          result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
		if (getNumBitsForMessage() > 0)
		{
			printer.println("          " + variables.get("set_has_field_bit_to_local") + ";");
		}
		printer.println("        }");
	}

	@Override
	public void generateBuilderParsingCode(PrintWriter printer)
	{
		printer.println("                " + variables.get("name") + "_ = input.readBytes();");
		printer.println("                " + variables.get("set_has_field_bit_builder"));
	}

	@Override
	public void generateSerializedSizeCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        size += com.google.protobuf.GeneratedMessage.computeStringSize("
				+ variables.get("number") + ", " + variables.get("name") + "_);");
		printer.println("      }");
	}

	@Override
	public void generateWriteToCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        com.google.protobuf.GeneratedMessage.writeString(output, "
				+ variables.get("number") + ", " + variables.get("name") + "_);");
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
		if (descriptor.hasPresence())
		{
			printer.println("      if (has" + variables.get("capitalized_name") + "()) {");
		}
		printer.println("        hash = (37 * hash) + " + variables.get("constant_name") + ";");
		printer.println("        hash = (53 * hash) + get" + variables.get("capitalized_name") + "().hashCode();");
		if (descriptor.hasPresence())
		{
			printer.println("      }");
		}
	}

	@Override
	public void generateSerializationCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("is_field_present_message") + ") {");
		printer.println("        com.google.protobuf.GeneratedMessage.writeString(output, "
				+ variables.get("number") + ", " + variables.get("name") + "_);");
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
		private final Context context;
		private final int fieldNumber;
		private final Map<String, String> variables;

		public RepeatedStringFieldGenerator(
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
				FieldGeneratorInfo info,
				Map<String, String> variables,
				Context context)
		{
			FieldCommon.setCommonFieldVariables(descriptor, info, variables);
			variables.put("empty_list", "com.google.protobuf.LazyStringArrayList.emptyList()");
			variables.put("name_make_immutable", variables.get("name") + "_.makeImmutable()");
			variables.put("get_has_field_bit_builder", Helpers.generateGetBit(builderBitIndex));
			variables.put("get_has_field_bit_from_local", Helpers.generateGetBitFromLocal(builderBitIndex));
			variables.put("set_has_field_bit_builder", Helpers.generateSetBit(builderBitIndex) + ";");
			variables.put("clear_has_field_bit_builder", Helpers.generateClearBit(builderBitIndex) + ";");
			variables.put("on_changed", "onChanged();");
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
					"  ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_GETTER,
							context,
							false,
							false,
							false));
			printer.println("  java.util.List<java.lang.String>");
			printer.println("      get" + variables.get("capitalized_name") + "List();");
			Helpers.writeDocComment(
					printer,
					"  ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_COUNT,
							context,
							false,
							false,
							false));
			printer.println("  int get" + variables.get("capitalized_name") + "Count();");
			Helpers.writeDocComment(
					printer,
					"  ",
					commentWriter -> DocComment.writeFieldAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("  java.lang.String get" + variables.get("capitalized_name") + "(int index);");
			Helpers.writeDocComment(
					printer,
					"  ",
					commentWriter -> DocComment.writeFieldStringBytesAccessorDocComment(
							commentWriter,
							descriptor,
							FieldAccessorType.LIST_INDEXED_GETTER,
							context,
							false,
							false,
							false));
			printer.println("  com.google.protobuf.ByteString");
			printer.println("      get" + variables.get("capitalized_name") + "Bytes(int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("  private com.google.protobuf.LazyStringArrayList " + variables.get("name") + "_ = "
					+ variables.get("empty_list") + ";");

			printer.println("  @java.lang.Override");
			printer.println(
					"  public com.google.protobuf.ProtocolStringList get" + variables.get("capitalized_name") + "List() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    return " + variables.get("name") + "_.size();");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("    return " + variables.get("name") + "_.get(index);");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println(
					"  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes(int index) {");
			printer.println("    return " + variables.get("name") + "_.getByteString(index);");
			printer.println("  }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("  private com.google.protobuf.LazyStringArrayList " + variables.get("name") + "_ = "
					+ variables.get("empty_list") + ";");

			printer.println("  private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("    if (!" + variables.get("get_has_field_bit_builder") + ") {");
			printer.println("      " + variables.get("name") + "_ = new com.google.protobuf.LazyStringArrayList("
					+ variables.get("name") + "_);");
			printer.println("      " + variables.get("set_has_field_bit_builder"));
			printer.println("    }");
			printer.println("  }");

			printer.println(
					"  public com.google.protobuf.ProtocolStringList get" + variables.get("capitalized_name") + "List() {");
			printer.println("    " + variables.get("name") + "_.makeImmutable();");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    return " + variables.get("name") + "_.size();");
			printer.println("  }");

			printer.println("  public java.lang.String get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("    return " + variables.get("name") + "_.get(index);");
			printer.println("  }");

			printer.println(
					"  public com.google.protobuf.ByteString get" + variables.get("capitalized_name") + "Bytes(int index) {");
			printer.println("    return " + variables.get("name") + "_.getByteString(index);");
			printer.println("  }");

			printer.println("  public Builder set" + variables.get("capitalized_name") + "(");
			printer.println("      int index, java.lang.String value) {");
			printer.println("    if (value == null) { throw new NullPointerException(); }");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("    " + variables.get("name") + "_.set(index, value);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder add" + variables.get("capitalized_name") + "(");
			printer.println("      java.lang.String value) {");
			printer.println("    if (value == null) { throw new NullPointerException(); }");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("    " + variables.get("name") + "_.add(value);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder addAll" + variables.get("capitalized_name") + "(");
			printer.println("      java.lang.Iterable<java.lang.String> values) {");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println(
					"    com.google.protobuf.AbstractMessageLite.Builder.addAll(values, " + variables.get("name") + "_);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
			printer.println("    " + variables.get("name") + "_ = " + variables.get("empty_list") + ";");
			printer.println("    " + variables.get("clear_has_field_bit_builder"));
			printer.println("    " + variables.get("on_changed"));
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder add" + variables.get("capitalized_name") + "Bytes(");
			printer.println("      com.google.protobuf.ByteString value) {");
			printer.println("    if (value == null) { throw new NullPointerException(); }");
			printer.println("    // checkByteStringIsUtf8(value);");
			printer.println("    ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("    " + variables.get("name") + "_.add(value);");
			printer.println("    // onChanged();");
			printer.println("    return this;");
			printer.println("  }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("        " + variables.get("name") + "_ =");
			printer.println("            " + variables.get("empty_list") + ";");
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
			printer.println("      size += com.google.protobuf.GeneratedMessage.computeStringSize("
					+ variables.get("number") + ", " + variables.get("name") + "_);");
		}

		@Override
		public void generateWriteToCode(PrintWriter printer)
		{
			printer.println("      com.google.protobuf.GeneratedMessage.writeString(output, "
					+ variables.get("number") + ", " + variables.get("name") + "_);");
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
			return "java.lang.String";
		}
	}
}
