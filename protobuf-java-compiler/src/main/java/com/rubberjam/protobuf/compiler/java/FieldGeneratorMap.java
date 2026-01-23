package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldGeneratorMap<T extends FieldGenerator>
{
	private final Descriptor descriptor;
	private final List<T> fieldGenerators;
	private final List<T> sortedFieldGenerators;
	
	public FieldGeneratorMap(Descriptor descriptor)
	{
		this.descriptor = descriptor;
		this.fieldGenerators = new ArrayList<>(descriptor.getFields().size());
		this.sortedFieldGenerators = new ArrayList<>(descriptor.getFields().size());
	}

	public void add(FieldDescriptor field, T fieldGenerator)
	{
		if (field.getContainingType() != descriptor)
		{
			throw new IllegalArgumentException("Field not belonging to the descriptor");
		}
		fieldGenerators.add(fieldGenerator);
		sortedFieldGenerators.add(fieldGenerator);
		Collections.sort(sortedFieldGenerators);
	}

	public T get(FieldDescriptor field)
	{
		if (field.getContainingType() != descriptor)
		{
			throw new IllegalArgumentException("Field not belonging to the descriptor");
		}
		return fieldGenerators.get(field.getIndex());
	}

	public List<T> getFieldGenerators()
	{
		return fieldGenerators;
	}
	
	public List<T> getSortedFieldGenerators()
	{
		return sortedFieldGenerators;
	}
}
