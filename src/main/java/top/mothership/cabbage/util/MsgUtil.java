package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.CqMsg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MsgUtil {
    private final CqUtil cqUtil;
    private Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    public MsgUtil(CqUtil cqUtil) {
        this.cqUtil = cqUtil;
    }

    public boolean CheckDayParam(CqMsg cqMsg) {
        try {
            //此处传入的Message必须是切好的#后面的数据
            int day = Integer.valueOf(cqMsg.getMessage());
            if (day > (int) ((new java.util.Date().getTime() - new SimpleDateFormat("yyyy-MM-dd").parse("2007-09-16").getTime()) / 1000 / 60 / 60 / 24)) {
                cqMsg.setMessage("你要找史前时代的数据吗。");
                cqUtil.sendMsg(cqMsg);
                logger.info("指定的日期早于osu!首次发布日期");
                return false;
            }
            if (day < 0) {
                cqMsg.setMessage("白菜不会预知未来。");
                cqUtil.sendMsg(cqMsg);
                logger.info("天数不能为负值");
                return false;
            }
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？");
            cqUtil.sendMsg(cqMsg);
            logger.info("给的天数不是int值");
            return false;
        } catch (ParseException e) {
            //由于解析的是固定字符串，不会出异常，无视
        }
        return true;
    }

    public boolean CheckNumParam(CqMsg cqMsg){
        try {
            int num = Integer.valueOf(cqMsg.getMessage());
            if (num < 0 || num > 100) {
                cqMsg.setMessage("其他人看不到的东西，白菜也看不到啦。");
                cqUtil.sendMsg(cqMsg);
                logger.info("BP不能大于100或者小于0");
                return false;
            } else {
                return true;
            }
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("Ай-ай-ай-ай-ай, что сейчас произошло!");
            cqUtil.sendMsg(cqMsg);
            logger.info("给的BP数目不是int");
            return false;
        }
    }
}
