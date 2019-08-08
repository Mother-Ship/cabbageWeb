package top.mothership.cabbage.constant.pattern;

import java.util.regex.Pattern;

public class CQCodePattern {
    /**
     * 匹配出带图片的消息
     */
    public final static Pattern MSG_WITH_IMAGE = Pattern.compile("\\[CQ:image,file=.+]");
    /**
     * 匹配出艾特消息中的QQ号（At全体时候，中间是all，所以不指定为数字）
     */
    public final static Pattern AT = Pattern.compile("\\[CQ:at,qq=(.*)]");
    /**
     * 匹配出纯图片的消息
     */
    public final static Pattern SINGLE_IMG = Pattern.compile("^\\[CQ:image,file=.+]$");
}
