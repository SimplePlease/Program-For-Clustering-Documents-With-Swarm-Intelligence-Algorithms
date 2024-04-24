package org.hse.swarmdocumentclustering.dto;

import java.math.BigDecimal;
import java.util.List;

public record IterationResult(
        BigDecimal result,
        int skippedEdgesNumber,
        List<Integer> documentsClusterIds
) {}
