package org.hse.aco.algorithm.graph.dto;

import java.math.BigDecimal;
import java.util.List;

public record IterationResult(
        BigDecimal result,
        int skippedEdgesNumber,
        List<Integer> documentsClusterIds
) {}
