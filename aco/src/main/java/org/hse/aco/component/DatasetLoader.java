package org.hse.aco.component;

import com.opencsv.CSVReader;
import org.hse.aco.model.Document;
import org.tinylog.Logger;

import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatasetLoader {
    public List<Document> loadDataset() throws Exception {
        var fileName = "org/hse/aco/dataset/svd_TFIDF2by50.csv";
        Path path = Paths.get(ClassLoader.getSystemResource(fileName).toURI());
        var documentId = -1;
        List<Document> documents = new ArrayList<>();
        var csvLines = readAllCsvLines(path);

        Logger.info("Reading dataset from file \"" + fileName + "\". It contains " + (csvLines.size() - 1) + " documents to cluster");
        for (var document : csvLines) {
            if (documentId >= 0) {
                var clusterName = document[document.length - 1];
                var wordAppearances = Arrays.copyOfRange(document, 1, document.length - 1);
                documents.add(
                        new Document(documentId, document[0], clusterName, Arrays.stream(wordAppearances).map(BigDecimal::new).toList())
                );
            }
            documentId++;
        }

        documents.addAll(List.of(
//                new Document(0, "doc0", "cluster1", List.of(BigDecimal.valueOf(1))),
//                new Document(1, "doc1", "cluster1", List.of(BigDecimal.valueOf(2))),
//                new Document(2, "doc2", "cluster2", List.of(BigDecimal.valueOf(1000))),
//                new Document(3, "doc3", "cluster2", List.of(BigDecimal.valueOf(1001)))

//                new Document(0, "doc0", "cluster1", List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(1))),
//                new Document(1, "doc1", "cluster1", List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2))),
//                new Document(2, "doc2", "cluster2", List.of(BigDecimal.valueOf(-1), BigDecimal.valueOf(-1))),
//                new Document(3, "doc3", "cluster2", List.of(BigDecimal.valueOf(-1), BigDecimal.valueOf(-3)))
        ));

        for (Document document : documents) {
            Logger.debug("Document is read: " + document);
        }
        return documents;
    }

    private List<String[]> readAllCsvLines(Path filePath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                return csvReader.readAll();
            }
        }
    }
}
