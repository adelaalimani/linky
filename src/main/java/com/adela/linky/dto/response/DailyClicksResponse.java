package com.adela.linky.dto.response;

import java.time.LocalDate;

public record DailyClicksResponse(LocalDate statDate,
                                  long clicks
) {
}
