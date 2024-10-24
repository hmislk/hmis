package com.divudi.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonProvider {

    public static Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Object.class, new OneLevelSerializer<>())
                .create();
    }
}
