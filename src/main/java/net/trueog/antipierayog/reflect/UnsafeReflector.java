package net.trueog.antipierayog.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.trueog.antipierayog.util.ThrowingRunnable;
import net.trueog.antipierayog.util.ThrowingSupplier;
import sun.misc.Unsafe;

// Base class for unsafe reflection functionality.
public class UnsafeReflector {

    UnsafeReflector() {
    }

    // Declare unsafe instance.
    static final Unsafe unsafe = UnsafeUtil.getUnsafe();

    // Declare global instance.
    static final UnsafeReflector instance = new UnsafeReflector();

    public static UnsafeReflector get() {

        return instance;

    }

    // Declare constants.
    final Method METHOD_Module_implAddOpens;
    final MethodHandle SETTER_Method_modifiers;
    final MethodHandle SETTER_Field_modifiers;
    final MethodHandles.Lookup INTERNAL_LOOKUP;
    {
        try {

            // Get lookup.
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.publicLookup();
            INTERNAL_LOOKUP = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(field),
                    unsafe.staticFieldOffset(field));

            SETTER_Method_modifiers = INTERNAL_LOOKUP.findSetter(Method.class, "modifiers", Integer.TYPE);
            SETTER_Field_modifiers = INTERNAL_LOOKUP.findSetter(Field.class, "modifiers", Integer.TYPE);
            METHOD_Module_implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class);

            forcePublic(METHOD_Module_implAddOpens);
        } catch (Throwable t) {

            // Throw exception in init.
            throw new ExceptionInInitializerError(t);

        }

    }

    // Execute 'safe' (no errors in signature required).
    private void doSafe(ThrowingRunnable runnable) {

        try {

            runnable.run();

        } catch (Throwable t) {

            throw new RuntimeException("Fatal error in unsafe reflection", t);

        }

    }

    // Execute 'safe' (no errors in signature required).
    private <T> T doSafe(ThrowingSupplier<T> runnable) {

        try {

            return runnable.issue();

        } catch (Throwable t) {

            throw new RuntimeException("Fatal error in unsafe reflection", t);

        }

    }

    /**
     * Get the {@link Unsafe} instance.
     *
     * @return The instance.
     */
    public Unsafe getUnsafe() {

        return unsafe;

    }

    /**
     * Opens the given list of packages from the provided module to all modules.
     *
     * This covers unnamed parent modules as well.
     *
     * @param module   The base module.
     * @param packages The packages to open.
     */
    public void open(final Module module, final List<String> packages) {

        doSafe(() -> {

            // Collect modules.
            List<Module> modules = new ArrayList<>();

            // Populate modules
            modules.add(module);
            for (ClassLoader curr = module.getClassLoader(); curr != null; curr = curr.getParent()) {

                modules.add(curr.getUnnamedModule());

            }

            // Open packages.
            for (Module m : modules) {

                for (String pkg : packages) {

                    METHOD_Module_implAddOpens.invoke(m, pkg);

                }

            }

        });

    }

    /**
     * @see UnsafeReflector#open(Module, List)
     */
    public void open(Module module, String... packages) {

        open(module, Arrays.asList(packages));

    }

    /**
     * Forcefully allow access to the given method by setting it's access modifier
     * to public.
     *
     * @param method The method.
     * @return The method.
     */
    public Method forcePublic(Method method) {

        // Get modifiers.
        int mods = method.getModifiers();

        // Negate every other access modifier.
        mods &= ~Modifier.PRIVATE;
        mods &= ~Modifier.PROTECTED;

        // Set public modifier.
        mods |= Modifier.PUBLIC;

        // Set modifiers.
        setModifiers(method, mods);

        return method;

    }

    /**
     * Forcefully allow access to the given field by setting it's access modifier to
     * public.
     *
     * @param field The field.
     * @return The field.
     */
    public Field forcePublic(Field field) {

        // Get modifiers.
        int mods = field.getModifiers();

        // Negate every other access modifier.
        mods &= ~Modifier.PRIVATE;
        mods &= ~Modifier.PROTECTED;

        // Set public modifier.
        mods |= Modifier.PUBLIC;

        // Set modifiers.
        setModifiers(field, mods);

        return field;

    }

    /**
     * Set the modifiers on the given method to your liking.
     *
     * @param method    The method.
     * @param modifiers The modifiers to set.
     */
    public void setModifiers(Method method, int modifiers) {

        doSafe(() -> {

            SETTER_Method_modifiers.invokeExact(method, modifiers);

        });

    }

    /**
     * Set the modifiers on the given field to your liking.
     *
     * @param field     The field.
     * @param modifiers The modifiers to set.
     */
    public void setModifiers(Field field, int modifiers) {

        doSafe(() -> {

            SETTER_Field_modifiers.invokeExact(field, modifiers);

        });

    }

    /**
     * Get an unsafe mirror of the given field.
     *
     * @param field The field to mirror.
     * @return The unsafe mirror.
     */
    public UnsafeField getField(Field field) {

        return new UnsafeField(this, field);

    }

    /**
     * Resolve the given field and {@link #getField(Field)}.
     */
    public UnsafeField getField(Class<?> klass, String fieldName) {

        return doSafe(() -> {

            Field field = klass.getDeclaredField(fieldName);

            return getField(field);

        });

    }

    /**
     * Resolve the given field and {@link #getField(Field)}.
     */
    public UnsafeField getField(String klass, String fieldName) {

        return doSafe(() -> {

            Field field = Class.forName(klass).getDeclaredField(fieldName);

            return getField(field);

        });

    }

    // I'm not even really sure if this is some special lookup but the field is
    // called IMPL_LOOKUP so it sounds special.
    public MethodHandles.Lookup getInternalLookup() {

        return INTERNAL_LOOKUP;

    }

}