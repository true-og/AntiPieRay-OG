package net.trueog.antipierayog.reflect;

import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_BYTE;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_CHAR;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_DOUBLE;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_FLOAT;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_INT;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_LONG;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_OBJ;
import static net.trueog.antipierayog.reflect.UnsafeTypes.PT_SHORT;
import static net.trueog.antipierayog.reflect.UnsafeTypes.getPrimitiveType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;

public class UnsafeField {

    static final Unsafe unsafe = UnsafeUtil.getUnsafe();

    // Declare unsafe reflector.
    final UnsafeReflector reflector;

    // Declare reflection field.
    final Field field;

    // Declare the field base and offset.
    final boolean isStatic;
    // null if not static.
    final Object staticBase;
    final long offset;

    // Declare primitive type.
    final byte primitiveType;

    // Constructor.
    UnsafeField(UnsafeReflector reflector, Field field) {

        this.reflector = reflector;
        this.field = field;

        // Calculate properties.
        Unsafe unsafe = reflector.getUnsafe();
        this.isStatic = 0 < (field.getModifiers() & Modifier.STATIC);
        if (isStatic) {

            staticBase = unsafe.staticFieldBase(field);

            offset = unsafe.staticFieldOffset(field);

        } else {

            staticBase = null;

            offset = unsafe.objectFieldOffset(field);

        }

        this.primitiveType = getPrimitiveType(field.getType());

    }

    /**
     * Get the reflection field.
     *
     * @return The field.
     */
    public Field getField() {

        return field;

    }

    /**
     * Check if this field is static.
     */
    public boolean isStatic() {

        return isStatic;

    }

    /**
     * Get the offset of this field. Either static or object based on
     * {@link #isStatic()}
     */
    public long getOffset() {

        return offset;

    }

    /**
     * Get the static base object, null if not static.
     */
    public Object getStaticBase() {

        return staticBase;

    }

    /**
     * Get the static base of this field if it is static, or else return the given
     * instance.
     *
     * @param base The instance base.
     * @return The base.
     */
    public Object getBase(Object base) {

        if (isStatic) {

            return staticBase;

        }

        return base;

    }

    /* Functionality */

    /**
     * Set the modifiers of this field.
     *
     * @param modifiers The modifiers to set.
     * @return This.
     */
    public UnsafeField setModifiers(int modifiers) {

        reflector.setModifiers(field, modifiers);

        return this;

    }

    public UnsafeField enableModifiers(int mask) {

        return setModifiers(field.getModifiers() | mask);

    }

    public UnsafeField disableModifiers(int mask) {

        return setModifiers(field.getModifiers() & ~mask);

    }

    /**
     * Forces this field to be accessible.
     *
     * @return This.
     */
    public UnsafeField forceAccess() {

        // Open declaring class.
        reflector.open(field.getDeclaringClass().getModule(), field.getDeclaringClass().getPackageName());

        // Force modifier access.
        reflector.forcePublic(field);

        // Set accessible.
        field.setAccessible(true);

        return this;

    }

    /**
     * Get the value of the field, returning a boxed primitive if the field is of a
     * primitive type.
     *
     * @param base1 The instance base (can be null if static).
     * @return The result value.
     */
    public Object get(Object base1) {

        Object base = getBase(base1);
        return switch (primitiveType) {
        // Primitives.
        case PT_BYTE -> unsafe.getByte(base, offset);
        case PT_SHORT -> unsafe.getShort(base, offset);
        case PT_CHAR -> unsafe.getChar(base, offset);
        case PT_INT -> unsafe.getInt(base, offset);
        case PT_LONG -> unsafe.getLong(base, offset);
        case PT_FLOAT -> unsafe.getFloat(base, offset);
        case PT_DOUBLE -> unsafe.getDouble(base, offset);
        // Object.
        case PT_OBJ -> unsafe.getObject(base, offset);
        // Unknown.
        default -> throw new AssertionError("Unknown primitive type for " + field + ": " + primitiveType);
        };

    }

    /**
     * Get the value of the field, returning a boxed primitive if the field is of a
     * primitive type.
     *
     * This operation is volatile.
     *
     * @param base1 The instance base (can be null if static).
     * @return The result value.
     */
    public Object getVolatile(Object base1) {

        Object base = getBase(base1);
        return switch (primitiveType) {

        // Primitives.
        case PT_BYTE -> unsafe.getByteVolatile(base, offset);
        case PT_SHORT -> unsafe.getShortVolatile(base, offset);
        case PT_CHAR -> unsafe.getCharVolatile(base, offset);
        case PT_INT -> unsafe.getIntVolatile(base, offset);
        case PT_LONG -> unsafe.getLongVolatile(base, offset);
        case PT_FLOAT -> unsafe.getFloatVolatile(base, offset);
        case PT_DOUBLE -> unsafe.getDoubleVolatile(base, offset);
        // Object.
        case PT_OBJ -> unsafe.getObjectVolatile(base, offset);
        // Unknown.
        default -> throw new AssertionError("Unknown primitive type for " + field + ": " + primitiveType);
        };

    }

    /**
     * Set the value of this field to the given value on the provided base, or the
     * static base if static.
     *
     * @param base1 The instance to set on.
     * @param value The value to set.
     * @return This.
     */
    public UnsafeField set(Object base1, Object value) {

        Object base = getBase(base1);
        switch (primitiveType) {
        // Primitives.
        case PT_BYTE -> unsafe.putByte(base, offset, (Byte) value);
        case PT_SHORT -> unsafe.putShort(base, offset, (Short) value);
        case PT_CHAR -> unsafe.putChar(base, offset, (Character) value);
        case PT_INT -> unsafe.putInt(base, offset, (Integer) value);
        case PT_LONG -> unsafe.putLong(base, offset, (Long) value);
        case PT_FLOAT -> unsafe.putFloat(base, offset, (Float) value);
        case PT_DOUBLE -> unsafe.putDouble(base, offset, (Double) value);
        // Object.
        case PT_OBJ -> unsafe.putObject(base, offset, value);
        // Unknown.
        default -> throw new AssertionError("Unknown primitive type for " + field + ": " + primitiveType);
        }
        ;

        return this;

    }

    /**
     * Set the value of this field to the given value on the provided base, or the
     * static base if static.
     *
     * This operation is volatile.
     *
     * @param base1 The instance to set on.
     * @param value The value to set.
     * @return This.
     */
    public UnsafeField setVolatile(Object base1, Object value) {

        Object base = getBase(base1);
        switch (primitiveType) {
        // Primitives.
        case PT_BYTE -> unsafe.putByteVolatile(base, offset, (Byte) value);
        case PT_SHORT -> unsafe.putShortVolatile(base, offset, (Short) value);
        case PT_CHAR -> unsafe.putCharVolatile(base, offset, (Character) value);
        case PT_INT -> unsafe.putIntVolatile(base, offset, (Integer) value);
        case PT_LONG -> unsafe.putLongVolatile(base, offset, (Long) value);
        case PT_FLOAT -> unsafe.putFloatVolatile(base, offset, (Float) value);
        case PT_DOUBLE -> unsafe.putDoubleVolatile(base, offset, (Double) value);
        // Object.
        case PT_OBJ -> unsafe.putObjectVolatile(base, offset, value);
        // Unknown.
        default -> throw new AssertionError("Unknown primitive type for " + field + ": " + primitiveType);
        }
        ;

        return this;

    }

}