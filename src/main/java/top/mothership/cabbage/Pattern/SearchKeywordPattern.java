package top.mothership.cabbage.Pattern;

import java.util.regex.Pattern;

public class SearchKeywordPattern {
    /**
     * osu search命令的表达式
     * 2017-12-4 10:45:55 现在支持分隔符后带空格了，横杠前的空格还是手动处理
     * 2018-2-24 23:32:22 现在支持全角【】（）
     */
    public final static Pattern OSU_SEARCH_KETWORD = Pattern.compile("^([^-]*)-[ ]?(.*)[ ]?[\\[【](.*)[]】][ ]?[(（](.*)[)）]\\{(.*)}");

    /**
     * 搞了一半失败的Shell格式
     */
//    public final static Pattern OSU_SEARCH_KETWORD_SHELL = Pattern.compile("((?:-(a|t|d|m|ar|od|cs|hp)) ([^-]*)){1,8}");
    /**
     * osu!search功能用的带mod表达式
     */
    public final static Pattern OSU_SEARCH_MOD_ONLY = Pattern.compile("[!！]([^ ]*)[ ]?(.*)(?:\\+| \\+)(.*)");
    /**
     * osu!search功能用的带mod+指定计算PP参数（acc，miss等）的表达式，由于acc之类的是可选项，不计算在keyword里面，所以单独写一条式子
     */
    public final static Pattern OSU_SEARCH_MOD_AND_ACC_ETC = Pattern.compile("[!！]([^ ]*)[ ]?(.*)[《<](.*)[》>](?:\\+| \\+)(.*)");
    /**
     * 取出四维
     */
    public final static Pattern OSU_SEARCH_FOUR_DEMENSIONS_REGEX = Pattern
            .compile("(?:AR(\\d{1,2}(?:\\.|。)?(?:\\d{1,2})?))?(?:OD(\\d{1,2}[.。]?(?:\\d{1,2})?))?(?:CS(\\d{1,2}[.。]?(?:\\d{1,2})?))?(?:HP(\\d{1,2}(?:\\.|。)?(?:\\d{1,2})?))?", Pattern.CASE_INSENSITIVE);

    /**
     * 用来处理纯数字搜索词（bid）的表达式
     */
    public final static Pattern ALL_NUMBER_SEARCH_KEYWORD = Pattern.compile("^(\\d{1,7})$");
    public final static Pattern KEYWORD_ACC = Pattern.compile("^(?:(\\d{1,2}[.。]?(?:\\d{1,2})?))(?:%|acc)$");
    public final static Pattern KEYWORD_COUNT_100 = Pattern.compile("^(\\d{1,4})(?:x100|\\*100)$");
    public final static Pattern KEYWORD_COUNT_50 = Pattern.compile("^(\\d{1,4})(?:x50|\\*50)$");
    public final static Pattern KEYWORD_MISS = Pattern.compile("^(\\d{1,4})(?:x|\\*miss)$");
    public final static Pattern KEYWORD_COMBO = Pattern.compile("^(\\d{1,5})(?:%|acc)$");
}
