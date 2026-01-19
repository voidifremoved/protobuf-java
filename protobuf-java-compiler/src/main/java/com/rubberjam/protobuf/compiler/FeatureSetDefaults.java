// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler;

import java.util.List;

/**
 * A compiled specification for the defaults of a set of features.
 */
public class FeatureSetDefaults
{
	private final List<FeatureSetEditionDefault> defaults;
	private final Edition minimumEdition;
	private final Edition maximumEdition;

	public FeatureSetDefaults(
			List<FeatureSetEditionDefault> defaults, Edition minimumEdition, Edition maximumEdition)
	{
		this.defaults = defaults;
		this.minimumEdition = minimumEdition;
		this.maximumEdition = maximumEdition;
	}

	public List<FeatureSetEditionDefault> getDefaults()
	{
		return defaults;
	}

	public Edition getMinimumEdition()
	{
		return minimumEdition;
	}

	public Edition getMaximumEdition()
	{
		return maximumEdition;
	}

	/**
	 * A map from every known edition with a unique set of defaults to its
	 * defaults.
	 */
	public static class FeatureSetEditionDefault
	{
		private final Edition edition;
		private final FeatureSet overridableFeatures;
		private final FeatureSet fixedFeatures;

		public FeatureSetEditionDefault(
				Edition edition, FeatureSet overridableFeatures, FeatureSet fixedFeatures)
		{
			this.edition = edition;
			this.overridableFeatures = overridableFeatures;
			this.fixedFeatures = fixedFeatures;
		}

		public Edition getEdition()
		{
			return edition;
		}

		public FeatureSet getOverridableFeatures()
		{
			return overridableFeatures;
		}

		public FeatureSet getFixedFeatures()
		{
			return fixedFeatures;
		}
	}
}
