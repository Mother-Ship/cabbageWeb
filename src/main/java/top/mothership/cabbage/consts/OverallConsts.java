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

    public final static String CHANGELOG = "2018-3-19\n" +
            "*移除 !sudo check系列命令。（被!sudo searchplayer取代）\n" +
            "*修正 !pr命令的模式错误问题。\n" +
            "*修正 !bp系列命令无法使用的问题。\n" +
            "*新增 !每日数据录入增加了redis缓存。\n" +
            "*新增 !search命令的结果加入了AR的缩圈毫秒，以及OD对应的判定毫秒数。\n";
}

