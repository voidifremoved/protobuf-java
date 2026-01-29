// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.another.compiler;

/**
 * An interface for collecting errors.
 */
public interface ErrorCollector
{
	void recordError(int line, int column, String message);
}
