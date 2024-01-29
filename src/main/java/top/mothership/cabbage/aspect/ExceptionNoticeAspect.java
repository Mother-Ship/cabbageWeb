package top.mothership.cabbage.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The type Exception notice aspect.
 *
 * @author 瞿瀚盛  用于处理全局异常的类，一旦有异常发生就通过QQ消息通知我
 */
@Component
@Aspect
/**
 * 异常通知的优先级必须在拦截之后
 */
@Order(3)
public class ExceptionNoticeAspect {
    @Autowired
    private CqManager cqManager;
    private Logger logger = LogManager.getLogger(this.getClass());
    /**
     * 不知道放这对不对……总之是格式化时间用的
     */
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss");



    /**
     * 捕获并通知异常，这里不使用@AfterThrowing（我也忘记原因了，反正有坑）
     *
     * @param pjp 程序运行时的织入点
     * @return aop织入方法时处理的方法返回结果
     */
    @Around(value = "aspectjMethod()")
    public Object exceptionNotice(ProceedingJoinPoint pjp) {
        Object result = null;

        try {
            result = pjp.proceed();

        } catch (Throwable e) {
            logger.error("",e);
            String resp = formatter.format(LocalDateTime.now()) +
                    "\n异常类型：" + e.toString() + "\n方法：" + pjp.getTarget().getClass() + "."
                    + pjp.getSignature().getName() + "()\n方法入参：";
            Object[] args = pjp.getArgs();
            for (Object arg : args) {
                if (arg != null) {
                    resp = resp.concat("\n" + arg);
                }
            }
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
//                if(stackTraceElement.getClassName().contains("top.mothership")) {
                resp = resp.concat("\n    at " + stackTraceElement);
//                }else if(stackTraceElement.getClassName().contains("java.")){
//                    resp = resp.concat("\n    at " + stackTraceElement);
//                } else if(stackTraceElement.getClassName().contains("javax.")){
//                    resp = resp.concat("\n    at " + stackTraceElement);
//                }else{
//                    if(!resp.endsWith("\n    ……")) {
//                        resp = resp.concat("\n    ……");
//                    }
//                }
            }
            CqMsg cqMsg = new CqMsg();
            cqMsg.setMessage(resp);
            cqMsg.setSelfId(1335734629L);
            cqMsg.setUserId(1335734657L);
            cqMsg.setMessageType("private");
            cqManager.sendMsg(cqMsg);

        }
        return result;
    }

    /**
     * 用于指定后续方法的异常捕捉范围
     */
    @Pointcut("execution(* top.mothership.cabbage.*.*.*(..))")
    private void aspectjMethod() {
    }

}
