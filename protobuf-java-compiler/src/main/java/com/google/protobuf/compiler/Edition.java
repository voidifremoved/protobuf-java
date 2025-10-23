// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

/**
 * The full set of known editions.
 */
public enum Edition {
  /**
   * A placeholder for an unknown edition value.
   */
  EDITION_UNKNOWN(0),

  /**
   * A placeholder edition for specifying default behaviors *before* a feature
   * was first introduced.  This is effectively an "infinite past".
   */
  EDITION_LEGACY(900),

  /**
   * Legacy syntax "editions".  These pre-date editions, but behave much like
   * distinct editions.  These can't be used to specify the edition of proto
   * files, but feature definitions must supply proto2/proto3 defaults for
   * backwards compatibility.
   */
  EDITION_PROTO2(998),
  EDITION_PROTO3(999),

  /**
   * Editions that have been released.  The specific values are arbitrary and
   * should not be depended on, but they will always be time-ordered for easy
   * comparison.
   */
  EDITION_2023(1000),
  EDITION_2024(1001),

  /**
   * Placeholder editions for testing feature resolution.  These should not be
   * used or relied on outside of tests.
   */
  EDITION_1_TEST_ONLY(1),
  EDITION_2_TEST_ONLY(2),
  EDITION_99997_TEST_ONLY(99997),
  EDITION_99998_TEST_ONLY(99998),
  EDITION_99999_TEST_ONLY(99999),

  /**
   * Placeholder for specifying unbounded edition support.  This should only
   * ever be used by plugins that can expect to never require any changes to
   * support a new edition.
   */
  EDITION_MAX(0x7FFFFFFF);

  private final int value;

  Edition(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
