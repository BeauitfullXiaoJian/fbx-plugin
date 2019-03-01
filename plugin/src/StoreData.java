package com.fbx;

import java.io.Serializable;

public class StoreData implements Serializable {

    private String storeTitle;
    private int storeId;

    public StoreData(int storeId, String storeTitle) {
        this.storeId = storeId;
        this.storeTitle = storeTitle;
    }

    public String getStoreTitle() {
        return storeTitle;
    }

    public void setStoreTitle(String storeTitle) {
        this.storeTitle = storeTitle;
    }

    public int getStoreId() {
        return storeId;
    }

    public void setStoreId(int storeId) {
        this.storeId = storeId;
    }
}
