package com.rubberjam.protobuf.compiler;

import org.junit.Test;


public class IncrementalParityTestStep9 extends AbstractProtoParityTest
{
	@Test
	public void testParity() throws Exception
	{

		verifyParity("incremental_test_step_9.proto", "IncrementalTestStep9.java");
	}
}
