package com.zwk.visitor;


import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.util.Objects;

public class MyMethodVisitor extends MethodVisitor {

    private static final String REFLECT_INVOKE_CLASS_NAME = "com/zwk/invoke/ReflectInvoke";
    private static final String REFLECT_INVOKE_METHOD_NAME = "invoke";
    private static final String REFLECT_INVOKE_METHOD_DESCRIPTOR = "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String REFLECT_METHOD_CLASS_NAME = "java/lang/reflect/Method";
    private static final String REFLECT_METHOD_NAME = "invoke";


    public MyMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (Objects.equals(REFLECT_METHOD_CLASS_NAME, owner) && Objects.equals(REFLECT_METHOD_NAME, name)) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    REFLECT_INVOKE_CLASS_NAME,
                    REFLECT_INVOKE_METHOD_NAME,
                    REFLECT_INVOKE_METHOD_DESCRIPTOR,
                    isInterface);
        } else {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
