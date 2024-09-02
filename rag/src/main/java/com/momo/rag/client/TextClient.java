package com.momo.rag.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class TextClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url.em}")
    private String apiUrl;

    public String generateResponse(String text, String retrievedData) throws IOException, InterruptedException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        // Construct request body
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", "text-embedding-3-small");
        requestBodyMap.put("input", text);

        String requestBody = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        int retryCount = 0;
        int maxRetries = 3; // Maximum number of retries
        int retryDelay = 2000; // Retry delay in milliseconds

        while (retryCount < maxRetries) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else if (response.code() == 429) { // Handle 429 Too Many Requests error
                    System.out.println("Received 429 Too Many Requests. Retrying after delay...");
                    retryCount++;
                    Thread.sleep(retryDelay);
                } else {
                    throw new IOException("Unexpected code " + response);
                }
            }
        }
        throw new IOException("Failed after " + maxRetries + " retries.");
    }
}
