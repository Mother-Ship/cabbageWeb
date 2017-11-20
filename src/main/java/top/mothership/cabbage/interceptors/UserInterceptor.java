package top.mothership.cabbage.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import top.mothership.cabbage.mapper.RedisDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInterceptor extends HandlerInterceptorAdapter {
    /*
    前端传入用户名/密码/验证码，去redis查找，并且负责颁发token
     */
    private final RedisDAO redisDAO;

    @Autowired
    public UserInterceptor(RedisDAO redisDAO) {
        this.redisDAO = redisDAO;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        return false;
    }
}
