package com.momo.rag.controller;

import com.momo.rag.client.OpenAIClient;
import com.momo.rag.client.TextClient;
import com.momo.rag.dto.ConversationResponse;
import com.momo.rag.dto.TextRequest;
import com.momo.rag.repository.SQLRepository;
import com.momo.rag.service.EmbeddingParser;
import com.momo.rag.service.IntentAnalyzer;
import com.momo.rag.service.MilvusSearchService;
import com.momo.rag.service.SQLQueryExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/1.0")
public class OpenAIController {

    @Value("${chatbot.userIntentionSM}")
    private String userIntentionSM;

    @Value("${chatbot.sqlSM}")
    private String sqlSM;

    @Value("${chatbot.nonSqlSM}")
    private String nonSqlSM;

    @Value("${chatbot.maxTokens}")
    private int maxTokens;

    private static final Logger logger = LoggerFactory.getLogger(OpenAIController.class);

    @Autowired
    private TextClient textClient;

    @Autowired
    private MilvusSearchService milvusSearchService;

    @Autowired
    private OpenAIClient openAIClient;

    @Autowired
    private SQLRepository sqlRepository;

    @Autowired
    private IntentAnalyzer intentAnalyzer;

    @Autowired
    private EmbeddingParser embeddingParser;

    @Autowired
    private SQLQueryExtractor sqlQueryExtractor;

    @PostMapping("/chat")
    public ResponseEntity<?> processText(@RequestBody TextRequest textRequest) {
        try {
            String conversationId = textRequest.getConversationId();
            if (conversationId == null || conversationId.isEmpty()) {
                conversationId = UUID.randomUUID().toString();
            }

            // 1. Retrieve conversation history
            List<String> conversationHistory = sqlRepository.getConversationHistory(conversationId);

            // 2. Build prompt
            StringBuilder promptBuilder = new StringBuilder();
            for (String historyEntry : conversationHistory) {
                if (historyEntry.startsWith("{")) {
                    // This is the AI's response, parse the content and add it to the prompt
                    JsonNode historyJson = new ObjectMapper().readTree(historyEntry);
                    String aiContent = historyJson.path("choices").get(0).path("message").path("content").asText();
                    promptBuilder.append("Assistant: ").append(aiContent).append("\n");
                } else {
                    // This is the user's input
                    promptBuilder.append("User: ").append(historyEntry).append("\n");
                }
            }
            promptBuilder.append("User: ").append(textRequest.getText()).append("\nAssistant:");
            String prompt = promptBuilder.toString();
            logger.info("Generated prompt: \n{}", prompt);

            // 3. Use 4o-mini to analyze the user's text intent
            String intentResponse = openAIClient.generateResponse(prompt, "", userIntentionSM);
            logger.info("intent: {}", intentResponse);
            boolean isSqlAppropriate = intentAnalyzer.analyzeIntent(intentResponse);

            String aiResponse;
            if (isSqlAppropriate) {
                logger.info("User Input: {}", textRequest.getText());

                // 4. Parse intentResponse to extract SQL query
                String sqlQuery =  sqlQueryExtractor.extractSQLQueryFromResponse(intentResponse);
                logger.info("Executing SQL Query: {}", sqlQuery);

                // 5. Execute SQL query and handle potential errors
                String sqlResult = sqlRepository.executeSQLQuery(sqlQuery);

                // 6. Combine SQL result with user input and pass it to 4o-mini
                String combinedInput = prompt + "\n" + textRequest.getText() + "\n" + sqlResult;

                // Limit the character count of combinedInput
                combinedInput = limitCombinedInput(combinedInput, maxTokens);

                logger.info("Combined Input: {}", combinedInput);
                aiResponse = openAIClient.generateResponse(combinedInput, "", sqlSM);
            } else {
                // 4. Use TextClient to generate a vector representation of the text and perform similarity search in Milvus
                String embeddingResponse = textClient.generateResponse(textRequest.getText(), "");
                List<Float> vector = embeddingParser.parseEmbeddingFromResponse(embeddingResponse);

                float[] queryVector = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    queryVector[i] = vector.get(i);
                }
                List<List<SearchResp.SearchResult>> searchResults = milvusSearchService.searchInMilvus(queryVector);

                // 5. Process the search results and generate context
                String context = searchResults.stream()
                        .flatMap(List::stream)
                        .map(result -> {
                            String id = result.getEntity().get("id").toString();
                            String text = result.getEntity().get("text") != null ? result.getEntity().get("text").toString() : "";
                            return "ID: " + id + " - " + text;
                        })
                        .collect(Collectors.joining(" "));

                logger.info("User Input: {}", textRequest.getText());
                logger.info("TopK Results: {}", context);

                // 6. Combine user's input with Milvus search topK results and pass it to 4o-mini
                String combinedInput = prompt + "\n" + textRequest.getText();
                logger.info("Combined Input: {}\nTopK Results: {}", combinedInput, context);
                aiResponse = openAIClient.generateResponse(combinedInput, context, nonSqlSM);
            }

            // 7. Save conversation history
            sqlRepository.saveConversation(conversationId, "user", textRequest.getText());
            sqlRepository.saveConversation(conversationId, "assistant", aiResponse);

            // 8. Return the final chat result with conversationId
            return ResponseEntity.ok(new ConversationResponse(conversationId, aiResponse));

        } catch (IOException | InterruptedException e) {
            logger.error("Error processing text: ", e);
            return ResponseEntity.status(500).body("Error processing text: " + e.getMessage());
        }
    }

    // Method to limit the character count of the combined input
    private String limitCombinedInput(String combinedInput, int maxChars) {
        if (combinedInput.length() > maxChars) {
            return combinedInput.substring(0, maxChars);
        }
        return combinedInput;
    }
}
