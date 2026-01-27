package com.rubberjam.protobuf.compiler;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractContext<O> {
    private final FileDescriptorProto sourceProto;
    protected final Map<FieldDescriptor, FieldGeneratorInfo<O>> fieldGeneratorInfoMap = new HashMap<>();
    protected final Map<OneofDescriptor, OneofGeneratorInfo> oneofGeneratorInfoMap = new HashMap<>();

    public AbstractContext(FileDescriptorProto sourceProto) {
        this.sourceProto = sourceProto;
    }

    public FileDescriptorProto getSourceProto() {
        return sourceProto;
    }

    public FieldGeneratorInfo<O> getFieldGeneratorInfo(FieldDescriptor field) {
        return fieldGeneratorInfoMap.get(field);
    }

    public OneofGeneratorInfo getOneofGeneratorInfo(OneofDescriptor oneof) {
        return oneofGeneratorInfoMap.get(oneof);
    }

    public boolean isSyntheticOneof(OneofDescriptor oneof) {
        return oneof.getFieldCount() == 1 && oneof.getField(0).toProto().getProto3Optional();
    }

    public boolean isSyntheticOneofField(FieldDescriptor field) {
        return field.getContainingOneof() != null && field.toProto().hasProto3Optional()
                && field.toProto().getProto3Optional();
    }

    public boolean isRealOneof(FieldDescriptor field) {
        return field.getContainingOneof() != null && !isSyntheticOneofField(field);
    }
}
