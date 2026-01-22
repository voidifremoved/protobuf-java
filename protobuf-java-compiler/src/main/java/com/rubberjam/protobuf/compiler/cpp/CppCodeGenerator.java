package com.rubberjam.protobuf.compiler.cpp;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CodeGenerator;
import com.rubberjam.protobuf.compiler.CodeGenerator.GenerationException;
import com.rubberjam.protobuf.compiler.GeneratorContext;

import java.io.IOException;
import java.io.PrintWriter;

public class CppCodeGenerator extends CodeGenerator {

    @Override
    public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
            throws GenerationException {

        String basename = file.getName();
        if (basename.endsWith(".proto")) {
            basename = basename.substring(0, basename.length() - ".proto".length());
        }

        CppFileGenerator fileGenerator = new CppFileGenerator(file);

        try {
            // Generate .pb.h
            String headerFilename = basename + ".pb.h";
            try (PrintWriter writer = new PrintWriter(generatorContext.open(headerFilename))) {
                fileGenerator.generateHeader(writer);
            }

            // Generate .pb.cc
            try (PrintWriter writer = new PrintWriter(generatorContext.open(basename + ".pb.cc"))) {
                fileGenerator.generateSource(writer, headerFilename);
            }
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    @Override
    public long getSupportedFeatures() {
        return Feature.FEATURE_PROTO3_OPTIONAL.getValue();
    }
}
