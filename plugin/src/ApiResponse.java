package com.fbx;

import android.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;


public class ApiResponse {
    private boolean result;
    private String message;
    private JsonElement data;

    public static JsonObject getAuthHeader(String tokenPackage) {
        String jsonString = new String(Base64.decode(tokenPackage, Base64.DEFAULT));
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(jsonString);
        return jsonElement.getAsJsonObject();
    }

    public ApiResponse(String responseString) {
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(responseString);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        try {
            result = jsonObject.get("result").getAsBoolean();
            message = jsonObject.get("message").getAsString();
            data = jsonObject.get("datas");
        } catch (Exception e) {
            e.printStackTrace();
            message = e.toString();
        }
    }

    public boolean getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }
}
