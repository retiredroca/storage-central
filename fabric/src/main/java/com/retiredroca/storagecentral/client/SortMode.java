package com.retiredroca.storagecentral.client;

public enum SortMode {
    NAME("name"),
    TYPE("type"),
    TAG("tag"),
    MOD("mod"),
    EQUIPMENT("equipment");

    private final String key;

    SortMode(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getTranslationKey() {
        return "sort.storage_central." + key;
    }
}