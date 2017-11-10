package top.mothership.cabbage.mapper;


import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Created by QHS on 2017/5/28.
 */
@Repository
public class RedisDAO {

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    @Resource(name = "redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    public void addValue(String key, String value) {
        valueOperations.set(key, value);
    }

    public String getValue(String key) {
        return valueOperations.get(key);
    }

    public void expire(String key, final long timeout, final TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }
}