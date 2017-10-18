package top.mothership.cabbage.util;

import top.mothership.cabbage.pojo.CqMsg;

import java.util.ArrayList;

public class MsgQueue {
    private final static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";
    private final static String filterRegex = "[^\\u4e00-\\u9fa5a-zA-Z0-9]";
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
                if (msg.getMessage().replaceAll(filterRegex, "")
                        .equals(msgs[i].getMessage().replaceAll(filterRegex, ""))
                        && !msg.getMessage().matches(singleImgRegex) && msg.getMessage().length() >= 3) {
                    count++;
                }
            }
        } else {
            for (int i = 0; i < start - 1; i++) {
                if (msg.getMessage().replaceAll(filterRegex, "")
                        .equals(msgs[i].getMessage().replaceAll(filterRegex, ""))
                        && !msg.getMessage().matches(singleImgRegex) && msg.getMessage().length() >= 3) {
                    count++;
                }
            }
            for (int i = end; i < msgs.length; i++) {
                if (msg.getMessage().replaceAll(filterRegex, "")
                        .equals(msgs[i].getMessage().replaceAll(filterRegex, ""))
                        && !msg.getMessage().matches(singleImgRegex) && msg.getMessage().length() >= 3) {
                    count++;
                }
            }
        }
        return count>=6;
    }
    public void addMsg(CqMsg msg){
        //循环队列的具体实现
        len++;
        if (len >= 100) {
            len = 100;
            start++;
        }
        if (end == 100) {
            end = 0;
        }
        if (start == 100) {
            start = 0;
        }
        msgs[end] =msg;
        //将这条消息存储到这个类的成员变量里
        this.msg = msg;
        end++;

    }
    public ArrayList<CqMsg> toArrayList(){
        ArrayList<CqMsg> result = new ArrayList<>();
        if (start < end) {
            for (int i = 0; i < end; i++) {
                result.add(msgs[i]);
            }
        } else {
            for (int i = 0; i < start - 1; i++) {
                result.add(msgs[i]);
            }
            for (int i = end; i < msgs.length; i++) {
                result.add(msgs[i]);
            }
        }
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
            for (int i = 0; i < start - 1; i++) {
                if(QQ.equals(msgs[i].getUserId()))
                result.add(msgs[i]);
            }
            //目前会引发一个问题，当容器刚启动，这个群还没有消息的时候，调用这个会出NPE
            //但是我并不打算去用if，毕竟几乎不可能出现这个问题……
            for (int i = end; i < msgs.length; i++) {
                if(QQ.equals(msgs[i].getUserId()))
                result.add(msgs[i]);
            }
        }
        return result;

    }
}
