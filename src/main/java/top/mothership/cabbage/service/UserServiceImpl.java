package top.mothership.cabbage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.util.web.CaptchaUtil;

@Service
public class UserServiceImpl {
    private final CaptchaUtil captchaUtil;
    private RedisDAO redisDAO;


    @Autowired
    public UserServiceImpl(CaptchaUtil captchaUtil, RedisDAO redisDAO) {
        this.captchaUtil = captchaUtil;
        this.redisDAO = redisDAO;
    }
}
