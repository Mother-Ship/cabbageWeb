package top.mothership.cabbage.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.util.CqUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Component
@Aspect
public class CqServiceAspect {
    //需求：将所有发生的异常收集到某个数据结构里，再使用命令调用
private final CqUtil cqUtil;
    @Autowired
    public CqServiceAspect(CqUtil cqUtil) {
        this.cqUtil = cqUtil;
    }


    //这样只需要定义一次，这个方法用来处理什么包的类
    @Pointcut("execution(* top.mothership.cabbage.*.*.*(..))")
    private void aspectjMethod() {
    }


    //直接进行try catch，而不是@AfterThrowing啥的（
    @Around(value = "aspectjMethod()")
    public Object doAround(ProceedingJoinPoint pjp){
        Object result=null;
        try {
            result=pjp.proceed();
        } catch (Throwable e) {

           String resp = new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())+
                   "\n"+e.toString() + "，方法：" + pjp.getTarget().getClass() + "."
                   + pjp.getSignature().getName() + "()\n方法入参：";
            Object[] args = pjp.getArgs();
            for (Object arg : args) {
                resp = resp.concat("\n" + arg);
            }
            StackTraceElement[] a = e.getStackTrace();
            for (int i = 0; i < 10; i++) {
                //打印前10个堆栈信息
                    resp = resp.concat("\n    at " + a[i]);
            }
            resp = resp.concat("\n……");
            CqMsg cqMsg = new CqMsg();
            cqMsg.setMessage(resp);
            cqMsg.setUserId(1335734657L);
            cqMsg.setMessageType("private");
            cqUtil.sendMsg(cqMsg);
        }
        return result;
    }

}
