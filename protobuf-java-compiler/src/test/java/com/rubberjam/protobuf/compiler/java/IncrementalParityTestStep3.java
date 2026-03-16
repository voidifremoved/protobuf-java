package com.rubberjam.protobuf.compiler.java;

import org.junit.Test;


public class IncrementalParityTestStep3 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_3.proto", "IncrementalTestStep3.java");
	}
}
