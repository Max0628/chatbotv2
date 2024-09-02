package com.momo.rag.service;

import io.milvus.exception.MilvusException;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import io.milvus.v2.service.vector.request.data.FloatVec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class MilvusSearchService {

    private MilvusClientV2 client;

    @Value("${milvus.collection.name}")
    private String collectionName;

    @Value("${milvus.cluster.endpoint}")
    private String host;

    @PostConstruct
    public void init() {
        ConnectConfig config = ConnectConfig.builder()
                .uri(host)
                .build();
        client = new MilvusClientV2(config);
        try {
            LoadCollectionReq loadReq = LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            client.loadCollection(loadReq);
        } catch (MilvusException e) {
            log.error("Failed to load Milvus collection: " + e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }

    public List<List<SearchResp.SearchResult>> searchInMilvus(float[] queryVector) {
        try {
            SearchResp searchResp = client.search(SearchReq.builder()
                    .collectionName(collectionName)
                    .annsField("vector")
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .topK(5) // Retrieve the 5 most similar results
                    .outputFields(Arrays.asList("id", "text")) // Return the specified fields
                    .build());

            return searchResp.getSearchResults();
        } catch (MilvusException e) {
            log.error("Milvus search failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
