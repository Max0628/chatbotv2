package com.momo.rag.service;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IntentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(IntentAnalyzer.class);

    // Analyze user intent
    public boolean analyzeIntent(@NotNull String intentResponse) {
        String lowerCaseResponse = intentResponse.toLowerCase();

        // Define a set of keywords related to querying and purchasing laptops
        Set<String> queryKeywords = Set.of(
                "sql", "database", "query", "select", "fetch", "retrieve", "list", "search", "lookup"
        );

        // Define a set of domain-specific keywords (considering both singular and plural forms)
        Set<String> domainKeywords = Set.of(
                "laptop", "notebook", "computer", "macbook", "ultrabook", "chromebook", "pc"
        );

        // Expand singular keywords to include plural forms
        Set<String> expandedDomainKeywords = domainKeywords.stream()
                .flatMap(keyword -> Set.of(keyword, keyword + "s").stream())
                .collect(Collectors.toSet());

        // If no domain-specific keywords are matched, do not trigger SQL
        boolean isDomainRelevant = false;
        for (String domainKeyword : expandedDomainKeywords) {
            if (lowerCaseResponse.contains(domainKeyword)) {
                isDomainRelevant = true;
                break;
            }
        }

        // If the query is not related to the specific domain, return false
        if (!isDomainRelevant) {
            return false;
        }

        // Check if the response contains any query-related keywords
        for (String keyword : queryKeywords) {
            Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
            if (pattern.matcher(lowerCaseResponse).find()) {
                return true;
            }
        }

        return false;
    }
}
