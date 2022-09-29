package com.zwk.transformer;

import com.zwk.visitor.MyClassVisitor;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

public class ReflectTransformer implements ClassFileTransformer {
    private List<String> names;


    public ReflectTransformer(String args) {
        if (args == null || args.length() == 0) {
            return;
        }
        String[] split = args.split(",");
        names = Arrays.asList(split);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (shouldReflect(className)) {
            ClassReader classReader = new ClassReader(classfileBuffer);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            MyClassVisitor visitor = new MyClassVisitor(Opcodes.ASM5, classWriter);
            classReader.accept(visitor, ClassReader.EXPAND_FRAMES);
            return classWriter.toByteArray();
        }
        return classfileBuffer;
    }

    private boolean shouldReflect(String className) {
        if (names == null || className == null) {
            return false;
        }
        for (String name : names) {
            if (className.startsWith(name)) {
                return true;
            }
        }
        return false;
    }
}
