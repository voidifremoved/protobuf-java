package com.google.protobuf;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;

/**
 * Helper class for generating code.
 */
public class InternalHelpers
{

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
