package top.mothership.cabbage.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
@Aspect
@Component
@Order(0)
public class StatTimeConsAspect {
    /**
     * 打印方法执行耗费时间的日志工具
     */
    private Logger logger = LogManager.getLogger(this.getClass());
    @Pointcut("execution(* top.mothership.cabbage.serviceImpl.*.*(..))")
    private void aspectjMethod() {
    }
    @Around(value = "aspectjMethod()")
    public Object statTimeCons(ProceedingJoinPoint pjp) throws Throwable {
        Instant s = Instant.now();
        pjp.proceed();
        logger.info("处理完毕，共耗费" + Duration.between(s, Instant.now()).toMillis() + "ms。");
        return null;
    }
}
