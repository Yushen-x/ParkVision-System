package com.parkvision.cps.dto.admin;

import java.util.List;

public record AdminReport(
        String query,
        String summary,
        List<String> labels,
        List<Integer> previousWeekRevenue,
        List<Integer> currentWeekRevenue
) {
}
