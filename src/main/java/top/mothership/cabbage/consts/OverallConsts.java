package top.mothership.cabbage.consts;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 全局常量
 * @author QHS
 */
public class OverallConsts {
    /**
     * 指定配置文件
     */
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");
    /**
     * sudo命令使用者列表
     */
    public static List<String> ADMIN_LIST = Arrays.asList(CABBAGE_CONFIG.getString("admin").split(","));
}
