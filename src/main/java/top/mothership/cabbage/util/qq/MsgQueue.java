package top.mothership.cabbage.util.qq;

import top.mothership.cabbage.constant.pattern.CQCodePattern;
import top.mothership.cabbage.constant.pattern.RegularPattern;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;

/**
 * 用于存放消息的循环队列
 * @author QHS
 */
public class MsgQueue {

    private int start = 0;
    private int end = 0;
    private int len = 0;
    private int N=100;
    private CqMsg[] msgs = new CqMsg[N];
    public MsgQueue(){}
    public MsgQueue(int N) {
        this.N=N;
        //2018-2-27 10:50:34构造方法居然没有重做一个数组……
        msgs = new CqMsg[N];
    }

    /**
     * 为了避免空指针异常，得new一个
     */
    private CqMsg msg = new CqMsg();

    /**
     * 计算最近的消息构成复读的次数
     * @return 最近一条消息构成复读的次数
     */
    public Integer countRepeat() {
        int count = 0;
        //根据循环队列情况不同进行遍历
        if (start < end) {
            for (int i = 0; i < end; i++) {
                if (isThisRepeat(i)) {
                    count++;
                }
            }
        } else {
            for (int i = end; i < msgs.length; i++) {
                if (isThisRepeat(i)) {
                    count++;
                }
            }
            for (int i = 0; i < start - 1; i++) {
                if (isThisRepeat(i)) {
                    count++;
                }
            }
        }
        return count;
    }
    /**
     * 向队列里增加消息，最近的消息会被写到成员变量里
     * @param msg 要增加的消息
     */
    public void addMsg(CqMsg msg) {
        //循环队列的具体实现
        //首先长度增加
        len++;
        //如果0-100都有了消息，那就把start右移一个
        if (len >= N) {
            len = N;
            start++;
        }
        //如果end已经达到数组最右端，就移到最左端
        if (end == N) {
            end = 0;
        }
        if (start == N) {
            start = 0;
        }
        //把消息存到结束坐标里
        msgs[end] = msg;
        //将这条消息存储到这个类的成员变量里
        this.msg = msg;
        //结束坐标+1
        end++;

    }
    /**
     * 把消息列表转为ArrayList……当时为啥要写这个方法来着？
     * 改成获取重复消息列表吧。
     * @return 消息列表
     */
    public ArrayList<CqMsg> getRepeatList() {
        ArrayList<CqMsg> result = new ArrayList<>();
        //根据循环队列情况不同进行遍历
        if (start < end) {
            for (int i = 0; i < end; i++) {
                if (isThisRepeat(i)) {
                    result.add(msgs[i]);
                }
            }
        } else {
            for (int i = end; i < msgs.length; i++) {
                if (isThisRepeat(i)) {
                    result.add(msgs[i]);
                }
            }
            for (int i = 0; i < start - 1; i++) {
                if (isThisRepeat(i)) {
                    result.add(msgs[i]);
                }
            }
        }

        return result;
    }

    /**
     * 根据QQ从循环队列中提取消息
     *
     * @param QQ 给的QQ号
     * @return 消息列表
     */
    public ArrayList<CqMsg> getMsgsByQQ(Long QQ) {
        ArrayList<CqMsg> result = new ArrayList<>();
        if (start < end) {
            for (int i = 0; i < end; i++) {
                if (QQ.equals(msgs[i].getUserId())) {
                    result.add(msgs[i]);
                }
            }
        } else {
            for (int i = end; i < msgs.length; i++) {
                if (QQ.equals(msgs[i].getUserId())) {
                    result.add(msgs[i]);
                }
            }
            for (int i = 0; i < start - 1; i++) {
                if (QQ.equals(msgs[i].getUserId())) {
                    result.add(msgs[i]);
                }
            }
            //目前会引发一个问题，当容器刚启动，这个群还没有消息的时候，调用这个会出NPE
            //但是我并不打算去用if，毕竟几乎不可能出现这个问题……

        }
        return result;

    }
    /**
     * 判断最近的一条消息是否重复
     *
     * @param i 循环队列里的坐标
     * @return 该消息是否算作复读
     */
    private boolean isThisRepeat(int i) {
        Matcher cmdMatcher = RegularPattern.REG_CMD_REGEX.matcher(msg.getMessage());
        if (cmdMatcher.find()) {
            //如果是命令，直接false
            return false;
        }
        Matcher singleImgMatcher = CQCodePattern.SINGLE_IMG.matcher(msg.getMessage());
        if (singleImgMatcher.find()) {
            //如果是纯图片，直接false
            return false;
        }
        String msgFromArray = CQCodePattern.SINGLE_IMG.matcher(
                RegularPattern.REPEAT_FILTER_REGEX.matcher(msgs[i].getMessage()).replaceAll(""))
                .replaceAll("");
        String msgFromGroup = CQCodePattern.SINGLE_IMG.matcher(
                RegularPattern.REPEAT_FILTER_REGEX.matcher(msg.getMessage()).replaceAll(""))
                .replaceAll("");

        if ("".equals(msgFromArray)) {
            msgFromArray = msgs[i].getMessage();
        }
        if("".equals(msgFromGroup)){
            msgFromGroup = msg.getMessage();
        }
        //目前的问题是：图片+符号会被判定复读，
        //流程是先去掉干扰，如果去干扰后是空串就恢复，然后判断去干扰后的消息是否相等+原消息是否是纯图片+原消息长度是否大于等于3
        //应改为先去掉图片，是空串则
        return msgFromArray.equals(msgFromGroup) && msg.getMessage().length() >= 3;
    }
}
