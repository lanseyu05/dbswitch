package online.yueyun.dbswitch.service.impl;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.enums.WriteMode;
import online.yueyun.dbswitch.service.WriteModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基于Redis的写入模式服务实现
 */
@Slf4j
@Service
public class RedisWriteModeService implements WriteModeService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${dbswitch.write-mode.redis-key:dbswitch:write-mode}")
    private String writeModeKey;

    @Value("${dbswitch.write-mode.default:MASTER_ONLY}")
    private String defaultWriteMode;

    @Override
    public WriteMode getCurrentWriteMode() {
        try {
            String modeValue = redisTemplate.opsForValue().get(writeModeKey);
            if (modeValue == null || modeValue.isEmpty()) {
                log.debug("未设置写入模式，使用默认模式: {}", defaultWriteMode);
                return WriteMode.valueOf(defaultWriteMode);
            }
            
            log.debug("当前写入模式: {}", modeValue);
            return WriteMode.valueOf(modeValue);
        } catch (Exception e) {
            log.error("获取写入模式异常，使用默认模式: {}", defaultWriteMode, e);
            return WriteMode.valueOf(defaultWriteMode);
        }
    }

    @Override
    public void updateWriteMode(WriteMode writeMode) {
        if (writeMode == null) {
            log.warn("写入模式不能为空");
            return;
        }
        
        try {
            redisTemplate.opsForValue().set(writeModeKey, writeMode.name());
            log.info("写入模式已更新为: {}", writeMode);
        } catch (Exception e) {
            log.error("更新写入模式异常: {}", writeMode, e);
        }
    }
} 