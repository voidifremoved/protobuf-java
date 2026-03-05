package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep10 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_10.proto", "IncrementalTestStep10.java");
	}
}
