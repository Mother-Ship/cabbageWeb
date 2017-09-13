package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.Userinfo;
import top.mothership.cabbage.service.CqService;
import top.mothership.cabbage.util.*;

import java.sql.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CqServiceImpl implements CqService {
    private CqMsg cqMsg;
    private ResourceBundle rb = ResourceBundle.getBundle("cabbage");
    private static String adminCmdRegex = "[!！]sudo ([^ ]*)(.*)";
    private static String cmdRegex = "[!！]([^ ]+)(.*)";
    private static String cmdRegexWithNum = "[!！]([^ ]+)([^#]*) #(.+)";
    private static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";
    private static String[] msgs = new String[100];
    private static int start = 0;
    private static int end = 0;
    private static int len = 0;
    private static List<Long> mp5Admin = Arrays.asList(2643555740L, 290514894L, 2307282906L, 2055805091L, 735862173L,
            1142592265L, 263202941L, 992931505L, 1335734657L, 526942417L, 1012621328L);
    private static List<Long> mp4Admin = Arrays.asList(89217167L, 295539897L, 290514894L, 2307282906L,
            2643555740L, 2055805091L, 954176984L, 879804833L, 526942417L);
    private static List<CqMsg> inviteRequests = new ArrayList<>();
    private final ApiUtil apiUtil;
    private final CqUtil cqUtil;
    private final ImgUtil imgUtil;
    private final MsgUtil msgUtil;
    private final WebPageUtil webPageUtil;
    private BaseMapper baseMapper;
    private Logger logger = LogManager.getLogger(this.getClass());


    @Autowired
    public CqServiceImpl(ApiUtil apiUtil, MsgUtil msgUtil, CqUtil cqUtil, ImgUtil imgUtil, WebPageUtil webPageUtil, BaseMapper baseMapper) {
        this.apiUtil = apiUtil;
        this.msgUtil = msgUtil;
        this.cqUtil = cqUtil;
        this.imgUtil = imgUtil;
        this.webPageUtil = webPageUtil;
        this.baseMapper = baseMapper;
    }


    @Override
    public void praseCmd(CqMsg cqMsg) {
        this.cqMsg = cqMsg;
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(cmdRegex).matcher(msg);
        m.find();
        switch (m.group(1)) {
            case "stat":
                //先经过检验参数
                int day = 1;
                String  username = m.group(2).substring(1);
                if (msg.matches(cmdRegexWithNum)) {
                    m = Pattern.compile(cmdRegexWithNum).matcher(msg);
                    m.find();
                    cqMsg.setMessage(m.group(3));
                    if (!msgUtil.CheckDayParam(cqMsg)) {
                        return;
                    }
                    day = Integer.valueOf(m.group(3));
                }
                statUserInfo(username,day);
                break;
            case "bp":
                break;
            case "setid":
                break;
            case "statme":
                break;
            case "bpme":
                break;
            case "recent":
                break;
            case "help":
                break;
        }
    }

    @Override
    public void praseAdminCmd(CqMsg cqMsg) {
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(adminCmdRegex).matcher(msg);
        m.find();
        switch (m.group(1)) {
            case "add":
                break;
            case "del":
                break;
            case "check":
                break;
            case "褪裙":
            case "退群":
                break;
            case "bg":
                break;
            case "recent":
                break;
            case "afk":
                break;
            case "smoke":
                break;
            case "invite":
                break;

        }
    }

    @Override
    public void praseSmoke(CqMsg cqMsg) {
//这里拿到的是没有刮去图片的
        int count = 0;
        String msg = cqMsg.getMessage();
        if (msg.matches(singleImgRegex)) {
            msg = "Image";
        }
        //刮掉除了中文英文数字之外的东西
        msg = msg.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        //循环数组
        if (cqMsg.getGroupId() == 201872650 || cqMsg.getGroupId() == 564679329 || cqMsg.getGroupId() == 532783765) {
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
            //把群号拼在字符串上
            msgs[end] = cqMsg.getGroupId() + msg;
            end++;
            if (start < end) {
                //复读不抓三个字以下的和纯图片
                for (int i = 0; i < end; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                        count++;
                    }
                }
            } else {
                for (int i = 0; i < start - 1; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                        count++;
                    }
                }
                for (int i = end; i < msgs.length; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i]) && !msg.equals("Image") && msg.length() >= 3) {
                        count++;
                    }
                }
            }

        }
        if (count >= 6) {
            String resp;
            if (mp5Admin.contains(cqMsg.getUserId()) && cqMsg.getGroupId() == 201872650) {
                logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                cqMsg.setMessage("[CQ:at,qq=2643555740] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");

            } else if (mp4Admin.contains(cqMsg.getUserId()) && cqMsg.getGroupId() == 564679329) {
                logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                cqMsg.setMessage("[CQ:at,qq=1012621328] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
            } else {
                logger.info("正在尝试禁言" + cqMsg.getUserId());
                cqMsg.setDuration(600);
                cqMsg.setMessageType("smoke");
            }
            cqUtil.sendMsg(cqMsg);
        }
    }

    @Override
    public void praseNewsPaper(CqMsg cqMsg) {
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的加群请求");
        String resp;
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp5。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "564679329":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp4。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "537646635":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp乐园主群。请修改一下你的群名片(包含osu! id)，以下为mp乐园系列分群介绍：\n" +
                        "osu!mp乐园高rank部 592339532\n" +
                        "osu!mp乐园3号群(四位数7000pp上限):234219559\n" +
                        "OSU! MP乐园4号群 (MP4) *(3600-5100pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4000pp，无严格下限):201872650";
                break;
            default:
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎加入本群。";
                break;
        }
        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);

    }

    @Override
    public void stashInviteRequest(CqMsg cqMsg) {
        inviteRequests.add(cqMsg);
    }

    private void statUserInfo(String username,int day){
        //将day转换为Date
        Calendar cl = Calendar.getInstance();
        if(cl.get(Calendar.HOUR_OF_DAY)<4){
            cl.add(Calendar.DAY_OF_MONTH,-1);
        }
        cl.add(Calendar.DATE, -day);
        Date date = new Date(cl.getTimeInMillis());
        //预定义变量
        boolean near = false;
        Userinfo userInDB = null;
        String role;
        int scoreRank;
        //彩蛋
        if ("白菜".equals(username)) {
            cqMsg.setMessage("唉，没人疼没人爱，我是地里一颗小白菜。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        logger.info("开始调用API查询" + username + "的信息");
        Userinfo userFromAPI = apiUtil.getUser(username, null);
        if (userFromAPI == null) {
            cqMsg.setMessage("没有在官网查到这个玩家。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        if (userFromAPI.getUserId() == 3) {
            cqMsg.setMessage("你们总是想查BanchoBot。\\n可是BanchoBot已经很累了，她不想被查。\\n她想念自己的小ppy，而不是被逼着查PP。\\n你有考虑过这些吗？没有！你只考虑过你自己。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        //如果day=0则不检验也不获取数据库中的userinfo（不写入数据库，避免预先查过之后add不到）
        if(day>0){
            userInDB =  getUserInDb(userFromAPI,date);
            if(userInDB==null){
                userInDB = baseMapper.getNearestUserInfo(userFromAPI.getUserId(),date);
                near = true;
            }
        }
        role = baseMapper.getUser(null,userFromAPI.getUserId()).getRole();
        if(role==null){role = "creep";}
        //获取score rank
        if (userFromAPI.getUserId() == 1244312
                || userFromAPI.getUserId() == 6149313
                || userFromAPI.getUserId() == 3213720) {
            scoreRank = webPageUtil.getRank(userFromAPI.getRankedScore(), 1, 10000);
        } else {
            scoreRank = webPageUtil.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        //调用绘图类绘图
        imgUtil.drawUserInfo(userFromAPI,userInDB,role,day,near,scoreRank);
        //构造消息并发送
        cqMsg.setMessage("[CQ:image,file=" + userFromAPI.getUserName() + ".png]");
        cqUtil.sendMsg(cqMsg);

    }

    private Userinfo getUserInDb(Userinfo userFromAPI,Date date){
        //如果userrole表中没有该玩家的记录
       if (baseMapper.getUser(null,userFromAPI.getUserId())==null){
           logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
           //构造User对象写入数据库
           User user = new User();
           user.setUserId(userFromAPI.getUserId());
           baseMapper.addUser(user);
            //构造日历对象和List
           Calendar c = Calendar.getInstance();
           if (c.get(Calendar.HOUR_OF_DAY) < 4) {
               c.add(Calendar.DAY_OF_MONTH, -1);
           }
           userFromAPI.setQueryDate(new java.sql.Date(c.getTime().getTime()));
           List<Userinfo> list = new ArrayList<>();
           list.add(userFromAPI);
           //写入一行userinfo
           baseMapper.addUserInfo(list);
           return userFromAPI;
       }else{
           //查找指定日期的Userinfo
           return baseMapper.getUserInfo(userFromAPI.getUserId(),date);
       }
    }
}
