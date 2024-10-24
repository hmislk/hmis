package com.divudi.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class OneLevelSerializer<T> implements JsonSerializer<T> {

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        Class<?> srcClass = src.getClass();

        // Serialize only the direct fields of the object
        for (Field field : srcClass.getDeclaredFields()) {
            try {
                field.setAccessible(true); // Make private fields accessible
                Object value = field.get(src);

                if (value != null && !isNestedObject(value)) {
                    jsonObject.add(field.getName(), context.serialize(value));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    // Helper method to detect nested objects
    private boolean isNestedObject(Object value) {
        return !(value instanceof Number || value instanceof String || value instanceof Boolean ||
                 value instanceof Enum || value instanceof Character);
    }
}
