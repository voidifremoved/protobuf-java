package com.rubberjam.protobuf.another.compiler.java;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FieldCommonTest {

  @Test
  public void testGetKotlinPropertyName() {
    // Tests derived from the algorithm description in field_common.h
    //

    // "getFoo" -> "foo" (Simple case: one capital at start)
    assertEquals("foo", FieldCommon.getKotlinPropertyName("Foo"));

    // "getX" -> "x"
    assertEquals("x", FieldCommon.getKotlinPropertyName("X"));

    // "getHTMLPage" -> "htmlPage" (Multiple capitals: all but last get lowercased)
    assertEquals("htmlPage", FieldCommon.getKotlinPropertyName("HTMLPage"));

    // "getID" -> "id" (Only capitals: all get lowercased)
    assertEquals("id", FieldCommon.getKotlinPropertyName("ID"));
    
    // "getFooBar" -> "fooBar" (Standard CamelCase)
    assertEquals("fooBar", FieldCommon.getKotlinPropertyName("FooBar"));
    
    // "getURL" -> "url"
    assertEquals("url", FieldCommon.getKotlinPropertyName("URL"));
  }
}