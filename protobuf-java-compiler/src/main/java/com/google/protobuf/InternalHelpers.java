package com.google.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Syntax;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for generating code.
 */
public class InternalHelpers
{

	public static boolean supportUnknownEnumValue(FieldDescriptor field)
	{
		return field.getEnumType() != null && !field.getFile().toProto().getSyntax().equals("proto2");
	}

	public static boolean checkUtf8(FieldDescriptor descriptor)
	{
		return descriptor.getType() == FieldDescriptor.Type.STRING;
	}

	public static String getClassName(Descriptor descriptor)
	{
		return descriptor.getName();
	}

	public static boolean isMapEntry(Descriptor descriptor)
	{
		return descriptor.getOptions().getMapEntry();
	}

	public static int getMapKeyField(Descriptor descriptor)
	{
		// Map entries always have key = 1, value = 2
		return 1;
	}

	public static int getMapValueField(Descriptor descriptor)
	{
		return 2;
	}

	public static boolean hasHasbit(FieldDescriptor descriptor)
	{
		if (!descriptor.hasPresence())
		{
			return false;
		}
		OneofDescriptor oneof = descriptor.getContainingOneof();
		return oneof == null || oneof.isSynthetic();
	}

	// Add other methods from internal_helpers.cc as needed
}
