package com.example.isla_beta.functions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchImageFunction {
    public static JSONObject getFunctionDefinition() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", "search_image");
            json.put("description", "return image title/description if user ask you to search an image");

            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("title", new JSONObject().put("type", "string").put("description", "The title or description of the image"));

            parameters.put("properties", properties);
            String[] requiredFields = {"title"};
            JSONArray requiredArray = new JSONArray(requiredFields);
            parameters.put("required", requiredArray);

            json.put("parameters", parameters);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }
}
