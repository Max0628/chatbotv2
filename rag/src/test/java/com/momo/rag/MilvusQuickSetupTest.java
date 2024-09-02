package com.momo.rag;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MilvusQuickSetupTest {

    private static MilvusClientV2 client;
    private static final String COLLECTION_NAME = "quick_setup";
    private static final String CLUSTER_ENDPOINT = "http://54.64.59.236:19530";

    @BeforeAll
    public static void setUp() {
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(CLUSTER_ENDPOINT)
                .build();
        client = new MilvusClientV2(connectConfig);
    }

    @Test
    public void testCreateCollectionAndCheckLoadState() throws InterruptedException {
        // Check if collection already exists
        if (!client.hasCollection(HasCollectionReq.builder().collectionName(COLLECTION_NAME).build())) {
            CreateCollectionReq quickSetupReq = CreateCollectionReq.builder()
                    .collectionName(COLLECTION_NAME)
                    .dimension(5)
                    .build();
            client.createCollection(quickSetupReq);
        } else {
            System.out.println("Collection '" + COLLECTION_NAME + "' already exists.");
        }

        // Wait for collection to load
        GetLoadStateReq quickSetupLoadStateReq = GetLoadStateReq.builder()
                .collectionName(COLLECTION_NAME)
                .build();

        int retries = 10;
        boolean isLoaded = false;
        for (int i = 0; i < retries; i++) {
            isLoaded = client.getLoadState(quickSetupLoadStateReq);
            if (isLoaded) {
                break;
            }
            Thread.sleep(1000);
        }

        // Assert that the collection is successfully loaded
        Assertions.assertTrue(isLoaded, "Collection should be loaded");

        if (isLoaded) {
            System.out.println("Collection '" + COLLECTION_NAME + "' is successfully loaded.");
        }
    }

    @AfterAll
    public static void tearDown() {
        if (client != null) {
            client.close();
        }
    }
}

