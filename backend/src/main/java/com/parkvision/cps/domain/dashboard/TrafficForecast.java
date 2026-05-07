package com.parkvision.cps.domain.dashboard;

import java.util.List;

public record TrafficForecast(List<Integer> history, List<Integer> prediction) {
}
