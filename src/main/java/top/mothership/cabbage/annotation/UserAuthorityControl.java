package top.mothership.cabbage.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface UserAuthorityControl {
    /**
     * @author 瞿瀚盛
     * 在这里写管理员QQ,要加人的话得维护两个地方。。
     */
    long[] value() default {};

}
