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

    public final static String CHANGELOG = "2018-3-27 15:23:07\n" +
            "*恢复 !sudo unbind命令。\n" +
            "*新增 一键获取兑换码功能。\n" +
            "*新增 给两个时雨厨加上时雨的报时。\n" +
            "*恢复 黄花菜对接相关。\n";
}

