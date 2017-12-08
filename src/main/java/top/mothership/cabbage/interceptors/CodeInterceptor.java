package top.mothership.cabbage.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.util.web.CaptchaUtil;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 用来处理验证码的类
 @author QHS
 */
public class CodeInterceptor extends HandlerInterceptorAdapter {
    private final RedisDAO redisDAO;
    private final CaptchaUtil captchaUtil;


    @Autowired
    public CodeInterceptor(RedisDAO redisDAO, CaptchaUtil captchaUtil) {
        this.redisDAO = redisDAO;
        this.captchaUtil = captchaUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取参数
        String urlParam = request.getParameter("code");
        if ("".equals(urlParam)||urlParam == null){
            //如果参数为空，等会调试下看具体表达
            return false;
        }
        Map<String,Object> map = captchaUtil.genCaptcha();
        String verifyCode = (String) map.get("code");
        //把URL参数和code写入redis
        redisDAO.addValue(urlParam,verifyCode);
        //10分钟过期
        redisDAO.expire(urlParam,600,TimeUnit.SECONDS);
        response.setContentType("image/jpg");
        try(OutputStream out = response.getOutputStream()){
            ImageIO.write((BufferedImage)map.get("img"),"jpg",out);
        }
        return false;
    }
}
