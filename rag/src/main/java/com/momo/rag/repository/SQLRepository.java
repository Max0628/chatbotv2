package com.momo.rag.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;

@Repository
public class SQLRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Use JdbcTemplate to execute SQL queries
    public String executeSQLQuery(String sqlQuery) {
        try {
            List<String> results = jdbcTemplate.query(sqlQuery, (rs, rowNum) -> {
                StringJoiner row = new StringJoiner(", ");
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    String columnValue = rs.getString(i);
                    row.add(columnName + ": " + columnValue);
                }
                return row.toString();
            });

            return String.join("\n", results);

        } catch (Exception e) {
            // Catch the exception and return the error message without throwing the exception
            return "Error executing SQL query: " + e.getMessage();
        }
    }

    // Save conversation history
    public void saveConversation(String conversationId, String role, String content) {
        String sql = "INSERT INTO chat_history (conversation_id, role, content) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, conversationId, role, content);
    }

    // Retrieve conversation history
    public List<String> getConversationHistory(String conversationId) {
        String sql = "SELECT content FROM chat_history WHERE conversation_id = ? ORDER BY timestamp";
        return jdbcTemplate.queryForList(sql, new Object[]{conversationId}, String.class);
    }
}
