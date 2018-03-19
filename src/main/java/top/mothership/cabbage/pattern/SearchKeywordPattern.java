package top.mothership.cabbage.pattern;

import java.util.regex.Pattern;

public class SearchKeywordPattern {
    /**
     * osu search命令的表达式
     * 2017-12-4 10:45:55 现在支持分隔符后带空格了，横杠前的空格还是手动处理
     * 2018-2-24 23:32:22 现在支持全角【】（）
     */
    public final static Pattern KETWORD = Pattern.compile("^([^-]*)-[ ]?(.*)[ ]?[\\[【](.*)[]】][ ]?[(（](.*)[)）]\\{(.*)}");

    /**
     * 搞了一半失败的Shell格式
     */
//    public final static pattern OSU_SEARCH_KETWORD_SHELL = pattern.compile("((?:-(a|t|d|m|ar|od|cs|hp)) ([^-]*)){1,8}");

    /**
     * MOD
     */
    public final static Pattern MOD = Pattern.compile("[!！]([^ ]*)[ ]?(.*)(?:\\+| \\+)(.*)");
    /**
     * acc cb miss
     */
    public final static Pattern PP_CALC = Pattern.compile("[!！]([^ ]*)[ ]?(.*)[《<](.*)[》>]");
    /**
     * mode
     */
    public final static Pattern MODE = Pattern.compile("[!！]([^ ]*)[ ]?(.*)(?: :| ：|：|:)(.*)");
    /**
     * 取出四维
     */
    public final static Pattern FOUR_DIMENSIONS_REGEX = Pattern
            .compile("(?:AR(\\d{1,2}(?:\\.|。)?(?:\\d{1,2})?))?(?:OD(\\d{1,2}[.。]?(?:\\d{1,2})?))?(?:CS(\\d{1,2}[.。]?(?:\\d{1,2})?))?(?:HP(\\d{1,2}(?:\\.|。)?(?:\\d{1,2})?))?", Pattern.CASE_INSENSITIVE);

    /**
     * 用来处理纯数字搜索词（bid）的表达式
     */
    public final static Pattern ALL_NUMBER_SEARCH_KEYWORD = Pattern.compile("^(\\d{1,7})$");
    public final static Pattern KEYWORD_ACC = Pattern.compile("^(?:(\\d{1,2}[.。]?(?:\\d{1,2})?))(?:%|acc)$");
    public final static Pattern KEYWORD_COUNT_100 = Pattern.compile("^(\\d{1,4})(?:x100|\\*100)$");
    public final static Pattern KEYWORD_COUNT_50 = Pattern.compile("^(\\d{1,4})(?:x50|\\*50)$");
    public final static Pattern KEYWORD_MISS = Pattern.compile("^(\\d{1,4})(?:x|\\*miss|xm|\\*m|xmiss)$");
    public final static Pattern KEYWORD_COMBO = Pattern.compile("^(\\d{1,5})(?:c|cb)$");
}
