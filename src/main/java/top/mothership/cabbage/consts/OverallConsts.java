package top.mothership.cabbage.consts;

import java.util.ResourceBundle;

/**
 * 全局常量
 *
 * @author QHS
 */
public class OverallConsts {
    /**
     * 指定配置文件
     */
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");

    public final static long[] ADMIN_LIST = {2307282906L,1335734657L,2643555740L,992931505L};

    public final static String DEFAULT_ROLE = "creep";

    public final static ParameterEnum[] EMPTY_PARAMETER_LIST = new ParameterEnum[]{};

    public final static String CHANGELOG = "2018-3-1 \n" +
            "*修正 !cost命令更新了mp4s2的相关信息。\n" +
            "*修正 !recent命令指定参数不生效的问题。\n" +
            "*修正 !cost命令现在能正确注册新用户了。\n";
}

