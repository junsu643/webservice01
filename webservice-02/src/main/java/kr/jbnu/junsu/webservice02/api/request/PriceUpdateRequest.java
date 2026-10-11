package kr.jbnu.junsu.webservice02.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PriceUpdateRequest(
        @NotNull @PositiveOrZero Integer price
) {}