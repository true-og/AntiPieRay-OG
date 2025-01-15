package net.trueog.antipierayog.util;

public class Throwables {

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {

        throw (T) t;

    }

}