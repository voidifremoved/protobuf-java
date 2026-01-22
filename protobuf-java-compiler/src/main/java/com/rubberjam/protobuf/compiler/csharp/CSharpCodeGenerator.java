package com.rubberjam.protobuf.compiler.csharp;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CodeGenerator;
import com.rubberjam.protobuf.compiler.CodeGenerator.GenerationException;
import com.rubberjam.protobuf.compiler.GeneratorContext;

import java.io.IOException;
import java.io.PrintWriter;

public class CSharpCodeGenerator extends CodeGenerator {

    @Override
    public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
            throws GenerationException {

        String basename = file.getName();
        if (basename.endsWith(".proto")) {
            basename = basename.substring(0, basename.length() - ".proto".length());
        }

        String csharpFilename = toPascalCase(basename) + ".cs";

        CSharpFileGenerator fileGenerator = new CSharpFileGenerator(file);

        try (PrintWriter writer = new PrintWriter(generatorContext.open(csharpFilename))) {
            fileGenerator.generate(writer);
        } catch (IOException e) {
            throw new GenerationException(e);
        }
    }

    private String toPascalCase(String name) {
        // Strip directory
        int idx = name.lastIndexOf('/');
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }

        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                up = true;
            } else {
                if (up) {
                    sb.append(Character.toUpperCase(c));
                    up = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public long getSupportedFeatures() {
        return Feature.FEATURE_PROTO3_OPTIONAL.getValue();
    }
}
