package top.mothership.cabbage.util;

public class MsgQueue {
    private int start = 0;
    private int end = 0;
    private int len = 0;
    private String[] msgs = new String[100];
    //避免空指针
    private String msg ="";

    public boolean isRepeat(){
        int count =0;
        //根据循环队列情况不同进行遍历
        if (start < end) {
            //复读不抓三个字以下的和纯图片
            for (int i = 0; i < end; i++) {
                if (msg.equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                    count++;
                }
            }
        } else {
            for (int i = 0; i < start - 1; i++) {
                if (msg.equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                    count++;
                }
            }
            for (int i = end; i < msgs.length; i++) {
                if (msg.equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                    count++;
                }
            }
        }
        return count>=6;
    }
    public void addMsg(String msg){
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

}
