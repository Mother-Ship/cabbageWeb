package top.mothership.cabbage.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.annotation.GroupAuthorityControl;
import top.mothership.cabbage.annotation.UserAuthorityControl;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static top.mothership.cabbage.consts.OverallConsts.ADMIN_LIST;

@Component
@Aspect
/**
 * @author 瞿瀚盛
 * 用于处理权限控制的类，配合自定义注解
 */
@Order(1)
public class RoleControlAspect {
    private final CqManager cqManager;
    private static final long[] ZERO = {0L};

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
    @Pointcut("execution(* top.mothership.cabbage.service.*.*(top.mothership.cabbage.pojo.coolq.CqMsg,..))")
    private void aspectjMethod() {
    }

    @Around("aspectjMethod() && args(cqMsg,..)")
    public Object roleControl(ProceedingJoinPoint pjp, CqMsg cqMsg) throws Throwable {
        //取出Class上的注解
        UserAuthorityControl userAuthorityControl = null;

        List<Long> allowedUser = new ArrayList<>();
        //统一管理管理员
        for (long l : ADMIN_LIST) {
            allowedUser.add(l);
        }
        Annotation[] a = pjp.getTarget().getClass().getAnnotations();
        for (Annotation aList : a) {
            if (aList.annotationType().equals(UserAuthorityControl.class)) {
                userAuthorityControl = (UserAuthorityControl) a[1];
            }
        }
        //如果Class上的注解不是null
        if (userAuthorityControl != null) {
            for (long l : userAuthorityControl.value()) {
                allowedUser.add(l);
            }
        }
        //同理 取出方法上的注解
        userAuthorityControl = pjp.getTarget().getClass().getMethod(
                pjp.getSignature().getName(),
                ((MethodSignature) pjp.getSignature()).getParameterTypes()
        ).getAnnotation(UserAuthorityControl.class);
        if (userAuthorityControl != null) {
            for (long l : userAuthorityControl.value()) {
                allowedUser.add(l);
            }
        }
        //如果拿到了用户权限的注解，并且这个注解的值没有消息发送者的qq，并且是QQ消息（而不是事件或者邀请）
        if (allowedUser.size() > 0 && !allowedUser.contains(cqMsg.getUserId()) && "message".equals(cqMsg.getPostType())) {
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqManager.sendMsg(cqMsg);
            return null;
        } else {
            //如果通过了用户权限判别
            //对群权限控制注解进行判别
            if (!"group".equals(cqMsg.getMessageType())) {
                //如果不是群消息，不吃群权限控制
                return pjp.proceed();
            }
            GroupAuthorityControl groupAuthorityControl = pjp.getTarget().getClass().getMethod(
                    pjp.getSignature().getName(),
                    ((MethodSignature) pjp.getSignature()).getParameterTypes()
            ).getAnnotation(GroupAuthorityControl.class);
            if (groupAuthorityControl != null) {
                //如果不允许任何群内使用
                if (groupAuthorityControl.allBanned()) {
                    cqMsg.setMessage("本命令不允许任何群内使用。请私聊。");
                    cqManager.sendMsg(cqMsg);
                    return null;
                }
                if (!Arrays.equals(groupAuthorityControl.allowed(), ZERO)) {
                    //当Allowed不是只有一个0的数组的时候，只有群号符合才通过
                    for (long l : groupAuthorityControl.allowed()) {
                        if (cqMsg.getGroupId().equals(l)) {
                            return pjp.proceed();
                        }
                    }
                    return null;
                }
                //如果Allowed是0，则判断Banned列表
                for (long l : groupAuthorityControl.banned()) {
                    if (cqMsg.getGroupId().equals(l)) {
                        cqMsg.setMessage("该群已停用本命令。");
                        cqManager.sendMsg(cqMsg);
                        return null;
                    }
                }
                for (long l : groupAuthorityControl.bannedDefault()) {
                    if (cqMsg.getGroupId().equals(l)) {
                        cqMsg.setMessage("该群已停用本命令。");
                        cqManager.sendMsg(cqMsg);
                        return null;
                    }
                }
                return pjp.proceed();
            } else {
                return pjp.proceed();
            }
        }


    }
}


