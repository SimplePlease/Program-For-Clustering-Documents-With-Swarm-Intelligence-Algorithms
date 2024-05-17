package org.hse.aco.component;

import java.util.*;


public class ProbabilityCollection<E> {

    private final NavigableSet<ProbabilitySetElement<E>> collection;
    private final SplittableRandom random = new SplittableRandom();

    private long totalProbability;

    public ProbabilityCollection() {
        this.collection = new TreeSet<>(Comparator.comparingLong(ProbabilitySetElement::getIndex));
        this.totalProbability = 0;
    }

    public long size() {
        return this.collection.size();
    }

    public boolean isEmpty() {
        return this.collection.isEmpty();
    }

    public boolean contains(E object) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot check if null object is contained in this collection");
        }

        return this.collection.stream()
                .anyMatch(entry -> entry.getObject().equals(object));
    }

    public Iterator<ProbabilitySetElement<E>> iterator() {
        return this.collection.iterator();
    }

    public void add(E object, long probability) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot add null object");
        }

        if (probability <= 0) {
            throw new IllegalArgumentException("Probability must be greater than 0");
        }

        ProbabilitySetElement<E> entry = new ProbabilitySetElement<E>(object, probability);
        entry.setIndex(this.totalProbability + 1);

        this.collection.add(entry);
        this.totalProbability += probability;
    }

    public boolean remove(E object) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot remove null object");
        }

        Iterator<ProbabilitySetElement<E>> it = this.iterator();
        boolean removed = false;

        while (it.hasNext()) {
            ProbabilitySetElement<E> entry = it.next();
            if (entry.getObject().equals(object)) {
                this.totalProbability -= entry.getProbability();
                it.remove();
                removed = true;
            }
        }

        this.updateIndexes();

        return removed;
    }

    public void clear() {
        this.collection.clear();
        this.totalProbability = 0;
    }

    public E get() {
        if (this.isEmpty()) {
            throw new IllegalStateException("Cannot get an object out of a empty collection");
        }

        ProbabilitySetElement<E> toFind = new ProbabilitySetElement<>(null, 0);
        toFind.setIndex(this.random.nextLong(1, this.totalProbability + 1));

        return Objects.requireNonNull(this.collection.floor(toFind).getObject());
    }

    public final long getTotalProbability() {
        return this.totalProbability;
    }

    private final void updateIndexes() {
        long previousIndex = 0;

        for (ProbabilitySetElement<E> entry : this.collection) {
            previousIndex = entry.setIndex(previousIndex + 1) + (entry.getProbability() - 1);
        }
    }

    final static class ProbabilitySetElement<T> {
        private final T object;
        private final long probability;
        private long index;

        private ProbabilitySetElement(T object, long probability) {
            this.object = object;
            this.probability = probability;
        }

        public final T getObject() {
            return this.object;
        }

        public final long getProbability() {
            return this.probability;
        }

        private final long getIndex() {
            return this.index;
        }

        private final long setIndex(long index) {
            this.index = index;
            return this.index;
        }
    }
}

