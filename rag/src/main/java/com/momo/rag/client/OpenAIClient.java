package com.momo.rag.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class OpenAIClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String generateResponse(String userQuery, String retrievedData, String systemMessagePrompt) {
        // Config OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Construct system message part, using the provided prompt
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemMessagePrompt);

            // Construct user message part
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userQuery);

            // Construct assistant message part
            Map<String, Object> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", "Based on the information you provided, here are some relevant results:\n\n"
                    + retrievedData
                    + "\n\nIf this information is not comprehensive enough, please let me know your specific needs, and I will do my best to assist you further!");

            // Construct request body
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "gpt-4o-mini");
            requestBodyMap.put("messages", new Object[]{systemMessage, userMessage, assistantMessage});
            requestBodyMap.put("max_tokens", 1000);

            // Convert request body to JSON string
            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            // Create request
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            // Execute request
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    System.out.println("Response code: " + response.code());
                    System.out.println("Response message: " + response.message());
                    System.out.println("Response body: " + response.body().string());
                    return "Request failed: " + response.code();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred.";
        }
    }
}
