package top.mothership.cabbage.task;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.osu.ApiUtil;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
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
    private final JavaMailSender javaMailSender;
    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired
    public DairyTask(UserDAO userDAO, UserInfoDAO userInfoDAO, ApiUtil apiUtil, JavaMailSender javaMailSender, FreeMarkerConfigurer freeMarkerConfigurer) {
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.apiUtil = apiUtil;
        this.javaMailSender = javaMailSender;
        this.freeMarkerConfigurer = freeMarkerConfigurer;
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
                    legacyUname.add(user.getCurrentUname());
                    user.setLegacyUname(new Gson().toJson(legacyUname));
                    user.setCurrentUname(userinfo.getUserName());
                    logger.info("检测到玩家" + userinfo.getUserName() + "改名，已登记");
                }
                //如果能获取到userinfo，就把banned设置为0
                user.setBanned(0);
                userDAO.updateUser(user);
            } else {
               //将null的用户直接设为banned
                user.setBanned(1);
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
