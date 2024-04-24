package org.hse.swarmdocumentclustering.model;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Ant {
    private final LinkedHashSet<Integer> visitedDocuments = new LinkedHashSet<>();

    private int currentDocument;

    private BigDecimal distanceTravelled = BigDecimal.ZERO;

    public Ant(int documentsNumber) {
        currentDocument = ThreadLocalRandom.current().nextInt(documentsNumber);
        visitedDocuments.add(currentDocument);
    }

    public int visitedDocumentsNumber() {
        return visitedDocuments.size();
    }

    public Set<Integer> getVisitedDocuments() {
        return visitedDocuments;
    }

    public void visit(int document, BigDecimal distance) {
        currentDocument = document;
        visitedDocuments.add(document);
        distanceTravelled = distanceTravelled.add(distance);
    }

    public int getCurrentDocument() {
        return currentDocument;
    }

    public BigDecimal getDistanceTravelled() {
        return distanceTravelled;
    }

    @Override
    public String toString() {
        return "Ant{" + "visitedDocuments=" + visitedDocuments + '}';
    }
}
