package com.pdd.common.snowflake;

import java.time.Instant;

/**
 * 简化版雪花算法 ID 生成器。
 * <p>
 * 用于订单等核心数据生成分布式唯一主键。
 */
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 创建雪花 ID 生成器并校验 workerId 范围。
     */
    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个分布式唯一 ID。
     */
    public synchronized long nextId() {
        long timestamp = Instant.now().toEpochMilli();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    /**
     * 当前毫秒内序列号耗尽后，自旋等待进入下一毫秒。
     */
    private long waitNextMillis(long last) {
        long timestamp = Instant.now().toEpochMilli();
        while (timestamp <= last) {
            timestamp = Instant.now().toEpochMilli();
        }
        return timestamp;
    }
}
