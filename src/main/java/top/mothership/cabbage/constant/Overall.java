package top.mothership.cabbage.constant;

import top.mothership.cabbage.enums.ParameterEnum;

import java.util.ResourceBundle;

/**
 * 全局常量
 *
 * @author QHS
 */
public class Overall {
    /**
     * 指定配置文件
     */
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");

    public final static long[] ADMIN_LIST = {2307282906L,1335734657L,2643555740L,992931505L,735862173L};

    public final static String DEFAULT_ROLE = "creep";

    public final static ParameterEnum[] EMPTY_PARAMETER_LIST = new ParameterEnum[]{};

    public final static String LAST_MODIFIED  = "2018-5-22 21:41:31";
}

