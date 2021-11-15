package top.mothership.cabbage.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * The interface Group role control.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface GroupAuthorityControl {
    /**
     * 禁止使用命令的群。
     *
     * @return the long [ ]
     */
    long[] banned() default {0L};
    //MP4后花园
    //MP2
    //测试群
    //MP3
    //MP5
//    MP5赛群
    //FK群
    long[] bannedDefault() default {
            112177148L,
            234219559L,
            532783765L,
            210342787L,
            201872650L,
            1005266186,
            263668213L,
            693299572L
    };

    /**
     * 允许使用命令的群。
     * 该参数优先级比banned高，如果注解里同时加了allowed和banned同一个群，allowed起效。
     * 如果allowed和banned不同的群，则banned没有意义。
     * （逻辑是 如果存在Allowed，先判断Allowed，如果包含则放行，不包含则直接拦截；不存在Allowed才判断Banned，如果包含则直接阻断；其他情况放行）
     *
     * @return the long [ ]
     */
    long[] allowed() default {0L};

    /**
     * 如果是，则该命令不允许在任何群使用。
     * （比使用默认值判断要简洁明了一点）
     *
     * @return the boolean
     */
    boolean allBanned() default false;
}
