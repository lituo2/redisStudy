package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisldWorker {
    private static final long BEGIN_TIMESTAMP = 1640995200l;
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisldWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowsecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowsecond - BEGIN_TIMESTAMP;

        //生成序列号
        //获取当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);


        return timestamp << COUNT_BITS | count;
    }
}
