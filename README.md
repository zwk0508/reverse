# reverse -方法调用的逆向工程

github地址：[https://github.com/zwk0508/reverse.git](https://github.com/zwk0508/reverse.git)

### 概述

很多框架中会用到方法的反射调用，反射调用会降低代码的执行速度，借用Java的agent技术实现反射的正向调用，即把反射调用改为直接调用

### 原理

1. 使用java agent技术实现类加载时字节码的转换，把method.invoke的方法调用替换为com.zwk.invoke.ReflectInvoke.invoke方法
2. 借助字节码技术实现类的动态生成，实现直接调用

### 使用

指定agent： -javaagent:<jar路径>\reverse-agent.jar=<需要处理的包名，多报名以都好分隔>，在maven中引入reflect的jar包

```code
 <dependency>
    <groupId>com.zwk</groupId>
    <artifactId>reflect</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 举例

```java
public class MyInvocationHandler implements InvocationHandler {
    private Object target;

    public MyInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    //其中method.invoke会被替换成ReflectInvoke.invoke
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }

    private void doAnotherSomething() {
        Runnable r1 = (Runnable) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Runnable.class},
                new MyInvocationHandler((Runnable) () -> System.out.println("run")));
        r1.run();
    }
}

```

代码中的method.invoke会被替换成ReflectInvoke.invoke

执行ReflectInvoke.invoke方法会动态生成一个Invoker类，具体生成的内容可以通过设置属性，`System.setProperty("com.zwk.reflect.saveGeneratedFiles", "true");`
查看生成的类文件 例如：

```java
public final class Generator$2$Invoker implements Invoker {
    public Generator$2$Invoker() {
    }

    public Object invoke(Object var1, Object[] var2) {
        ((Runnable) var1).run();
        return null;
    }
}
```
