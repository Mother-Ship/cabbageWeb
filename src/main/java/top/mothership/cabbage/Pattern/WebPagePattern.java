package top.mothership.cabbage.Pattern;

import java.util.regex.Pattern;

public class WebPagePattern {
    /**
     * osu官网中添加好友连接的表达式
     */
    public final static Pattern ADD_FRIEND_REGEX = Pattern.compile("<div class='centrep'>\\n<a href='([^']*)");

    /**
     * 从旧官网取出正确的S和SS数据
     */
    public final static Pattern CORRECT_X_S = Pattern.compile("<td width='42'><img height='42' src='//s.ppy.sh/images/X.png'></td><td width='50'>(\\d*)</td>\n" +
            "<td width='42'><img height='42' src='//s.ppy.sh/images/S.png'></td><td width='50'>(\\d*)</td>\n");
}
