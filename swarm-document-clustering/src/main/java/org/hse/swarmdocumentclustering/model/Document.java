package org.hse.swarmdocumentclustering.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Document {
    public final int id;

    private final List<BigDecimal> wordAppearances;

    public Document(int id, List<BigDecimal> wordAppearances) {
        this.id = id;
        this.wordAppearances = new ArrayList<>(wordAppearances);
    }

    public BigDecimal distance(Document document) {
        BigDecimal diff_square_sum = BigDecimal.ZERO;
        for (int i = 0; i < wordAppearances.size(); i++) {
            var diff = wordAppearances.get(i).subtract(document.wordAppearances.get(i));
            diff_square_sum = diff_square_sum.add(diff.pow(2));
        }
        return diff_square_sum.sqrt(new MathContext(8));
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
        wordAppearances.replaceAll(bigDecimal -> bigDecimal.divide(BigDecimal.valueOf(divisor), 8, RoundingMode.HALF_UP));
    }

    @Override
    public String toString() {
        return "Document{" + "id=" + id + ", wordAppearances=" + wordAppearances + '}';
    }
}
