package org.hse.aco.algorithm.fuzzy.dto;

import org.hse.aco.model.DistanceFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.hse.aco.controller.StartAlgorithmController.DISTANCE_FUNCTION;

public class IterationSolutions {
    private final List<Solution> solutions = new ArrayList<>();

    public void addSolution(Solution solution) {
        solutions.add(solution);
    }

    public Solution getBestSolution() {
        var bestSolution = new Solution(null, null);
        for (var solution : solutions) {
            if (solution.betterThan(bestSolution)) {
                bestSolution = solution;
            }
        }
        return bestSolution;
    }

    public static class Solution {
        public BigDecimal fitness;
        public final int[] assignedClusters;

        public Solution(BigDecimal fitness, int[] assignedClusters) {
            this.fitness = fitness;
            this.assignedClusters = assignedClusters;
        }

        public boolean betterThan(Solution other) {
            if (fitness == null) {
                return false;
            }
            if (other.fitness == null) {
                return true;
            }
            if (DISTANCE_FUNCTION == DistanceFunction.COSINE) {
                return fitness.compareTo(other.fitness) > 0;
            }
            return fitness.compareTo(other.fitness) < 0;
        }
    }
}
