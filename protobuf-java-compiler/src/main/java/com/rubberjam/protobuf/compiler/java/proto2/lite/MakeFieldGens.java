package com.rubberjam.protobuf.compiler.java.proto2.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;
import com.rubberjam.protobuf.compiler.java.JavaType;

public final class MakeFieldGens
{
	private MakeFieldGens()
	{
	}

	public static FieldGeneratorMap<ImmutableFieldGenerator> makeImmutableFieldGenerators(
			Descriptor descriptor, JavaContext context)
	{
		FieldGeneratorMap<ImmutableFieldGenerator> result = new FieldGeneratorMap<>(descriptor);

		for (FieldDescriptor field : descriptor.getFields())
		{
			result.add(field, createFieldGenerator(field, context));
		}
		return result;
	}

	private static ImmutableFieldGenerator createFieldGenerator(FieldDescriptor field, JavaContext context)
	{
		if (field.isRepeated())
		{
			// For now, treat repeated as singular stub to avoid compilation
			// error until repeated support is added
			// Or fallback to stub
			return new ImmutableFieldGenerator()
			{
				@Override
				public void generateSerializationCode(java.io.PrintWriter printer)
				{
				}

				@Override
				public void generateMembers(java.io.PrintWriter printer)
				{
				}

				@Override
				public void generateBuilderMembers(java.io.PrintWriter printer)
				{
				}

				@Override
				public void generateInitializationCode(java.io.PrintWriter printer)
				{
				}

				@Override
				public int getFieldNumber()
				{
					return field.getNumber();
				}
			};
		}

		switch (field.getJavaType())
		{
		case MESSAGE:
			return new MessageFieldGenerator(field, context);
		case ENUM:
			return new EnumFieldGenerator(field, context);
		case STRING:
			return new StringFieldGenerator(field, context);
		default:
			return new PrimitiveFieldGenerator(field, context);
		}
	}
}
