package top.mothership.cabbage.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.annotation.GroupRoleControl;
import top.mothership.cabbage.annotation.UserRoleControl;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Aspect
/**
 * @author 瞿瀚盛
 * 用于处理权限控制的类，配合自定义注解
 */
@Order(1)
public class RoleControlAspect {
    private final CqManager cqManager;

    /**
     * 在构造函数中注入发送QQ消息的工具类
     *
     * @param cqManager 用于发送QQ消息
     */
    @Autowired
    public RoleControlAspect(CqManager cqManager) {
        this.cqManager = cqManager;

    }
    /**
     * 拦截service层所有方法中带AllowedUser注解的方法
     */
    @Pointcut("execution(* top.mothership.cabbage.serviceImpl.*.*(..))")
    private void aspectjMethod() {
    }

    @Around("aspectjMethod() && args(cqMsg)")
    public Object doAround(ProceedingJoinPoint pjp, CqMsg cqMsg) throws Throwable {
        UserRoleControl userRoleControl = pjp.getTarget().getClass().getMethod(
                pjp.getSignature().getName(),
                ((MethodSignature)pjp.getSignature()).getParameterTypes()
        ).getAnnotation(UserRoleControl.class);
        if(userRoleControl==null){
           Annotation[] a =  pjp.getTarget().getClass().getAnnotations();
           for(Annotation aList:a){
               if(aList.getClass().equals(UserRoleControl.class)){
                   userRoleControl = (UserRoleControl) a[1];
               }
           }
        }
        if(userRoleControl==null){
            return pjp.proceed();
        }
        if (Arrays.stream(userRoleControl.value()).boxed().collect(Collectors.toList()).contains(cqMsg.getUserId())) {
            return pjp.proceed();
        }else {
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqManager.sendMsg(cqMsg);
            return null;
        }

    }
}


