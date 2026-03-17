package com.rubberjam.protobuf.compiler.java;

import org.junit.Test;


public class IncrementalParityTestStep2 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_2.proto", "IncrementalTestStep2.java");
	}
}
