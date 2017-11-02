package top.mothership.cabbage.util;

import top.mothership.cabbage.pojo.CoolQ.CqMsg;

import java.util.ArrayList;

public class MsgQueue {

    private int start = 0;
    private int end = 0;
    private int len = 0;
    private CqMsg[] msgs = new CqMsg[100];
    //避免空指针
    private CqMsg msg =new CqMsg();




    public boolean isRepeat(){

        int count =0;
        //根据循环队列情况不同进行遍历
        if (start < end) {
            //复读不抓三个字以下的和纯图片
            for (int i = 0; i < end; i++) {
                //为了避免纯标点被识别为复读，如果过滤之后剩下空串，则恢复原样
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
        return count>=6;
    }
    public void addMsg(CqMsg msg){
        //循环队列的具体实现
        //首先长度增加
        len++;
        //如果0-100都有了消息，那就把start右移一个
        if (len >= 100) {
            len = 100;
            start++;
        }
        //如果end已经达到数组最右端，就移到最左端
        if (end == 100) {
            end = 0;
        }
        if (start == 100) {
            start = 0;
        }
        //把消息存到结束坐标里
        msgs[end] =msg;
        //将这条消息存储到这个类的成员变量里
        this.msg = msg;
        //结束坐标+1
        end++;

    }
    public ArrayList<CqMsg> toArrayList(){
        ArrayList<CqMsg> result = new ArrayList<>();
        if (start < end) {
            for (int i = 0; i < end; i++) {
                result.add(msgs[i]);
            }
        } else {
            for (int i = end; i < msgs.length; i++) {
                result.add(msgs[i]);
            }
            for (int i = 0; i < start - 1; i++) {
                result.add(msgs[i]);
            }

        }
//        Collections.sort(result);
        return result;
    }

    public ArrayList<CqMsg> getMsgsByQQ(Long QQ){
        ArrayList<CqMsg> result = new ArrayList<>();
        if (start < end) {
            for (int i = 0; i < end; i++) {
                if(QQ.equals(msgs[i].getUserId()))
                result.add(msgs[i]);
            }
        } else {
            for (int i = end; i < msgs.length; i++) {
                if(QQ.equals(msgs[i].getUserId()))
                    result.add(msgs[i]);
            }
            for (int i = 0; i < start - 1; i++) {
                if(QQ.equals(msgs[i].getUserId()))
                result.add(msgs[i]);
            }
            //目前会引发一个问题，当容器刚启动，这个群还没有消息的时候，调用这个会出NPE
            //但是我并不打算去用if，毕竟几乎不可能出现这个问题……

        }
//        Collections.sort(result);
        return result;

    }
    private boolean isThisRepeat(int i){
        String msgFromGroup;
        String msgFromArray;
        msgFromArray = msgs[i].getMessage().replaceAll(Constant.REPEAT_FILTER_REGEX, "");
        msgFromGroup = msg.getMessage().replaceAll(Constant.REPEAT_FILTER_REGEX, "");

        if("".equals(msgFromArray)){
            msgFromArray = msgs[i].getMessage();
        }
        if("".equals(msgFromGroup)){
            msgFromGroup = msg.getMessage();
        }
        return msgFromArray
                .equals(msgFromGroup)
                && !msg.getMessage().matches(Constant.SINGLE_IMG_REGEX) && msg.getMessage().length() >= 3;
    }
}
