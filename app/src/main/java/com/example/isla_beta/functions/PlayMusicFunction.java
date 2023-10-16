package com.example.isla_beta.functions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PlayMusicFunction {
    public static JSONObject getFunctionDefinition() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", "play_music");
            json.put("description", "play a music if user ask to play music and user give the music title/a youtube URL");

            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("title", new JSONObject().put("type", "string").put("description", "The title or URL of music"));
            properties.put("respond", new JSONObject().put("type", "string").put("description", "your response to ask the user to wait for the download result"));
            properties.put("result", new JSONObject().put("type", "string").put("description", "your response when the result is complete is sent to the user"));
            properties.put("error", new JSONObject().put("type", "string").put("description", "your response if error playing the music"));

            parameters.put("properties", properties);
            String[] requiredFields = {"title", "respond", "result", "error"};
            JSONArray requiredArray = new JSONArray(requiredFields);
            parameters.put("required", requiredArray);

            json.put("parameters", parameters);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }
}
