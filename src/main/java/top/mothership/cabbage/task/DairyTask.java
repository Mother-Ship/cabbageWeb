package top.mothership.cabbage.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.osu.ApiUtil;
import top.mothership.cabbage.util.qq.CqUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Date;
import java.util.*;

@Component
public class DairyTask {
    private Logger logger = LogManager.getLogger(this.getClass());
    private UserDAO userDAO;
    private UserInfoDAO userInfoDAO;
    private ApiUtil apiUtil;
    private CqUtil cqUtil;
//    private final JavaMailSender javaMailSender;
//    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    public DairyTask(UserDAO userDAO, UserInfoDAO userInfoDAO, ApiUtil apiUtil, JavaMailSender javaMailSender, FreeMarkerConfigurer freeMarkerConfigurer, CqUtil cqUtil) {
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.apiUtil = apiUtil;
//        this.javaMailSender = javaMailSender;
//        this.freeMarkerConfigurer = freeMarkerConfigurer;
        this.cqUtil = cqUtil;
    }
    //似乎每分钟并发也就600+，不需要加延迟……
    @Scheduled(cron = "0 0 4 * * ?")
    public void importUserInfo() {
        java.util.Date start = Calendar.getInstance().getTime();
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, -1);
        userInfoDAO.clearTodayInfo(new Date(cl.getTimeInMillis()));
        logger.info("开始进行每日登记");
        List<Integer> list = userDAO.listUserIdByRole(null);
        for (Integer aList : list) {
            User user = userDAO.getUser(null,aList);
            Userinfo userinfo = apiUtil.getUser(null, aList);
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24));
                userInfoDAO.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据录入成功");
                if(!userinfo.getUserName().equals(user.getCurrentUname())){
                    //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                    List<String> legacyUname =  new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {}.getType());
                    if(user.getCurrentUname()!=null)
                    legacyUname.add(user.getCurrentUname());
                    user.setLegacyUname(new Gson().toJson(legacyUname));
                    user.setCurrentUname(userinfo.getUserName());
                    logger.info("检测到玩家" + userinfo.getUserName() + "改名，已登记");
                }
                if (Arrays.asList(user.getRole().split(",")).contains("mp4")) {

                   if(userinfo.getPpRaw() > Integer.valueOf(Overall.CABBAGE_CONFIG.getString("mp4PP")) + 0.49){
                       CqMsg cqMsg = new CqMsg();
                       cqMsg.setMessageType("group");
                       cqMsg.setGroupId(564679329L);
                    //回溯1天前的PP
                       Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(aList,new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24*2));
                       if(lastDayUserinfo.getPpRaw() > Integer.valueOf(Overall.CABBAGE_CONFIG.getString("mp4PP")) + 0.49){
                           //回溯2天前的PP
                           lastDayUserinfo = userInfoDAO.getUserInfo(aList,new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24*3));
                           if(lastDayUserinfo.getPpRaw() > Integer.valueOf(Overall.CABBAGE_CONFIG.getString("mp4PP")) + 0.49){
                               //回溯3天前的PP
                               lastDayUserinfo = userInfoDAO.getUserInfo(aList,new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24*4));
                               if(lastDayUserinfo.getPpRaw() > Integer.valueOf(Overall.CABBAGE_CONFIG.getString("mp4PP")) + 0.49){

                               }else{
                                   if(user.getQQ()!=null) {
                                       cqMsg.setUserId(user.getQQ());
                                       cqMsg.setMessageType("kick");
                                       cqUtil.sendMsg(cqMsg);
                                       cqMsg.setMessageType("private");
                                       cqMsg.setMessage("由于PP超限，已将你移出MP4群。");
                                       cqUtil.sendMsg(cqMsg);
                                   }
                               }
                           }else{
                               if(user.getQQ()!=null) {
                                   cqMsg.setMessage("[CQ:at,qq=" + user.getQQ() + "] 检测到你的PP超限。将会在1天后将你移除。");
                                   cqUtil.sendMsg(cqMsg);
                               }

                               continue;
                           }
                       }else{
                            //如果前一天PP没有超
                           if(user.getQQ()!=null){
                           cqMsg.setMessage("[CQ:at,qq="+user.getQQ()+"] 检测到你的PP超限。将会在2天后将你移除。" );
                           cqUtil.sendMsg(cqMsg);
                       }
                           continue;
                       }
                       if(user.getQQ()!=null){
                           cqMsg.setMessage("[CQ:at,qq="+user.getQQ()+"] 检测到你的PP超限。将会在3天后将你移除。" );
                           cqUtil.sendMsg(cqMsg);
                       }
                   }
                }
                if (Arrays.asList(user.getRole().split(",")).contains("mp5")) {
                    if(userinfo.getPpRaw() > Integer.valueOf(Overall.CABBAGE_CONFIG.getString("mp5PP")) + 0.49){

                    }
                }
                //如果能获取到userinfo，就把banned设置为0
                user.setBanned(0);
                userDAO.updateUser(user);
            } else {
               //将null的用户直接设为banned
                user.setBanned(1);
                logger.info("检测到玩家" + user.getUserId() + "被Ban，已登记");
                userDAO.updateUser(user);
            }
        }
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
    }
    @Scheduled(cron = "0 0 4 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get(Overall.CABBAGE_CONFIG.getString("path") + "/data/image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("正在删除" + file.toString());
                    Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path2 = Paths.get(Overall.CABBAGE_CONFIG.getString("path") + "/data/record");
        SimpleFileVisitor<Path> finder2 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        try {

            Files.walkFileTree(path, finder);
            Files.walkFileTree(path2, finder2);
        } catch (IOException e) {
            logger.error("清空临时文件时出现异常，"+e.getMessage());
        }

    }

//    private void sendMail(String target, Map<String, String> map) {
//        String content;
//        Template template = null;
//        try {
//            template = freeMarkerConfigurer.getConfiguration().getTemplate("mail.ftl");
//            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
//            //创建多媒体邮件
//            MimeMessage mmm = javaMailSender.createMimeMessage();
//            //修改邮件体
//            MimeMessageHelper mmh = new MimeMessageHelper(mmm, true, "UTF-8");
//            //设置发件人信息
//            mmh.setFrom("1335734657@qq.com");
//            //收件人
//            mmh.setTo(target);
//            //主题
//            mmh.setSubject("数据录入结果通知");
//            //内容
//            mmh.setText(content, true);
//            //送出
//            javaMailSender.send(mmm);
//        } catch (MessagingException | IOException | TemplateException e) {
//            e.printStackTrace();
//        }


//    }
}
