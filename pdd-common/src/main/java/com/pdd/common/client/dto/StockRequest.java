package com.pdd.common.client.dto;

import java.io.Serializable;

public record StockRequest(Long activityId, Integer quantity) implements Serializable {
}
