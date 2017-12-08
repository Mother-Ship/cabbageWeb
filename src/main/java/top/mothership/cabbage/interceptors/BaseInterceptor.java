package top.mothership.cabbage.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import top.mothership.cabbage.annotation.NeedLogin;
import top.mothership.cabbage.mapper.RedisDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public class BaseInterceptor extends HandlerInterceptorAdapter {
    /*
    拦截器两个用处：访问需要登录的API时检查是否已经登录，以及登录时验证给的验证码是否正确（没有注册系统）
    整个登录和权限验证都可以放在过滤器这一层，controller只判断下有无用户信息然后进行业务操作就行
     */
    private final RedisDAO redisDAO;

    @Autowired
    public BaseInterceptor(RedisDAO redisDAO) {
        this.redisDAO = redisDAO;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截每个方法
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        //预定义一个boolean
        boolean isPass = false;
        if(method.getAnnotation(NeedLogin.class)!=null){
            //如果方法加了@NeedLogin的注解，则取参数判断是否已登录
            isPass = isLogin(request);
            return isPass;
        }
        return true;
    }

    private boolean isLogin(HttpServletRequest request){

        return false;
    }

}
