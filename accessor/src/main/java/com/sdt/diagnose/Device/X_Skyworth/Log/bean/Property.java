package com.sdt.diagnose.Device.X_Skyworth.Log.bean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * ClassName: Property
 *
 * <p>ClassDescription: Property
 *
 * <p>Author: ZHX Date: 2022/10/28
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
public class Property {
    private String key;
    private String value;

    public Property() {
    }

    public Property(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String formatString() {
        return key + "=" + value;
    }

    @Nullable
    public static Property fromString(String data) {
        String[] dataPair = data.split("=");
        if (dataPair.length == 2) {
            String key = dataPair[0];
            String value = dataPair[1];
            return new Property(key, value);
        }
        return null;
    }

    @Override
    @NonNull
    public String toString() {
        return "Property{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return Objects.equals(key, property.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
