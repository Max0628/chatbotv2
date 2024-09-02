package com.momo.rag.db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.util.Objects;

@SpringBootApplication
@ComponentScan(basePackages = {"com.momo.rag.db", "com.momo.rag.client"})
public class ImportApplication {
    public static void main(String[] args) {
        // Start the Spring application context
        ApplicationContext context = SpringApplication.run(ImportApplication.class, args);

        // Retrieve the MilvusDataImporter bean from the context
        MilvusDataImporter milvusDataImporter = context.getBean(MilvusDataImporter.class);

        // Use the correct path depending on where your CSV file is stored
        String csvFilePath = Objects.requireNonNull(ImportApplication.class.getClassLoader().getResource("laptop_data_text.csv")).getPath();

        try {
            // Call the importData method with the CSV file path
            milvusDataImporter.importData(csvFilePath);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }
}
