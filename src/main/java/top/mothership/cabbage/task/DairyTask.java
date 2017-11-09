package top.mothership.cabbage.task;

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

    @Scheduled(cron = "0 0 4 * * ?")
    public void importUserInfo() {
        java.util.Date start = Calendar.getInstance().getTime();
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, -1);
        userInfoDAO.clearTodayInfo(new Date(cl.getTimeInMillis()));
        logger.info("开始进行每日登记");
        List<Integer> list = userDAO.listUserIdByRole(null);
        List<Integer> nullList = new ArrayList<>();
        for (Integer aList : list) {
            Userinfo userinfo = apiUtil.getUser(null, String.valueOf(aList));
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis() - 1000 * 3600 * 24));
                userInfoDAO.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据录入成功");
            } else {
                nullList.add(aList);
            }
        }
        if (nullList.size() > 0) {
            logger.info("以下玩家没有在官网抓取到数据，正在发送邮件：" + nullList);
            Map<String, String> map = new HashMap<>();
            map.put("nullList", nullList.toString());
            sendMail("1335734657@qq.com", map);
            sendMail("2307282906@qq.com", map);
        }
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
    }
    @Scheduled(cron = "0 0 4 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get(Overall.CABBAGE_CONFIG.getString("path") + "\\data\\image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().contains("resource")
                        && !file.toString().contains("!help")
                        && !file.toString().contains("!smokeAll")
                        && !file.toString().contains("!helpTrick")) {
                    System.out.println("正在删除" + file.toString());
                    Files.delete(file);
                }
                return super.visitFile(file, attrs);
            }
        };
        try {

            Files.walkFileTree(path, finder);
        } catch (IOException e) {
            logger.error("清空临时文件时出现异常，"+e.getMessage());
        }

    }


    private void sendMail(String target, Map<String, String> map) {
        String content;
        Template template = null;
        try {
            template = freeMarkerConfigurer.getConfiguration().getTemplate("mail.ftl");
            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            //创建多媒体邮件
            MimeMessage mmm = javaMailSender.createMimeMessage();
            //修改邮件体
            MimeMessageHelper mmh = new MimeMessageHelper(mmm, true, "UTF-8");
            //设置发件人信息
            mmh.setFrom("1335734657@qq.com");
            //收件人
            mmh.setTo(target);
            //主题
            mmh.setSubject("数据录入结果通知");
            //内容
            mmh.setText(content, true);
            //送出
            javaMailSender.send(mmm);
        } catch (MessagingException | IOException | TemplateException e) {
            e.printStackTrace();
        }


    }
}
