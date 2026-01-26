// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

/**
 * An enum representing the different types of field accessors.
 */
public enum FieldAccessorType
{
	HAZZER,
	GETTER,
	VALUE_GETTER,
	SETTER,
	VALUE_SETTER,
	CLEARER,
	// Repeated
	LIST_COUNT,
	LIST_GETTER,
	LIST_INDEXED_GETTER,
	LIST_INDEXED_SETTER,
	LIST_ADDER,
	LIST_MULTI_ADDER,
	// Repeated Enum Values
	LIST_VALUE_GETTER,
	LIST_INDEXED_VALUE_GETTER,
	LIST_INDEXED_VALUE_SETTER,
	LIST_VALUE_ADDER,
	LIST_VALUE_MULTI_ADDER
}
