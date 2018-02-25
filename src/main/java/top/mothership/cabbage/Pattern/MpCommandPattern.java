package top.mothership.cabbage.Pattern;

import java.util.regex.Pattern;

public class MpCommandPattern {
    /**
     * MP系列命令,和sudo一样
     */
    public final static Pattern MP_CMD_REGEX = Pattern.compile("[!！]mp ([^ ]*)[ ]?([^:：]*)[:|：]?(.*)");
    /**
     * 对所有服务器消息（不是banchobot发送的私聊消息）进行基本的筛选……
     */
    public final static Pattern IRC_SERVER_MSG = Pattern.compile(":cho.ppy.sh (\\d\\d\\d) (.+)");
    /**
     * Banchobot发送的没有该用户的消息
     */
    public final static Pattern ROOM_NOT_EXIST = Pattern.compile(":cho.ppy.sh 401 .+ #mp_(.+) :No such nick");

    /**
     * Banchobot发送的 创建房间已经达到4个的消息
     */
    public final static Pattern ROOM_LIMITED = Pattern.compile(
            ":BanchoBot!cho@ppy.sh PRIVMSG [.*] :You cannot create any more tournament matches. Please close any previous tournament matches you have open.");

    /**
     * 其他用户发送的私聊
     */
    public final static Pattern IRC_PRIVATE_MSG = Pattern.compile(":(.+)!cho@ppy.sh PRIVMSG (.+) :(.+)");

}
