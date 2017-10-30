package top.mothership.cabbage.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.Exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Component
@Aspect
public class CqServiceAspect {
    //需求：将所有发生的异常收集到某个数据结构里，再使用命令调用

    //我需要存储一个Exception对象，入参，发生时间,以及某个标志用来标明它
    public static Map<Integer,Exception> Exceptions = new LinkedHashMap<Integer,Exception>();




    //这样只需要定义一次，这个方法用来处理什么包的类
    @Pointcut("execution(* top.mothership.cabbage.*.*.*(..))")
    private void aspectjMethod() {
    }


    //直接进行try catch，而不是@AfterThrowing啥的（
    @Around(value = "aspectjMethod()")
    public Object doAround(ProceedingJoinPoint pjp){
        long Time=System.currentTimeMillis();
        Object result=null;
        try {
            result=pjp.proceed();
        } catch (Throwable e) {
            Exceptions.put(new Random().nextInt(),new Exception(Time,e,pjp));
        }
        return result;
    }

}
