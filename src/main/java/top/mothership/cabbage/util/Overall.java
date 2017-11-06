package top.mothership.cabbage.util;

import top.mothership.cabbage.pojo.CoolQ.CqMsg;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;

public class Overall {
    public final static String MAIN_FILTER_REGEX = "[!！]([^ \\u4e00-\\u9fa5]+)([\\u892a\\u88d9\\u9000\\u7fa4\\u767d\\u83dcA-Za-z0-9\\[\\] :#-_]*+)";
    public final static String IMG_REGEX = ".*\\[CQ:image,file=(.+)\\].*";
    public final static String SINGLE_IMG_REGEX = "\\[CQ:image,file=(.+)\\]";
    public final static String ADMIN_CMD_REGEX = "[!！]sudo ([^ ]*)(.*)";
    public final static String CMD_REGEX = "[!！]([^ ]+)(.*)";
    public final static String CMD_REGEX_NUM = "[!！]([^ ]+)([^#]*) #(.+)";
    public final static String MAIN_FILTER_REGEX_CHINESE = "[!！]([^ \\u4e00-\\u9fa5]+)(.*+)";
    public final static ResourceBundle CABBAGE_CONFIG = ResourceBundle.getBundle("cabbage");
    public final static String REPEAT_FILTER_REGEX = "[^\\u4e00-\\u9fa5a-zA-Z0-9]";
    public final static String BGNAME_REGEX = "(?<=[\\d*],[\\d*],\")(?:.*\\\\)*(.*\\.(?i)(jpg|png|jpeg))";
    public final static String DOWNLOAD_FILENAME_REGEX ="(?<=filename=\")([^\";]*)";
    public final static String ADD_FRIEND_REGEX = "<div class='centrep'>\\n<a href='([^']*)";
    public static List<String> ADMIN_LIST = Arrays.asList(Overall.CABBAGE_CONFIG.getString("admin").split(","));
    public static LinkedHashMap<CqMsg, String> inviteRequests = new LinkedHashMap<>();
}
