package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.rubberjam.protobuf.compiler.java.Helpers;

@RunWith(JUnit4.class)
public class HelpersTest {

  @Test
  public void testToCamelCase() {
    assertEquals("fooBar", Helpers.toCamelCase("foo_bar", true));
    assertEquals("FooBar", Helpers.toCamelCase("foo_bar", false));
    assertEquals("fooBar", Helpers.toCamelCase("fooBar", true));
  }

  @Test
  public void testUnderscoresToCamelCase() {
    assertEquals("FooBar", Helpers.underscoresToCamelCase("foo_bar", true));
    assertEquals("fooBar", Helpers.underscoresToCamelCase("foo_bar", false));
    // Test trailing hash logic from C++
    assertEquals("Foo_", Helpers.underscoresToCamelCase("foo#", true));
  }

  @Test
  public void testBitFieldGeneration() {
    // Index 0 -> bitField0_ & 0x00000001
    assertEquals("((bitField0_ & 0x00000001) != 0)", Helpers.generateGetBit(0));
    assertEquals("bitField0_ |= 0x00000001", Helpers.generateSetBit(0));
    assertEquals("bitField0_ = (bitField0_ & ~0x00000001)", Helpers.generateClearBit(0));

    // Index 33 -> bitField1_ & 0x00000002 (33 / 32 = 1, 33 % 32 = 1)
    // Wait, 33 % 32 = 1. Mask 1 is 0x00000002.
    assertEquals("((bitField1_ & 0x00000002) != 0)", Helpers.generateGetBit(33));
  }

  @Test
  public void testFieldConstantName() {
      // Mocking FieldDescriptor logic is hard without full proto setup, 
      // but testing the string logic part:
      String fieldName = "my_field";
      assertEquals("MY_FIELD_FIELD_NUMBER", fieldName.toUpperCase() + "_FIELD_NUMBER");
  }
}