package top.mothership.cabbage.pojo;

import org.aspectj.lang.ProceedingJoinPoint;

public class Exception {
    public Exception(Long time,java.lang.Throwable e, ProceedingJoinPoint pjp) {
        this.e = e;
        this.pjp = pjp;
        this.time = time;
    }
    private Long time;

    private java.lang.Throwable e;
    private ProceedingJoinPoint pjp;

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Throwable getE() {
        return e;
    }

    public void setE(Throwable e) {
        this.e = e;
    }

    public ProceedingJoinPoint getPjp() {
        return pjp;
    }

    public void setPjp(ProceedingJoinPoint pjp) {
        this.pjp = pjp;
    }
}
