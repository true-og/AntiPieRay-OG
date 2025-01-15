package net.trueog.antipierayog.util;

@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Throwable;

}