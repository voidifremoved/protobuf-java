package com.rubberjam.protobuf.compiler.java;

import org.junit.Test;


public class IncrementalParityTestStep1 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_1.proto", "IncrementalTestStep1.java");
	}
}
