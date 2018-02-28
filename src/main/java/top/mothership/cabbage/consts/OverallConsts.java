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

    public final static String CHANGELOG = "2018-2-28 \n" +
            "*新增：AllModeSupport.\n" +
            "（Cost命令从原理上不支持其他游戏模式。）\n" +
            "1.!mode命令。可以指定自己所使用的默认游戏模式。影响stat/bp/recent/bns四个系列的命令。\n" +
            "2.上述四个系列命令均支持新增参数：模式。可以使用!statme#2:std这样的格式来按照指定的模式查询。\n" +
            "*未指定模式时，视情况采用你指定的默认模式/主模式。我们尽量向下兼容。\n" +
            "*我们支持多种对游戏模式的称呼，可以自行挖掘（\n" +
            "3.其他模式有了自己的结算界面。\n" +
            "osu!mania的分数图标来自官网论坛的某LoveLive皮肤（https://osu.ppy.sh/forum/t/539048）；\n" +
            "osu!ctb的分数图标为原SakuraMiku皮肤（https://tieba.baidu.com/p/4399134680）自带；\n" +
            "osu!taiko的分数图标来自ProjectVOEZ皮肤（来自皮肤群213726031）。\n" +
            "\n" +
            "*新增：现在如果你邀请白菜入群，并且你使用过白菜，四个管理员会收到关于你的osu!id绑定信息。\n" +
            "*新增：最近一次历史数据的API（例：http://www.mothership.top:8080/api/v1/userinfo/nearest/2545898），会在查询没有使用过白菜的玩家时自动完成登记。\n" +
            "*新增：现在!search命令支持显示指定acc/cb/miss/100/50数的PP。\n" +
            "如果指定了acc，它会覆盖掉指定的100 50数量。" +
            "可接受的表示acc的符号有：%acc\n" +
            "100、50：x100(50)/*100(50)\n" +
            "miss：x/*miss/xm/*m\n" +
            "combo：c/cb\n" +
            "这四个参数需要用空格分割。\n" +
            "一个合法的例子是：!search歌手-歌名[难度名](麻婆名){AR10OD10HP10CS10}:osu!std<100cb2x1003x504xmiss>+HDHR\n" +
            "对于!me/!rc命令，指定的成绩参数会被忽略。\n" +
            "\n" +
            "*新增：!search/!me命令支持全角符号。\n" +
            "*新增：!sudoadv命令。向白菜加入的所有群发送广告。特别的，当广告内容是changelog的时候，发送当前版本更新日志。\n" +
            "*新增：每小时对所有凌晨录入失败而标记为被ban的玩家尝试更新状态。\n" +
            "\n" +
            "*移除：移除!changelog命令。替代为!sudoadv命令，在每次更新并测试通过时，由管理员手动发送更新日志。\n" +
            "*移除：移除!fp命令。现在当你使用!me命令时，如果这个谱面你是#1，会自动出现fp命令的界面。\n" +
            "\n" +
            "*修正：现在从旧官网（而不是新官网）获取正确的S、X数据，加快了速度。\n" +
            "*修正：现在!search结果界面的歌曲文字信息离缩略图更近（模拟没有留下成绩的情况）。\n" +
            "*修正：现在!sleep命令如果在感叹号之前有其他文字，什么都不会发生。\n" +
            "（应该不会出现转发别人的sleep消息，导致自己被禁言的情况了。但是如果恶意利用这点，我认为管理员有权按心情禁你言。）\n" +
            "*修正：调整绘图逻辑，现在用户组没有对应的BG/徽标时会采用默认的BG/不绘制徽标。\n" +
            "*修正：!search/!me命令在没有指定Artist时，会补上一个空Artist（可以省去开头的横杠了）。\n" +
            "*修正：优化!sudo help，增加了一个分类。\n" +
            "*修正：更新oppai版本为1.0.12。更新日志参考https://github.com/Francesco149/koohii/releases，作者没有提到PP计算不准确的问题，我不确定更新oppai版本能否解决。\n" +
            "*修正：!sudo listMsg命令现在可以正常使用了。\n" +
            "*修正：对官网数据异常（如S数目小于0，或没有玩过某个模式）的玩家进行异常处理。\n" +
            "*修正：!sudo checkroleban命令更名为!sudo roleinfo，返回详细信息。";
}

