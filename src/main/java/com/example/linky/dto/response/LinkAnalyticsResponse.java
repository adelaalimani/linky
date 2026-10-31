package com.example.linky.dto.response;

import java.util.List;

public record LinkAnalyticsResponse(String shortKey,
                                    long totalClicks,
                                    List<DailyClicksResponse> dailyClicks
) {
}
