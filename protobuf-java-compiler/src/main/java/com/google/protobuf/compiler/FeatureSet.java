// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

/**
 * A message representing a set of features.
 */
public class FeatureSet {
  private final FieldPresence fieldPresence;
  private final EnumType enumType;
  private final RepeatedFieldEncoding repeatedFieldEncoding;
  private final Utf8Validation utf8Validation;
  private final MessageEncoding messageEncoding;
  private final JsonFormat jsonFormat;

  public FeatureSet(
      FieldPresence fieldPresence,
      EnumType enumType,
      RepeatedFieldEncoding repeatedFieldEncoding,
      Utf8Validation utf8Validation,
      MessageEncoding messageEncoding,
      JsonFormat jsonFormat) {
    this.fieldPresence = fieldPresence;
    this.enumType = enumType;
    this.repeatedFieldEncoding = repeatedFieldEncoding;
    this.utf8Validation = utf8Validation;
    this.messageEncoding = messageEncoding;
    this.jsonFormat = jsonFormat;
  }

  public FieldPresence getFieldPresence() {
    return fieldPresence;
  }

  public EnumType getEnumType() {
    return enumType;
  }

  public RepeatedFieldEncoding getRepeatedFieldEncoding() {
    return repeatedFieldEncoding;
  }

  public Utf8Validation getUtf8Validation() {
    return utf8Validation;
  }

  public MessageEncoding getMessageEncoding() {
    return messageEncoding;
  }

  public JsonFormat getJsonFormat() {
    return jsonFormat;
  }

  public enum FieldPresence {
    FIELD_PRESENCE_UNKNOWN(0),
    EXPLICIT(1),
    IMPLICIT(2),
    LEGACY_REQUIRED(3);

    private final int value;

    FieldPresence(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum EnumType {
    ENUM_TYPE_UNKNOWN(0),
    OPEN(1),
    CLOSED(2);

    private final int value;

    EnumType(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum RepeatedFieldEncoding {
    REPEATED_FIELD_ENCODING_UNKNOWN(0),
    PACKED(1),
    EXPANDED(2);

    private final int value;

    RepeatedFieldEncoding(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum Utf8Validation {
    UTF8_VALIDATION_UNKNOWN(0),
    VERIFY(2),
    NONE(3);

    private final int value;

    Utf8Validation(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum MessageEncoding {
    MESSAGE_ENCODING_UNKNOWN(0),
    LENGTH_PREFIXED(1),
    DELIMITED(2);

    private final int value;

    MessageEncoding(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum JsonFormat {
    JSON_FORMAT_UNKNOWN(0),
    ALLOW(1),
    LEGACY_BEST_EFFORT(2);

    private final int value;

    JsonFormat(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }
}
