package com.momo.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SQLQueryExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SQLQueryExtractor.class);

    public String extractSQLQueryFromResponse(String intentResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(intentResponse);
            JsonNode contentNode = rootNode.path("choices").get(0).path("message").path("content");

            if (contentNode.isTextual()) {
                String content = contentNode.asText();

                // Regular expression matching for SQL queries
                Pattern sqlPattern = Pattern.compile("(SELECT).*?;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher = sqlPattern.matcher(content);

                if (matcher.find()) {
                    return matcher.group().trim();
                } else {
                    return "Error: No valid SQL query found in response.";
                }
            } else {
                return "Error: No valid SQL query found in response.";
            }
        } catch (Exception e) {
            logger.error("Error parsing intent response: ", e);
            return "Error: Failed to extract SQL query from intent response.";
        }
    }
}
