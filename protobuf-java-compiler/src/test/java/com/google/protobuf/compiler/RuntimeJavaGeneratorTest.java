package com.google.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.midoki.jupiter.gameserver.messages.MessageCopier;
import com.rubberjam.protobuf.compiler.runtime.RuntimeCompiler;
import com.rubberjam.protobuf.compiler.runtime.RuntimeJavaGenerator;

public class RuntimeJavaGeneratorTest
{
	@Test
	public void testGenerateJavaSourceFromDescriptorProto() throws Exception
	{
		DescriptorProto message = DescriptorProto.newBuilder()
				.setName("Person")
				.addField(FieldDescriptorProto.newBuilder()
						.setName("name")
						.setNumber(1)
						.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
						.setType(FieldDescriptorProto.Type.TYPE_STRING)
						.build())
				.build();

		FileDescriptorProto fileProto = FileDescriptorProto.newBuilder()
				.setName("runtime_test.proto")
				.setPackage("com.example")
				.setSyntax("proto3")
				.addMessageType(message)
				.build();

		RuntimeJavaGenerator.GeneratedJavaFile generated = RuntimeJavaGenerator.generateJavaSource(fileProto, null, "");

		assertEquals("com/example/RuntimeTestProto.java", generated.getFileName());
		assertEquals("com.example", generated.getPackageName());
		assertEquals("RuntimeTestProto", generated.getClassName());
		assertTrue(generated.getSource().contains("public final class RuntimeTestProto"));
		assertTrue(generated.getSource().contains("private volatile java.lang.Object name_"));
		
		RuntimeCompiler compiler = new RuntimeCompiler(getClass().getClassLoader());
		
		
		System.out.println(generated.getSource());
		
		Class<? extends Object> compiledClass = compiler.compile(generated.getPackageName() + "." + generated.getClassName(), generated.getSource(), null);
		
		System.out.println(compiledClass);
	}
}
