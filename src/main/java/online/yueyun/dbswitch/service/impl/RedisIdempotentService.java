package online.yueyun.dbswitch.service.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.service.IdempotentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的幂等性服务实现
 */
@Slf4j
@Service
public class RedisIdempotentService implements IdempotentService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${dbswitch.idempotent.key-prefix:dbswitch:idempotent:}")
    private String keyPrefix;

    @Value("${dbswitch.idempotent.expire-hours:24}")
    private long expireHours;

    @Override
    public boolean isProcessed(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return false;
        }
        
        String key = keyPrefix + messageId;
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    @Override
    public void markAsProcessed(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return;
        }
        
        String key = keyPrefix + messageId;
        redisTemplate.opsForValue().set(key, "1", expireHours, TimeUnit.HOURS);
        log.debug("消息[{}]已标记为已处理，过期时间{}小时", messageId, expireHours);
    }
} 