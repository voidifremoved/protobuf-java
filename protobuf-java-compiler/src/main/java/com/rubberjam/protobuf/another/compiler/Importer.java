package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.InputStream;
import java.util.Map;

public class Importer {

    // Skeleton implementation

    public interface MultiFileErrorCollector {
        void recordError(String filename, int line, int column, String message);
        void recordWarning(String filename, int line, int column, String message);
    }

    public interface SourceTree {
        InputStream open(String filename);
        String getLastErrorMessage();
    }

    public static class DiskSourceTree implements SourceTree {
        public void mapPath(String virtualPath, String diskPath) {
            // ...
        }

        @Override
        public InputStream open(String filename) {
            return null;
        }

        @Override
        public String getLastErrorMessage() {
            return "";
        }
    }

    public Importer(SourceTree sourceTree, MultiFileErrorCollector errorCollector) {
    }

    public FileDescriptor importFile(String filename) {
        return null;
    }
}
