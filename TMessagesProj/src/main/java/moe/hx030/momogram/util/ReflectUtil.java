package moe.hx030.momogram.util;


import org.h2.util.StringUtils;
import org.telegram.messenger.FileLog;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;



public class ReflectUtil {

    private static final ConcurrentHashMap<Class<?>, HashSet<Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    public static HashSet<Field> getFieldsDirectly(Class<?> beanClass, boolean withSuperClassFields) throws SecurityException {
        assert (null != beanClass);

        HashSet<Field> allFields = new HashSet<>();
        Class<?> searchType = beanClass;
        Field[] declaredFields;
        while (searchType != null) {
            declaredFields = searchType.getDeclaredFields();
            allFields.addAll(Arrays.asList(declaredFields));
            searchType = withSuperClassFields ? searchType.getSuperclass() : null;
        }

        return allFields;
    }

    public static HashSet<Field> getFields(Class<?> beanClass) throws SecurityException {
        assert (beanClass != null);
        return FIELDS_CACHE.computeIfAbsent(beanClass, __ -> getFieldsDirectly(beanClass, true));
    }

    public static Field getField(Class<?> beanClass, String name) throws SecurityException {
        final HashSet<Field> fields = getFields(beanClass);
        Optional<Field> ret = fields.stream().filter(x -> name.equals(x.getName())).findFirst();
        return ret.isPresent() ? ret.get() : null;
    }

    public static boolean hasField(Class<?> beanClass, String name) throws SecurityException {
        return null != getField(beanClass, name);
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (null == obj || StringUtils.isNullOrEmpty(fieldName)) {
            return null;
        }
        Field field = getField(obj instanceof Class ? (Class<?>) obj : obj.getClass(), fieldName);
        field.setAccessible(true);
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            FileLog.e(e);
            return null;
        }
    }
}
