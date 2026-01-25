package com.rubberjam.protobuf.compiler.java;

import java.io.PrintWriter;
import java.io.Writer;

public class IndentPrinter extends PrintWriter {
    private final String indentStep;
    private StringBuilder currentIndent = new StringBuilder();
    private boolean atStartOfLine = true;

    public IndentPrinter(Writer out, String indentStep) {
        super(out);
        this.indentStep = indentStep;
    }

    public void indent() {
        currentIndent.append(indentStep);
    }

    public void outdent() {
        if (currentIndent.length() >= indentStep.length()) {
            currentIndent.setLength(currentIndent.length() - indentStep.length());
        }
    }

    private void writeIndent() {
        if (atStartOfLine && currentIndent.length() > 0) {
            String indent = currentIndent.toString();
            super.write(indent.toCharArray(), 0, indent.length());
            atStartOfLine = false;
        }
    }

    @Override
    public void write(int c) {
        if (c == '\n') {
            super.write(c);
            atStartOfLine = true;
        } else {
            writeIndent();
            super.write(c);
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        for (int i = 0; i < len; i++) {
            write(buf[off + i]);
        }
    }

    @Override
    public void write(String s, int off, int len) {
        for (int i = 0; i < len; i++) {
            write(s.charAt(off + i));
        }
    }

    @Override
    public void println() {
        write('\n');
    }
}
