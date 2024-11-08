package net.trueog.antipierayog.reflect;

import java.lang.reflect.Field;

import net.trueog.antipierayog.util.Throwables;
import sun.misc.Unsafe;

public class UnsafeUtil {

	// the unsafe instance
	static final Unsafe UNSAFE;

	static {
		Unsafe unsafe = null; // temp var

		try {
			// get using reflection
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (Unsafe) field.get(null);
		} catch (Exception e) {
			// rethrow error
			Throwables.sneakyThrow(e);
		}

		// set unsafe
		UNSAFE = unsafe;
	}

	public static Unsafe getUnsafe() {
		return UNSAFE;
	}

}