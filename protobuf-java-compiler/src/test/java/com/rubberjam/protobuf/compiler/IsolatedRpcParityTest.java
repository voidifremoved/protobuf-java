package com.rubberjam.protobuf.compiler;

import org.junit.Test;

public class IsolatedRpcParityTest extends AbstractProtoParityTest {
	@Test
	public void testParity() throws Exception {
		verifyParity("IsolatedRpc.proto", "IsolatedRpc.java");
	}
}
