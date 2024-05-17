package org.hse.aco.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Document {
    public final int id;
    public String name;
    public final String clusterName;
    private final List<BigDecimal> wordAppearances;

    public Document(int id,String name, String clusterName, List<BigDecimal> wordAppearances) {
        this.id = id;
        this.name = name;
        this.clusterName = clusterName;
        this.wordAppearances = new ArrayList<>(wordAppearances);
    }

    public BigDecimal distance(Document document) {
        BigDecimal diff_square_sum = BigDecimal.ZERO;
        for (int i = 0; i < getAlphabetSize(); i++) {
            var diff = wordAppearances.get(i).subtract(document.wordAppearances.get(i));
            diff_square_sum = diff_square_sum.add(diff.pow(2));
        }
        return diff_square_sum.sqrt(new MathContext(128));
    }

    public BigDecimal cosine(Document document) {
        BigDecimal productsSum = BigDecimal.ZERO;
        BigDecimal words1Squared = BigDecimal.ZERO;
        BigDecimal words2Squared = BigDecimal.ZERO;
        for (int i = 0; i < getAlphabetSize(); i++) {
            var a = wordAppearances.get(i);
            var b = document.wordAppearances.get(i);
            productsSum = productsSum.add(a.multiply(b));
            words1Squared = words1Squared.add(a.pow(2));
            words2Squared = words2Squared.add(b.pow(2));
        }
        words1Squared = words1Squared.sqrt(new MathContext(128));
        words2Squared = words2Squared.sqrt(new MathContext(128));
        return productsSum.divide(words1Squared, 128, RoundingMode.HALF_UP).divide(words2Squared, 128, RoundingMode.HALF_UP).add(BigDecimal.ONE);
    }

    public int getAlphabetSize() {
        return wordAppearances.size();
    }

    public void addWordAppearances(Document document) {
        for (int i = 0; i < wordAppearances.size(); i++) {
            var appearance = wordAppearances.get(i);
            wordAppearances.set(i, appearance.add(document.wordAppearances.get(i)));
        }
    }

    public void divWordAppearances(int divisor) {
        wordAppearances.replaceAll(bigDecimal -> bigDecimal.divide(BigDecimal.valueOf(divisor), 128, RoundingMode.HALF_UP));
    }

    @Override
    public String toString() {
        return "Document{" + "id=" + id + ", name=" + name + ", clusterName=" + clusterName + ", wordAppearances=" + wordAppearances + '}';
    }
}
