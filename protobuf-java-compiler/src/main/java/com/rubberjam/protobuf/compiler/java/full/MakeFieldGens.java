package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

public final class MakeFieldGens
{

	public static FieldGeneratorMap<ImmutableFieldGenerator> makeImmutableFieldGenerators(
			Descriptor descriptor, Context context)
	{
		FieldGeneratorMap<ImmutableFieldGenerator> ret = new FieldGeneratorMap<>(descriptor);
		int messageBitIndex = 0;
		int builderBitIndex = 0;

		for (FieldDescriptor field : descriptor.getFields())
		{
			ImmutableFieldGenerator generator = makeImmutableGenerator(field, messageBitIndex, builderBitIndex, context);
			messageBitIndex += generator.getNumBitsForMessage();
			builderBitIndex += generator.getNumBitsForBuilder();
			ret.add(field, generator);
		}
		return ret;
	}

	private static ImmutableFieldGenerator makeImmutableGenerator(
			FieldDescriptor field, int messageBitIndex, int builderBitIndex, Context context)
	{
		if (field.isMapField())
		{
			return new MapFieldGenerator(field, messageBitIndex, builderBitIndex, context);
		}
		if (field.isRepeated())
		{
			JavaType javaType = StringUtils.getJavaType(field);
			switch (javaType)
			{
			case MESSAGE:
				return new MessageFieldGenerator.RepeatedMessageFieldGenerator(field, messageBitIndex, builderBitIndex, context);
			case ENUM:
				// return new RepeatedEnumFieldGenerator(field, messageBitIndex,
				// builderBitIndex, context);
				// TODO: repeated enum
				return new PrimitiveFieldGenerator.RepeatedPrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex,
						context); // Fallback
			case STRING:
				return new StringFieldGenerator.RepeatedStringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
			default:
				return new PrimitiveFieldGenerator.RepeatedPrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex,
						context);
			}
		}
		else
		{
			if (field.getContainingOneof() != null)
			{
				// TODO: Oneof support
				JavaType javaType = StringUtils.getJavaType(field);
				if (javaType == JavaType.STRING)
				{
					return new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should
																										// be
																										// StringOneof
				}
				if (javaType == JavaType.ENUM)
				{
					return new EnumFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should
																										// be
																										// EnumOneof
				}
				if (javaType == JavaType.MESSAGE)
				{
					return new MessageFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should
																										// be
																										// MessageOneof
				}
				return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should
																										// be
																										// PrimitiveOneof
			}
			else
			{
				JavaType javaType = StringUtils.getJavaType(field);
				switch (javaType)
				{
				case MESSAGE:
					return new MessageFieldGenerator(field, messageBitIndex, builderBitIndex, context);
				case ENUM:
					return new EnumFieldGenerator(field, messageBitIndex, builderBitIndex, context);
				case STRING:
					return new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
				default:
					return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context);
				}
			}
		}
	}
}
