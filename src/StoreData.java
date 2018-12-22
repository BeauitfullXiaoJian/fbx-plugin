package com.fbx;

import java.io.Serializable;

public class StoreData implements Serializable {

    private String storeTitle;
    private int storeId;
    private int companyId;

    public StoreData(int storeId, String storeTitle) {
        this.storeId = storeId;
        this.storeTitle = storeTitle;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
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
