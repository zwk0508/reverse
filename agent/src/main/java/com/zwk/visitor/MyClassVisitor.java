package com.zwk.visitor;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;



import java.lang.reflect.Modifier;

public class MyClassVisitor extends ClassVisitor {
    public MyClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (Modifier.isPrivate(access)) {
            return methodVisitor;
        }
        try {
            return new MyMethodVisitor(Opcodes.ASM5, methodVisitor);
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return methodVisitor;
        }
    }
}
