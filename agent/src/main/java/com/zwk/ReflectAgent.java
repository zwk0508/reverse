package com.zwk;

import com.zwk.transformer.ReflectTransformer;

import java.lang.instrument.Instrumentation;

public class ReflectAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ReflectTransformer(agentArgs), true);
    }
}
