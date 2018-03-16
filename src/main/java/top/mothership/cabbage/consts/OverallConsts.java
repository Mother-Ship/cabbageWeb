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

    public final static String CHANGELOG = "2018-3-9 \n" +
            "*移除 !sudo 褪裙命令。（被每天凌晨的扫描PP超限替代。）\n" +
            "*移除 !sudo adv命令。（会导致QQ被封禁。）\n" +
            "*移除 !sudo unbind命令。（被钦点命令取代。）\n" +
            "*修正 !search 命令Artist/Mapper位置不正确问题。\n" +
            "*修正 !search 命令在没有结果时强行判断模式触发NPE，导致没有正常输出的问题。\n" +
            "*修正 某些情况下中文冒号指定模式不识别的问题。\n" +
            "*修正 语音文件移到数据库。\n" +
            "*修正 现在!recent和!pr有了正确的模式提示。\n" +
            "*修正 !mode命令的错误提示信息现在能正确显示了。\n" +
            "*修正 !sudo afk命令在处理用户组中被ban玩家时引发NPE的问题。\n" +
            "*修正 !add命令现在会先检查指定的QQ是否已经绑定用户。\n" +
            "*修正 !sudo系列命令现在有了AOP参数验证，支持全半角冒号。\n" +
            "*新增 !sudo 钦点 命令。强行修改某玩家的用户组、QQ。\n" +
            "*新增 !cost 命令增加了第十届OCLB的Cost及相关信息。\n";
}

