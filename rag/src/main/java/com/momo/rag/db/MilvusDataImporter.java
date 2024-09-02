package com.momo.rag.db;

import com.momo.rag.client.TextClient;
import com.google.gson.*;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.InsertResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MilvusDataImporter {

    private static final Logger log = LoggerFactory.getLogger(MilvusDataImporter.class);

    @Autowired
    private TextClient textClient;

    @Value("${milvus.collection.name}")
    private String collectionName;

    @Value("${milvus.cluster.endpoint}")
    private String clusterEndpoint;

    private MilvusClientV2 client;

    public void importData(String csvFilePath) throws InterruptedException, IOException {
        log.info("Starting data import process...");

        // 1. Connect to Milvus server
        log.info("Connecting to Milvus server at {}", clusterEndpoint);
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(clusterEndpoint)
                .build();
        client = new MilvusClientV2(connectConfig);

        // 2. Create Milvus collection in a customized setup mode
        log.info("Creating Milvus collection '{}'", collectionName);

        // 2.1 Create schema
        CreateCollectionReq.CollectionSchema schema = client.createSchema();

        // 2.2 Add fields to schema
        log.info("Adding fields to collection schema");
        schema.addField(AddFieldReq.builder().fieldName("id").dataType(DataType.Int64).isPrimaryKey(true).autoID(false).build());
        schema.addField(AddFieldReq.builder().fieldName("vector").dataType(DataType.FloatVector).dimension(1536).build());
        schema.addField(AddFieldReq.builder().fieldName("text").dataType(DataType.VarChar).maxLength(1000).build());

        // 2.3 Prepare index parameters
        log.info("Preparing index parameters");
        IndexParam indexParamForIdField = IndexParam.builder()
                .fieldName("id")
                .indexType(IndexParam.IndexType.STL_SORT)
                .build();

        IndexParam indexParamForVectorField = IndexParam.builder()
                .fieldName("vector")
                .indexType(IndexParam.IndexType.IVF_FLAT)
                .metricType(IndexParam.MetricType.IP)
                .extraParams(Map.of("nlist", 1024))
                .build();

        List<IndexParam> indexParams = new ArrayList<>();
        indexParams.add(indexParamForIdField);
        indexParams.add(indexParamForVectorField);

        // 2.4 Create a collection with schema and index parameters
        log.info("Creating collection with schema and index parameters");
        CreateCollectionReq customizedSetupReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .enableDynamicField(true)
                .build();

        client.createCollection(customizedSetupReq);

        // Wait for the collection to be created
        Thread.sleep(5000);

        // 2.5 Get load state of the collection
        log.info("Checking load state of the collection '{}'", collectionName);
        GetLoadStateReq customSetupLoadStateReq1 = GetLoadStateReq.builder()
                .collectionName(collectionName)
                .build();

        boolean isLoaded = client.getLoadState(customSetupLoadStateReq1);

        log.info("Collection Load State: {}", isLoaded);

        // 3. Read and process CSV data
        log.info("Reading and processing CSV data from '{}'", csvFilePath);
        List<String[]> data = readCSV(csvFilePath);

        Gson gson = new Gson(); // Instantiate Gson

        // 4. Feature processing and vectorization using TextClient
        log.info("Starting feature processing and vectorization");
        List<List<Float>> vectors = data.stream()
                .map(this::processDataRow)
                .collect(Collectors.toList());
        List<Long> ids = generateIds(data.size());

        // 5. Insert data into Milvus
        log.info("Inserting data into Milvus collection '{}'", collectionName);
        List<JsonObject> insertData = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", ids.get(i));
            obj.add("vector", gson.toJsonTree(vectors.get(i)));  // Converting list to JSON
            obj.addProperty("text", String.join(" ", Arrays.copyOfRange(data.get(i), 1, 13)));
            insertData.add(obj);
        }

        InsertReq insertReq = InsertReq.builder()
                .collectionName(collectionName)
                .data(insertData)
                .build();

        InsertResp insertResp = client.insert(insertReq);

        log.info("Insert Response: {}", insertResp);

        // Close the client connection
        client.close();
        log.info("Data import process completed.");
    }

    // Helper methods

    private List<String[]> readCSV(String filePath) {
        log.info("Reading CSV file from '{}'", filePath);
        List<String[]> data = new ArrayList<>();
        try {
            // Read all lines from the CSV file
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            // Parse each line and split by comma, then add to the list
            data = lines.stream()
                    .map(line -> line.split(","))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage(), e);
        }

        return data;
    }

    private List<Long> generateIds(int size) {
        log.info("Generating {} unique IDs", size);
        List<Long> ids = new ArrayList<>();
        for (long i = 0; i < size; i++) {
            ids.add(i);
        }
        return ids;
    }

    // Process each row and convert it into an embedding using TextClient
    private List<Float> processDataRow(String[] row) {
        log.info("Processing row: {}", Arrays.toString(row));
        String modelText = String.join(" ", Arrays.copyOfRange(row, 1, 13));

        try {
            log.info("Generating embedding for text: '{}'", modelText);
            String embeddingResponse = textClient.generateResponse(modelText, "");
            return parseEmbeddingFromResponse(embeddingResponse);
        } catch (IOException | InterruptedException e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return Arrays.asList(new Float[768]); // Return a default vector in case of failure
        }
    }

    private List<Float> parseEmbeddingFromResponse(String response) {
        log.info("Parsing embedding from response");
        List<Float> embedding = new ArrayList<>();

        // Parse the JSON response
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

        // Navigate to the "embedding" array
        JsonArray dataArray = jsonObject.getAsJsonArray("data");
        if (dataArray != null && dataArray.size() > 0) {
            JsonObject dataObject = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = dataObject.getAsJsonArray("embedding");

            // Convert each element in the embedding array to a float and add it to the list
            for (JsonElement element : embeddingArray) {
                embedding.add(element.getAsFloat());
            }
        }

        log.info("Embedding parsed successfully");
        return embedding;
    }
}
