package com.momo.rag.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingParser {

    public List<Float> parseEmbeddingFromResponse(String response) {
        List<Float> embedding = new ArrayList<>();

        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        JsonArray dataArray = jsonObject.getAsJsonArray("data");

        if (dataArray != null && dataArray.size() > 0) {
            JsonObject dataObject = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = dataObject.getAsJsonArray("embedding");

            for (JsonElement element : embeddingArray) {
                embedding.add(element.getAsFloat());
            }
        }

        return embedding;
    }
}
