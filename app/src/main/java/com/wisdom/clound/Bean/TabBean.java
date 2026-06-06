package com.wisdom.clound.Bean;

public class TabBean implements java.io.Serializable {
    private int Id;
    private String KeyName;

    public TabBean(int id, String keyName) {
        Id = id;
        KeyName = keyName;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getKeyName() {
        return KeyName;
    }

    public void setKeyName(String keyName) {
        KeyName = keyName;
    }
}