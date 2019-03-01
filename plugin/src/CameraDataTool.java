package com.fbx;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class CameraDataTool {

    /**
     * json字符串转列表
     *
     * @param jsonString
     * @return List<CameraData> 转换结果
     */
    public static List<CameraData> getCardListFromJsonString(String jsonString) {
        List<CameraData> cards = new ArrayList<CameraData>();
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(jsonString).getAsJsonArray();
        for (JsonElement item : array) {
            JsonObject object = item.getAsJsonObject();
            int cameraId = object.get("cameraId").getAsInt();
            String cameraSns = object.get("cameraSns").getAsString();
            String cameraTitle = object.get("cameraTitle").getAsString();
            String snapshotUrl = object.get("snapshotUrl").getAsString();
            Boolean isOnline = object.get("online").getAsBoolean();
            int cameraNo = object.get("cameraNo").getAsInt();
            int shopId = object.get("shopId").getAsInt();
            cards.add(new CameraData(cameraId, cameraSns, cameraNo, cameraTitle, snapshotUrl, isOnline, shopId));
        }
        return cards;
    }

    /**
     * 列表装字符串
     *
     * @param cards
     * @return String 转换结果
     */
    public static String cardListToJsonString(List<CameraData> cards) {
        Gson gson = new Gson();
        return gson.toJson(cards);
    }

    /**
     * 从字符串中提取店铺数据
     *
     * @param jsonString
     * @return StoreData 转换结果
     */
    public static StoreData getStoreDataFromJsonString(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, StoreData.class);
    }
}
