package com.zwk.invoke;


import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static jdk.internal.org.objectweb.asm.Opcodes.*;


public class ReflectInvoke {
    private static final Map<Method, Invoker> cache = new ConcurrentHashMap<>();
    /**
     * unsafe
     */
    private static final Unsafe unsafe;
    /**
     * 生成类的自增索引
     */
    private static final AtomicInteger index = new AtomicInteger(1);
    /**
     * 生成类名的前缀
     */
    private static final String CLASS_NAME_PREFIX = "com/zwk/Generator$";
    /**
     * 生成类名的后缀
     */
    private static final String CLASS_NAME_SUFFIX = "$Invoker";

    /**
     * Object的类名
     */
    private static final String OBJECT_CLASS_NAME = "java/lang/Object";
    /**
     * 实现的接口名
     */
    private static final String INVOKER_INTERFACE_NAME = "com/zwk/invoke/Invoker";

    /**
     * 构造器名称
     */
    private static final String CONSTRUCTOR_NAME = "<init>";
    /**
     * 构造器描述符
     */
    private static final String CONSTRUCTOR_DESCRIPTOR = "()V";

    /**
     * 实现接口的方法
     */
    private static final String INVOKER_METHOD_NAME = "invoke";
    /**
     * 实现接口的描述符
     */
    private static final String INVOKER_METHOD_DESCRIPTOR = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

    /**
     * 保存生成的class文件
     */
    private static final String SAVE_GENERATED_FILES = "com.zwk.reflect.saveGeneratedFiles";

    public static Object invoke(Method method, Object obj, Object... args) throws Throwable {
        Invoker invoker = cache.get(method);
        if (invoker == null) {
            synchronized (cache) {
                invoker = cache.get(method);
                if (invoker == null) {
                    invoker = generateInvoker(method);
                }
                cache.put(method, invoker);
            }
        }
        return invoker.invoke(obj, args);
    }

    private static Invoker generateInvoker(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String className = declaringClass.getName().replace('.', '/');
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        boolean voidReturn = false;
        Type type = Type.getType(method);
        Type returnType = type.getReturnType();
        if (returnType == Type.VOID_TYPE) {
            voidReturn = true;
        }
        String fullClassName = CLASS_NAME_PREFIX + index.getAndIncrement() + CLASS_NAME_SUFFIX;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                fullClassName,
                null,
                OBJECT_CLASS_NAME,
                new String[]{INVOKER_INTERFACE_NAME});

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, CONSTRUCTOR_DESCRIPTOR, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, OBJECT_CLASS_NAME, CONSTRUCTOR_NAME, CONSTRUCTOR_DESCRIPTOR, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, INVOKER_METHOD_NAME, INVOKER_METHOD_DESCRIPTOR, null, null);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, className);
        }
        Type[] argumentTypes = type.getArgumentTypes();
        if (argumentTypes.length > 0) {
            for (int i = 0; i < argumentTypes.length; i++) {
                Type argumentType = argumentTypes[i];
                mv.visitVarInsn(ALOAD, 2);
                mv.visitIntInsn(BIPUSH, i);
                mv.visitInsn(AALOAD);
                handleArgumentType(argumentType, mv);
            }
        }
        boolean isInterface = declaringClass.isInterface();
        mv.visitMethodInsn(isStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL, className, method.getName(), type.getDescriptor(), isInterface);
        if (voidReturn) {
            mv.visitInsn(ACONST_NULL);
        } else {
            handleReturnType(returnType, mv);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        byte[] bytes = cw.toByteArray();
        String property = System.getProperty(SAVE_GENERATED_FILES);
        if (Objects.equals("true", property)) {
            File file = new File(fullClassName + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(bytes);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        Class<?> clazz = unsafe.defineClass(fullClassName, bytes, 0, bytes.length, ClassLoader.getSystemClassLoader(), null);
        try {
            return (Invoker) clazz.newInstance();
        } catch (Exception e) {
            try {
                return (Invoker) unsafe.allocateInstance(clazz);
            } catch (InstantiationException ex) {
                return (s, t) -> null;
            }
        }
    }

    private static void handleReturnType(Type returnType, MethodVisitor mv) {
        if (returnType == Type.BOOLEAN_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (returnType == Type.CHAR_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        } else if (returnType == Type.BYTE_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (returnType == Type.SHORT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        } else if (returnType == Type.INT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (returnType == Type.FLOAT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (returnType == Type.LONG_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (returnType == Type.DOUBLE_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        }
    }

    private static void handleArgumentType(Type argumentType, MethodVisitor mv) {
        if (argumentType == Type.BOOLEAN_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (argumentType == Type.CHAR_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        } else if (argumentType == Type.BYTE_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
        } else if (argumentType == Type.SHORT_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
        } else if (argumentType == Type.INT_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        } else if (argumentType == Type.FLOAT_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        } else if (argumentType == Type.LONG_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        } else if (argumentType == Type.DOUBLE_TYPE) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        }
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
