package com.claude.cameo.bridge.util;

import java.util.jar.JarFile;

/** Resolve bridge class signatures without initializing Cameo or opening a project. */
public final class SdkLinkageProbe {
    public static void main(String[] args) throws Exception {
        int expectedJava = Integer.parseInt(args[1]);
        if (Runtime.version().feature() != expectedJava) throw new AssertionError("Wrong validation JVM");
        int count = 0;
        try (JarFile jar = new JarFile(args[0])) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class")) continue;
                Class<?> type = Class.forName(name.substring(0, name.length()-6).replace('/', '.'), false,
                        SdkLinkageProbe.class.getClassLoader());
                type.getDeclaredMethods();
                type.getDeclaredConstructors();
                type.getDeclaredFields();
                count++;
            }
        }
        // The plugin starts its listeners as an after-startup activity, once saved options are applied.
        Class.forName("com.nomagic.magicdraw.core.Application", false, SdkLinkageProbe.class.getClassLoader())
                .getMethod("addActivityAfterStartup", Runnable.class);
        boolean wait = GenericTableLoad.supportsWait();
        if (wait != (expectedJava == 17)) throw new AssertionError("Unexpected table wait API availability");
        System.out.println("Resolved " + count + " bridge classes on Java " + expectedJava
                + "; table wait API=" + wait + "; after-startup activity API present"
                + "; no Cameo application/model initialized");
    }
}
