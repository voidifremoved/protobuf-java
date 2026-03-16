package com.rubberjam.protobuf.compiler.java;

import org.junit.Test;


public class IncrementalParityTestStep4 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_4.proto", "IncrementalTestStep4.java");
	}
}
