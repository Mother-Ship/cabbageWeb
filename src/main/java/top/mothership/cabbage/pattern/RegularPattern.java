package top.mothership.cabbage.pattern;

import java.util.regex.Pattern;

/**
 * 正则表达式的常量类。
 *
 * @author QHS
 */
public class RegularPattern {
    /**
     * 匹配出带/不带数字的常规命令，简单的用空格分割命令字符串。
     * 当处理!statme类命令时：group(1)为statme，2和3为""
     * !setid xxx类命令是：group(1)为setid，group(2)为xxx，3为""
     */
    public final static Pattern REG_CMD_REGEX = Pattern.compile("[!！]\\s*([^#:： ]*)\\s*(.*)");
    /**
     * sleep命令专用正则，只有感叹号在全文开头时才匹配
     */
    public final static Pattern SLEEP_REGEX = Pattern.compile("^[!！]\\s*([^#:： ]*)\\s*(.*)");

    /**
     * 匹配出sudo命令
     */
    public final static Pattern ADMIN_CMD_REGEX = Pattern.compile("[!！]\\s*sudo\\s+([^#:： ]*)\\s*(.*)");
    /**
     * 复读禁言时抗干扰的匹配表达式
     */
    public final static Pattern REPEAT_FILTER_REGEX = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z0-9]");
    /**
     * 从.osu文件中匹配出BG文件名的表达式
     */
    public final static Pattern BGNAME_REGEX = Pattern.compile("(?<=[\\d*],[\\d*],\")(?:.*\\\\)*(.*\\.(?i)(jpg|png|jpeg))");


    /**
     * UNICODE转String的表达式。
     */
    public final static Pattern UNICODE_TO_STRING = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");

    /**
     * 这是一个弱智玩意
     * 贼他妈弱智
     */
    public final static Pattern QIANESE_RECENT = Pattern.compile("((?:4|5|t|f|d|e|r)(?:1|q|w|3|e|r|4|d)(?:x|d|f|v|c)(?:1|q|w|3|e|r|4)(?:b|h|j|m|n)(?:r|5|6|y|g|f|t))");
    /**
     * 指定需要以文本方式返回的子命令
     */
    public final static Pattern TEXT_VERSION_COMMAND = Pattern.compile("rs|bps|bpus|bpmes|mybps");
    /**
     * 赛事分析系统中用来处理bid
     */
    public final static Pattern ANALYZE_BID_PARAM = Pattern.compile("[!！]([^ ]*)[ ]?(\\d*)");
    /**
     * 匹配osu的UID（1-8个数字）
     */
    public final static Pattern OSU_USER_ID = Pattern.compile("^(\\d{1,8})$");
    /**
     * 匹配osu的ID（3-15个\w、横杠 左括号右括号空格）
     */
    public final static Pattern OSU_USER_NAME = Pattern.compile("([0-9A-Za-z_\\-\\[\\]][0-9A-Za-z_\\-\\[\\] ]{1,13}[0-9A-Za-z_\\-\\[\\]])");

    /**
     * 匹配QQ（5-10个数字）
     */
    public final static Pattern QQ = Pattern.compile("^(\\d{5,10})$");
    public final static Pattern BPNUM = Pattern.compile("^(\\d{1,3})$");
    public final static Pattern URL = Pattern.compile("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");

}
