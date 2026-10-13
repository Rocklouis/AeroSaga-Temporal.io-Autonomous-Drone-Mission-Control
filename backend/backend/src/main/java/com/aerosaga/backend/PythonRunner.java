package com.aerosaga.backend;

import org.graalvm.polyglot.Context;

public class PythonRunner {

    public static void main(String[] args) {
        try (Context context = Context.newBuilder("python")
                .allowAllAccess(true)
                .build()) {

            context.eval("python", "print('Hello')");
        }
    }
}