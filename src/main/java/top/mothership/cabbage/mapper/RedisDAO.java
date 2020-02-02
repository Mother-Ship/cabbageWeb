package top.mothership.cabbage.mapper;


import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.util.concurrent.TimeUnit;

/**
 * Created by QHS on 2017/5/28.
 */
@Component
public class RedisDAO {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisDAO(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(Integer userId, Userinfo userinfo) {
        redisTemplate.opsForHash().put(String.valueOf(userId), String.valueOf(userinfo.getMode()), new Gson().toJson(userinfo));
    }

    public void add(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void add(String key, String value, Long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public Userinfo get(Integer userId, Integer mode) {
        return new Gson().fromJson((String) redisTemplate.opsForHash().get(String.valueOf(userId), String.valueOf(mode)), Userinfo.class);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void expire(Integer userId, final long timeout, final TimeUnit unit) {
        redisTemplate.expire(String.valueOf(userId), timeout, unit);
    }

    public void expire(String key, final long timeout, final TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    public void flushDb() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
}