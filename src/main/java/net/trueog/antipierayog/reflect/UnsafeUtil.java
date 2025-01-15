package net.trueog.antipierayog.reflect;

import java.lang.reflect.Field;

import net.trueog.antipierayog.util.Throwables;
import sun.misc.Unsafe;

public class UnsafeUtil {

    // Declare unsafe instance.
    static final Unsafe UNSAFE;

    static {

        // Placeholder
        Unsafe unsafe = null;

        try {
            // Get real value using reflection.
            Field field = Unsafe.class.getDeclaredField("theUnsafe");

            field.setAccessible(true);

            unsafe = (Unsafe) field.get(null);

        } catch (Exception error) {

            // Rethrow error.
            Throwables.sneakyThrow(error);

        }

        // Set unsafe.
        UNSAFE = unsafe;

    }

    public static Unsafe getUnsafe() {

        return UNSAFE;

    }

}