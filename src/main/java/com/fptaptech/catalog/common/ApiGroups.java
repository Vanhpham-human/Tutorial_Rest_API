package com.fptaptech.catalog.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiGroups {

    String[] value();

    final class Filter {
        private Filter() {
        }

        public static boolean matches(Method method, String group) {
            ApiGroups groups = method.getAnnotation(ApiGroups.class);
            if (groups == null) {
                return false;
            }
            for (String candidate : groups.value()) {
                if (candidate.equals(group)) {
                    return true;
                }
            }
            return false;
        }
    }
}
