package com.pdd.lottery.dto;

import java.io.Serializable;

public record PrizeSlice(
        String code,
        String name,
        String level,
        Integer weight,
        Integer stock,
        Boolean winning
) implements Serializable {
}
