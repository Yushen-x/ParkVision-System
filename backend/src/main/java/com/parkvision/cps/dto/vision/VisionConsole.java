package com.parkvision.cps.dto.vision;

import java.util.List;

public record VisionConsole(VisionStats stats, List<RecognitionRow> records) {

    public record VisionStats(
            int total,
            int today,
            int allow,
            int deny,
            int review,
            double avgConfidence
    ) {
    }
}
