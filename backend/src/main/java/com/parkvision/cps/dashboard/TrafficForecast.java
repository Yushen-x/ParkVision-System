package com.parkvision.cps.dashboard;

import java.util.List;

public record TrafficForecast(List<Integer> history, List<Integer> prediction) {
}
