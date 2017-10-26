package top.mothership.cabbage.util;

import java.util.ResourceBundle;

public class Constant {
    public final static String MAIN_FILTER_REGEX = "[!！]([^ \\u4e00-\\u9fa5]+)([\\u892a\\u88d9\\u9000\\u7fa4\\u767d\\u83dcA-Za-z0-9\\[\\] :#-_]*+)";
    public final static String IMG_REGEX = ".*\\[CQ:image,file=(.+)\\].*";
    public final static String SINGLE_IMG_REGEX = "\\[CQ:image,file=(.+)\\]";
    public final static String ADMIN_CMD_REGEX = "[!！]sudo ([^ ]*)(.*)";
    public final static String CMD_REGEX = "[!！]([^ ]+)(.*)";
    public final static String CMD_REGEX_NUM = "[!！]([^ ]+)([^#]*) #(.+)";
    public final static  String MAIN_FILTER_REGEX_CHINESE = "[!！]([^ \\u4e00-\\u9fa5]+)(.*+)";
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");
    public final static String REPEAT_FILTER_REGEX = "[^\\u4e00-\\u9fa5a-zA-Z0-9]";
    public final static String BGNAME_REGEX = "(?<=[\\d*],[\\d*],\")(?:.*\\\\)*(.*\\.(jpg|png|JPG|PNG|Jpg|jPg|jpG|JPg|jPG|Png|pNg|pnG|PNg|pNG))";
    public final static String DOWNLOAD_FILENAME_REGEX ="(?<=filename=\")([^\";]*)";
    public final static String ADD_FRIEND_REGEX = "<div class='centrep'>\\n<a href='([^']*)";
}
