package com.cometchat.chat.models;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/** Shared clone/contentEquals for the plain data holders. */
public abstract class ModelBase implements Cloneable {
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /** Field-by-field equality (the UI uses it to skip redundant redraws). */
    public boolean contentEquals(Object other) {
        if (this == other) return true;
        if (other == null || other.getClass() != getClass()) return false;
        for (Class<?> c = getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    if (!Objects.equals(f.get(this), f.get(other))) return false;
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        return true;
    }
}
