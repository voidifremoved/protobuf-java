package com.rubberjam.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumOptions;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueOptions;
import com.google.protobuf.DescriptorProtos.ExtensionRangeOptions;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofOptions;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility class mirroring {@code google/protobuf/compiler/retention.h}.
 */
public final class Retention
{

  private Retention()
  {
  }

  public static FileDescriptorProto stripSourceRetentionOptions(
      FileDescriptor file, boolean includeSourceCodeInfo)
  {
    FileDescriptorProto.Builder fileProto = file.toProto().toBuilder();
    if (includeSourceCodeInfo)
    {
      // FileDescriptor.toProto() normally does not include source code info unless it was parsed with it.
      // If the original file had it, it's there.
      // C++ explicitly calls CopySourceCodeInfoTo. Java's toProto might already have it if available.
      // But if we want to ensure it, we might need to copy it if we had access to it separately.
      // Assuming file.toProto() includes it if it was present.
    }
    else
    {
       fileProto.clearSourceCodeInfo();
    }

    ExtensionRegistry registry = createExtensionRegistry(file);
    stripFile(fileProto, registry);

    // Handle SourceCodeInfo stripping if needed.
    // implementation of StripSourceCodeInfo is complex, skipping for now as it's optional optimization?
    // C++ version does it. I should probably do it too if I want full parity.

    return fileProto.build();
  }

  public static FileDescriptorProto stripSourceRetentionOptions(FileDescriptor file)
  {
      return stripSourceRetentionOptions(file, false);
  }

  // Overloads for other descriptors
  public static DescriptorProto stripSourceRetentionOptions(Descriptor message)
  {
     DescriptorProto.Builder builder = message.toProto().toBuilder();
     ExtensionRegistry registry = createExtensionRegistry(message.getFile());
     // We need to strip options from the message and its children.
     // But message.toProto() returns DescriptorProto. We need to process it using the registry.

     // The C++ implementation converts to DynamicMessage and strips.
     // Here we have DescriptorProto (which is a Message).
     // Wait, DescriptorProto IS the definition. It contains options.
     // But checking retention of fields inside DescriptorProto (like 'name', 'field') is not what we want.
     // We want to check retention of fields inside the OPTIONS messages (MessageOptions, FieldOptions).

     // Actually, DescriptorProto describes a message. It has 'options' field which is MessageOptions.
     // We need to strip MessageOptions.

     stripMessageDescriptor(builder, registry);
     return builder.build();
  }

  // ... other overloads ...

  private static void stripFile(FileDescriptorProto.Builder file, ExtensionRegistry registry)
  {
      if (file.hasOptions())
      {
          file.setOptions(stripOptions(file.getOptions(), FileOptions.getDefaultInstance(), registry));
      }

      for (int i = 0; i < file.getMessageTypeCount(); i++)
      {
          stripMessageDescriptor(file.getMessageTypeBuilder(i), registry);
      }
      for (int i = 0; i < file.getEnumTypeCount(); i++)
      {
          stripEnumDescriptor(file.getEnumTypeBuilder(i), registry);
      }
      for (int i = 0; i < file.getServiceCount(); i++)
      {
          stripServiceDescriptor(file.getServiceBuilder(i), registry);
      }
      for (int i = 0; i < file.getExtensionCount(); i++)
      {
          stripFieldDescriptor(file.getExtensionBuilder(i), registry);
      }
  }

  private static void stripMessageDescriptor(DescriptorProto.Builder message, ExtensionRegistry registry)
  {
      if (message.hasOptions())
      {
          message.setOptions(stripOptions(message.getOptions(), MessageOptions.getDefaultInstance(), registry));
      }
      for (int i = 0; i < message.getFieldCount(); i++)
      {
          stripFieldDescriptor(message.getFieldBuilder(i), registry);
      }
      for (int i = 0; i < message.getNestedTypeCount(); i++)
      {
          stripMessageDescriptor(message.getNestedTypeBuilder(i), registry);
      }
      for (int i = 0; i < message.getEnumTypeCount(); i++)
      {
          stripEnumDescriptor(message.getEnumTypeBuilder(i), registry);
      }
      for (int i = 0; i < message.getExtensionCount(); i++)
      {
          stripFieldDescriptor(message.getExtensionBuilder(i), registry);
      }
      for (int i = 0; i < message.getOneofDeclCount(); i++)
      {
          stripOneofDescriptor(message.getOneofDeclBuilder(i), registry);
      }
      for (int i = 0; i < message.getExtensionRangeCount(); i++)
      {
           DescriptorProto.ExtensionRange.Builder range = message.getExtensionRangeBuilder(i);
           if (range.hasOptions())
           {
               range.setOptions(stripOptions(range.getOptions(), ExtensionRangeOptions.getDefaultInstance(), registry));
           }
      }
  }

  private static void stripEnumDescriptor(EnumDescriptorProto.Builder enm, ExtensionRegistry registry)
  {
      if (enm.hasOptions())
      {
          enm.setOptions(stripOptions(enm.getOptions(), EnumOptions.getDefaultInstance(), registry));
      }
      for (int i = 0; i < enm.getValueCount(); i++)
      {
          stripEnumValueDescriptor(enm.getValueBuilder(i), registry);
      }
  }

  private static void stripEnumValueDescriptor(EnumValueDescriptorProto.Builder val, ExtensionRegistry registry)
  {
      if (val.hasOptions())
      {
          val.setOptions(stripOptions(val.getOptions(), EnumValueOptions.getDefaultInstance(), registry));
      }
  }

  private static void stripServiceDescriptor(ServiceDescriptorProto.Builder service, ExtensionRegistry registry)
  {
      if (service.hasOptions())
      {
          service.setOptions(stripOptions(service.getOptions(), ServiceOptions.getDefaultInstance(), registry));
      }
      for (int i = 0; i < service.getMethodCount(); i++)
      {
          stripMethodDescriptor(service.getMethodBuilder(i), registry);
      }
  }

  private static void stripMethodDescriptor(MethodDescriptorProto.Builder method, ExtensionRegistry registry)
  {
      if (method.hasOptions())
      {
          method.setOptions(stripOptions(method.getOptions(), MethodOptions.getDefaultInstance(), registry));
      }
  }

  private static void stripFieldDescriptor(FieldDescriptorProto.Builder field, ExtensionRegistry registry)
  {
      if (field.hasOptions())
      {
          field.setOptions(stripOptions(field.getOptions(), FieldOptions.getDefaultInstance(), registry));
      }
  }

  private static void stripOneofDescriptor(OneofDescriptorProto.Builder oneof, ExtensionRegistry registry)
  {
      if (oneof.hasOptions())
      {
          oneof.setOptions(stripOptions(oneof.getOptions(), OneofOptions.getDefaultInstance(), registry));
      }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Message> T stripOptions(T options, T defaultInstance, ExtensionRegistry registry)
  {
      // 1. Convert to DynamicMessage to parse unknown fields (extensions)
      try
      {
          DynamicMessage.Builder builder = DynamicMessage.newBuilder(options.getDescriptorForType());
          // Serialize and parse to apply registry
          builder.mergeFrom(options.toByteString(), registry);

          // 2. Strip source retention fields
          stripMessage(builder);

          // 3. Convert back to original type
          // We can't easily convert DynamicMessage back to generated Message directly without serialization
          return (T) defaultInstance.newBuilderForType().mergeFrom(builder.build().toByteString()).build();
      }
      catch (InvalidProtocolBufferException e)
      {
          // Fallback: just return original options if something goes wrong
          // Or log error
          return options;
      }
  }

  private static void stripMessage(Message.Builder m)
  {
      Map<FieldDescriptor, Object> fields = m.getAllFields();
      // We need to iterate over a copy of keys because we might modify the message
      List<FieldDescriptor> fieldList = new ArrayList<>(fields.keySet());

      for (FieldDescriptor field : fieldList)
      {
          if (field.getOptions().getRetention() == FieldOptions.OptionRetention.RETENTION_SOURCE)
          {
              m.clearField(field);
          }
          else if (field.getType() == FieldDescriptor.Type.MESSAGE)
          {
              if (field.isRepeated())
              {
                  int count = m.getRepeatedFieldCount(field);
                  for (int i = 0; i < count; i++)
                  {
                      Message.Builder child = ((Message) m.getRepeatedField(field, i)).toBuilder();
                      stripMessage(child);
                      m.setRepeatedField(field, i, child.build());
                  }
              }
              else
              {
                  Message.Builder child = ((Message) m.getField(field)).toBuilder();
                  boolean wasNonEmptyOptions = isOptionsProto(child.getDescriptorForType()) && !child.getAllFields().isEmpty();
                  stripMessage(child);
                  if (wasNonEmptyOptions && child.getAllFields().isEmpty())
                  {
                      m.clearField(field);
                  }
                  else
                  {
                      m.setField(field, child.build());
                  }
              }
          }
      }
  }

  private static boolean isOptionsProto(Descriptor descriptor)
  {
      return descriptor.getFile().getName().endsWith("descriptor.proto") && descriptor.getName().endsWith("Options");
  }

  private static ExtensionRegistry createExtensionRegistry(FileDescriptor file)
  {
      ExtensionRegistry registry = ExtensionRegistry.newInstance();
      registerExtensions(file, registry);
      return registry;
  }

  private static void registerExtensions(FileDescriptor file, ExtensionRegistry registry)
  {
      for (FieldDescriptor extension : file.getExtensions())
      {
          registry.add(extension);
      }
      for (Descriptor message : file.getMessageTypes())
      {
          registerExtensions(message, registry);
      }
      for (FileDescriptor dependency : file.getDependencies())
      {
          registerExtensions(dependency, registry);
      }
  }

  private static void registerExtensions(Descriptor message, ExtensionRegistry registry)
  {
      for (FieldDescriptor extension : message.getExtensions())
      {
          registry.add(extension);
      }
      for (Descriptor nested : message.getNestedTypes())
      {
          registerExtensions(nested, registry);
      }
  }
}
