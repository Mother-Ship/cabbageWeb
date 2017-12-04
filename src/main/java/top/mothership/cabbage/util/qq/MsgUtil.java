package top.mothership.cabbage.util.qq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.util.Overall;

import java.text.ParseException;
import java.text.SimpleDateFormat;

@Component
public class MsgUtil {
    private final CqManager cqManager;
    private Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    public MsgUtil(CqManager cqManager) {
        this.cqManager = cqManager;

    }

    public boolean checkdayparam(String msg, CqMsg cqMsg) {
        try {
            //此处传入的Message必须是切好的#后面的数据
            int day = Integer.valueOf(msg);
            if (day > (int) ((new java.util.Date().getTime() - new SimpleDateFormat("yyyy-MM-dd").parse("2007-09-16").getTime()) / 1000 / 60 / 60 / 24)) {
                cqMsg.setMessage("你要找史前时代的数据吗。");
                cqManager.sendMsg(cqMsg);
                logger.info("指定的日期早于osu!首次发布日期");
                return false;
            }
            if (day < 0) {
                cqMsg.setMessage("白菜不会预知未来。");
                cqManager.sendMsg(cqMsg);
                logger.info("天数不能为负值");
                return false;
            }
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？");
            cqUtil.sendMsg(cqMsg);
            logger.info("给的天数不是int值");
            return false;
        } catch (ParseException ignore) {
            //由于解析的是固定字符串，不会出异常，无视
        }
        return true;
    }

    public boolean checkbpnumparam(String msg, CqMsg cqMsg){
        try {
            int num = Integer.valueOf(msg);
            if (num < 0 || num > 100) {
                cqMsg.setMessage("其他人看不到的东西，白菜也看不到啦。");
                cqUtil.sendMsg(cqMsg);
                logger.info("BP不能大于100或者小于0");
                return false;
            } else {
                return true;
            }
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("[CQ:record,file=base64://"+ Overall.AYA_YA_YA+"]");
            cqUtil.sendMsg(cqMsg);
            logger.info("给的BP数目不是int");
            return false;
        }
    }
    public boolean checkbidparam(String msg, CqMsg cqMsg){
        try {
            int bid = Integer.valueOf(msg);
            return true;
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("It's a disastah!!");
            cqUtil.sendMsg(cqMsg);
            logger.info("给的bid不是int");
            return false;
        }
    }
}
