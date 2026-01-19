package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorInfo;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MessageFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final int messageBitIndex;
	private final int builderBitIndex;
	private final Context context;
	private final Map<String, String> variables;

	public MessageFieldGenerator(
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

		// Bit logic
		variables.put("is_field_present_message", "true"); // Placeholder
		variables.put("set_has_field_bit_builder", "");
		variables.put("clear_has_field_bit_builder", "");
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
		return 0;
	}

	@Override
	public void generateInterfaceMembers(PrintWriter printer)
	{
		printer.println("  boolean has" + variables.get("capitalized_name") + "();");
		printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "();");
		printer.println("  " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name") + "OrBuilder();");
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		printer.println("  private " + variables.get("type") + " " + variables.get("name") + "_;");

		printer.println("  @java.lang.Override");
		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		printer.println("    return " + variables.get("name") + "_ != null;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    return " + variables.get("name") + "_ == null ? " + variables.get("type")
				+ ".getDefaultInstance() : " + variables.get("name") + "_;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println(
				"  public " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name") + "OrBuilder() {");
		printer.println("    return " + variables.get("name") + "_ == null ? " + variables.get("type")
				+ ".getDefaultInstance() : " + variables.get("name") + "_;");
		printer.println("  }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		printer.println("  private " + variables.get("type") + " " + variables.get("name") + "_;");

		// Nested builder logic (SingleFieldBuilder) is complex, creating
		// simplified version first
		printer.println("  private com.google.protobuf.SingleFieldBuilder<" + variables.get("type") + ", " + variables.get("type")
				+ ".Builder, " + variables.get("type") + "OrBuilder> " + variables.get("name") + "Builder_;");

		printer.println("  public boolean has" + variables.get("capitalized_name") + "() {");
		printer.println("    return " + variables.get("name") + "Builder_ != null || " + variables.get("name") + "_ != null;");
		printer.println("  }");

		printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "() {");
		printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
		printer.println("      return " + variables.get("name") + "_ == null ? " + variables.get("type")
				+ ".getDefaultInstance() : " + variables.get("name") + "_;");
		printer.println("    } else {");
		printer.println("      return " + variables.get("name") + "Builder_.getMessage();");
		printer.println("    }");
		printer.println("  }");

		printer.println("  public Builder set" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		printer.println("    if (value == null) { throw new NullPointerException(); }");
		printer.println("    " + variables.get("name") + "_ = value;");
		printer.println("    " + variables.get("name") + "Builder_ = null;");
		printer.println("    return this;");
		printer.println("  }");

		// merge, clear, builder getters...
		printer.println("  public Builder merge" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
		printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
		printer.println("      if (" + variables.get("name") + "_ != null) {");
		printer.println("        " + variables.get("name") + "_ = " + variables.get("type") + ".newBuilder("
				+ variables.get("name") + "_).mergeFrom(value).buildPartial();");
		printer.println("      } else {");
		printer.println("        " + variables.get("name") + "_ = value;");
		printer.println("      }");
		printer.println("    } else {");
		printer.println("      " + variables.get("name") + "Builder_.mergeFrom(value);");
		printer.println("    }");
		printer.println("    return this;");
		printer.println("  }");

		printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
		printer.println("    " + variables.get("name") + "_ = null;");
		printer.println("    " + variables.get("name") + "Builder_ = null;");
		printer.println("    return this;");
		printer.println("  }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// No-op for null initialization
	}

	@Override
	public void generateBuilderClearCode(PrintWriter printer)
	{
		printer.println("    " + variables.get("name") + "_ = null;");
		printer.println("    " + variables.get("name") + "Builder_ = null;");
	}

	@Override
	public void generateMergingCode(PrintWriter printer)
	{
		// Placeholder
	}

	@Override
	public void generateBuildingCode(PrintWriter printer)
	{
		printer.println("      if (" + variables.get("name") + "Builder_ == null) {");
		printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
		printer.println("      } else {");
		printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "Builder_.build();");
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
		return variables.get("type");
	}

	public static class RepeatedMessageFieldGenerator extends ImmutableFieldGenerator
	{
		private final FieldDescriptor descriptor;
		private final int messageBitIndex;
		private final int builderBitIndex;
		private final Context context;
		private final Map<String, String> variables;

		public RepeatedMessageFieldGenerator(
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
		}

		@Override
		public void generateInterfaceMembers(PrintWriter printer)
		{
			printer.println(
					"  java.util.List<" + variables.get("type") + "> get" + variables.get("capitalized_name") + "List();");
			printer.println("  " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index);");
			printer.println("  int get" + variables.get("capitalized_name") + "Count();");
			printer.println("  java.util.List<? extends " + variables.get("type") + "OrBuilder> get"
					+ variables.get("capitalized_name") + "OrBuilderList();");
			printer.println(
					"  " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name") + "OrBuilder(int index);");
		}

		@Override
		public void generateMembers(PrintWriter printer)
		{
			printer.println("  private java.util.List<" + variables.get("type") + "> " + variables.get("name") + "_;");

			printer.println("  @java.lang.Override");
			printer.println("  public java.util.List<" + variables.get("type") + "> get" + variables.get("capitalized_name")
					+ "List() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public java.util.List<? extends " + variables.get("type") + "OrBuilder> get"
					+ variables.get("capitalized_name") + "OrBuilderList() {");
			printer.println("    return " + variables.get("name") + "_;");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    return " + variables.get("name") + "_.size();");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("    return " + variables.get("name") + "_.get(index);");
			printer.println("  }");

			printer.println("  @java.lang.Override");
			printer.println("  public " + variables.get("type") + "OrBuilder get" + variables.get("capitalized_name")
					+ "OrBuilder(int index) {");
			printer.println("    return " + variables.get("name") + "_.get(index);");
			printer.println("  }");
		}

		@Override
		public void generateBuilderMembers(PrintWriter printer)
		{
			printer.println("  private java.util.List<" + variables.get("type") + "> " + variables.get("name")
					+ "_ = java.util.Collections.emptyList();");
			printer.println(
					"  private com.google.protobuf.RepeatedFieldBuilder<" + variables.get("type") + ", " + variables.get("type")
							+ ".Builder, " + variables.get("type") + "OrBuilder> " + variables.get("name") + "Builder_;");

			printer.println("  private void ensure" + variables.get("capitalized_name") + "IsMutable() {");
			printer.println("    if (!(" + variables.get("name") + "_.getClass() == java.util.ArrayList.class)) {"); // Simplified
																														// check
			printer.println("      " + variables.get("name") + "_ = new java.util.ArrayList<" + variables.get("type") + ">("
					+ variables.get("name") + "_);");
			printer.println("    }");
			printer.println("  }");

			printer.println("  public java.util.List<" + variables.get("type") + "> get" + variables.get("capitalized_name")
					+ "List() {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      return java.util.Collections.unmodifiableList(" + variables.get("name") + "_);");
			printer.println("    } else {");
			printer.println("      return " + variables.get("name") + "Builder_.getMessageList();");
			printer.println("    }");
			printer.println("  }");

			printer.println("  public int get" + variables.get("capitalized_name") + "Count() {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      return " + variables.get("name") + "_.size();");
			printer.println("    } else {");
			printer.println("      return " + variables.get("name") + "Builder_.getCount();");
			printer.println("    }");
			printer.println("  }");

			printer.println("  public " + variables.get("type") + " get" + variables.get("capitalized_name") + "(int index) {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      return " + variables.get("name") + "_.get(index);");
			printer.println("    } else {");
			printer.println("      return " + variables.get("name") + "Builder_.getMessage(index);");
			printer.println("    }");
			printer.println("  }");

			printer.println("  public Builder set" + variables.get("capitalized_name") + "(int index, " + variables.get("type")
					+ " value) {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      if (value == null) { throw new NullPointerException(); }");
			printer.println("      ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("      " + variables.get("name") + "_.set(index, value);");
			printer.println("    } else {");
			printer.println("      " + variables.get("name") + "Builder_.setMessage(index, value);");
			printer.println("    }");
			printer.println("    return this;");
			printer.println("  }");

			printer.println(
					"  public Builder add" + variables.get("capitalized_name") + "(" + variables.get("type") + " value) {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      if (value == null) { throw new NullPointerException(); }");
			printer.println("      ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println("      " + variables.get("name") + "_.add(value);");
			printer.println("    } else {");
			printer.println("      " + variables.get("name") + "Builder_.addMessage(value);");
			printer.println("    }");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder addAll" + variables.get("capitalized_name") + "(java.lang.Iterable<? extends "
					+ variables.get("type") + "> values) {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      ensure" + variables.get("capitalized_name") + "IsMutable();");
			printer.println(
					"      com.google.protobuf.AbstractMessageLite.Builder.addAll(values, " + variables.get("name") + "_);");
			printer.println("    } else {");
			printer.println("      " + variables.get("name") + "Builder_.addAllMessages(values);");
			printer.println("    }");
			printer.println("    return this;");
			printer.println("  }");

			printer.println("  public Builder clear" + variables.get("capitalized_name") + "() {");
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      " + variables.get("name") + "_ = java.util.Collections.emptyList();");
			printer.println("    } else {");
			printer.println("      " + variables.get("name") + "Builder_.clear();");
			printer.println("    }");
			printer.println("    return this;");
			printer.println("  }");
		}

		@Override
		public void generateInitializationCode(PrintWriter printer)
		{
			printer.println("    " + variables.get("name") + "_ = java.util.Collections.emptyList();");
		}

		@Override
		public void generateBuilderClearCode(PrintWriter printer)
		{
			printer.println("    if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("      " + variables.get("name") + "_ = java.util.Collections.emptyList();");
			printer.println("    } else {");
			printer.println("      " + variables.get("name") + "Builder_.clear();");
			printer.println("    }");
		}

		@Override
		public void generateMergingCode(PrintWriter printer)
		{
			// Placeholder
		}

		@Override
		public void generateBuildingCode(PrintWriter printer)
		{
			printer.println("      if (" + variables.get("name") + "Builder_ == null) {");
			printer.println("        if ((" + variables.get("name") + "_.getClass() == java.util.ArrayList.class)) {");
			printer.println("          " + variables.get("name") + "_ = java.util.Collections.unmodifiableList("
					+ variables.get("name") + "_);");
			printer.println("        }");
			printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "_;");
			printer.println("      } else {");
			printer.println("        result." + variables.get("name") + "_ = " + variables.get("name") + "Builder_.build();");
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
			return variables.get("type");
		}
	}
}
