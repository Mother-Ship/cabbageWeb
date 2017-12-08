package top.mothership.cabbage.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface UserRoleControl {
    /**
     * @author 瞿瀚盛
     * 默认参数为四个管理员……即使我指定了static final的long[]也会报错，只能指定默认值了……
     */
    long[] value() default {2307282906L,2643555740L,992931505L};

}
