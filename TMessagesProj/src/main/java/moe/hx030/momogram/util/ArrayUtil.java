package moe.hx030.momogram.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;


// mostly from hutool with minor changes
public class ArrayUtil {

    public static <T> boolean contains(T[] array, T value) {
        return array != null &&
                Arrays.stream(array).anyMatch(e -> Objects.equals(e, value));
    }

    @SuppressWarnings({"unchecked", "SuspiciousSystemArraycopy"})
    public static <T> Object insert(Object[] array, int index, T... newElements) {
        if (newElements.length == 0) {
            return array;
        }
        if (null == array || !array.getClass().isArray()) {
            return newElements;
        }

        final int len = getLength(array);
        if (index < 0) {
            index = (index % len) + len;
        }

        final Class<?> originComponentType = array.getClass().getComponentType();
        Object newEleArr = newElements;
//        if (originComponentType.isPrimitive()) {
//            newEleArr = convertWithCheck(array.getClass(), newElements, null, true);
//        }
        final Object result = Array.newInstance(originComponentType, Math.max(len, index) + newElements.length);
        System.arraycopy(array, 0, result, 0, Math.min(len, index));
        System.arraycopy(newEleArr, 0, result, index, newElements.length);
        if (index < len) {
            System.arraycopy(array, index, result, index + newElements.length, len - index);
        }
        return result;
    }

    public static int getLength(Object array)
        /* throws IllegalArgumentException */ {
        if (array instanceof Object[]) {
            return ((Object[]) array).length;
        } else if (array instanceof boolean[]) {
            return ((boolean[]) array).length;
        } else if (array instanceof byte[]) {
            return ((byte[]) array).length;
        } else if (array instanceof char[]) {
            return ((char[]) array).length;
        } else if (array instanceof double[]) {
            return ((double[]) array).length;
        } else if (array instanceof float[]) {
            return ((float[]) array).length;
        } else if (array instanceof int[]) {
            return ((int[]) array).length;
        } else if (array instanceof long[]) {
            return ((long[]) array).length;
        } else if (array instanceof short[]) {
            return ((short[]) array).length;
        }
        throw new IllegalArgumentException(String.format("unexpected type %s", array.getClass().getName()));
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object remove(Object[] array, int index) throws IllegalArgumentException {
        if (null == array) {
            return null;
        }
        int length = array.length;
        if (index < 0 || index >= length) {
            return array;
        }

        final Object result = Array.newInstance(array[0].getClass(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) {
            // 后半部分
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    public static <T> T[] sub(T[] array, int start, int end) {
        return Arrays.copyOfRange(
                array,
                Math.max(0, Math.min(array.length, start < 0 ? start + array.length : start)),
                Math.max(0, Math.min(array.length, end < 0 ? end + array.length : end))
        );
    }
}
