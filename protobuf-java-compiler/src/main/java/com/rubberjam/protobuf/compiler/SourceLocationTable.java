// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * A table of source locations.
 */
public class SourceLocationTable
{

	private final List<SourceCodeInfo.Location> locations = new ArrayList<>();

	/**
	 * Adds a location to the table.
	 */
	public void add(int[] path, int[] span, String leadingComments, String trailingComments)
	{
		SourceCodeInfo.Location.Builder location = SourceCodeInfo.Location.newBuilder();
		for (int p : path)
		{
			location.addPath(p);
		}
		for (int s : span)
		{
			location.addSpan(s);
		}
		if (leadingComments != null)
		{
			location.setLeadingComments(leadingComments);
		}
		if (trailingComments != null)
		{
			location.setTrailingComments(trailingComments);
		}
		locations.add(location.build());
	}

	public void add(int[] path, int[] span)
	{
		add(path, span, null, null);
	}

	/**
	 * Returns the locations in the table.
	 */
	public List<SourceCodeInfo.Location> getLocations()
	{
		return locations;
	}
}
