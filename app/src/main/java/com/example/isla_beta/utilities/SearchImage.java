package com.example.isla_beta.utilities;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class SearchImage {

    public static String searchImage(String query) {
        try {
            String apiKey = "AIzaSyCtShZUVar0rMmVLZxanSqzpnSOMHf4gdc";
            String searchEngineId = "d1c2823f195bd44ef";
            OkHttpClient client = new OkHttpClient();
            String url = "https://www.googleapis.com/customsearch/v1?key=" + apiKey + "&cx=" + searchEngineId + "&searchType=image&q=" + query;
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String jsonData = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("items")) {
                return jsonObject.getJSONArray("items").getJSONObject(0).getString("link");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
