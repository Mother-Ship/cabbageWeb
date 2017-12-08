package top.mothership.cabbage.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * The type Dairy task.
 */
@Component
public class DairyTask {
    private Logger logger = LogManager.getLogger(this.getClass());
    private UserDAO userDAO;
    private UserInfoDAO userInfoDAO;
    private ApiManager apiManager;
    private CqManager cqManager;
//    private final JavaMailSender javaMailSender;
//    private final FreeMarkerConfigurer freeMarkerConfigurer;

    /**
     * Instantiates a new Dairy task.
     *
     * @param userDAO     the user dao
     * @param userInfoDAO the user info dao
     * @param apiManager  the api manager
     * @param cqManager   the cq manager
     */
    @Autowired
    public DairyTask(UserDAO userDAO, UserInfoDAO userInfoDAO, ApiManager apiManager, CqManager cqManager) {
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.apiManager = apiManager;
        this.cqManager = cqManager;
    }


    /**
     * Import user info.
     */

    @Scheduled(cron = "0 0 4 * * ?")
    public void importUserInfo() {
        //似乎每分钟并发也就600+，不需要加延迟……
        java.util.Date start = Calendar.getInstance().getTime();
        //清掉前一天全部信息
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));
        logger.info("开始进行每日登记");
        List<Integer> list = userDAO.listUserIdByRole(null);
        for (Integer aList : list) {
            User user = userDAO.getUser(null, aList);
            Userinfo userinfo = apiManager.getUser(null, aList);
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(LocalDate.now().minusDays(1));
                userInfoDAO.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据录入成功");
                if (!userinfo.getUserName().equals(user.getCurrentUname())) {
                    //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                    List<String> legacyUname = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                    }.getType());
                    if (user.getCurrentUname() != null) {
                        legacyUname.add(user.getCurrentUname());
                    }
                    user.setLegacyUname(new Gson().toJson(legacyUname));
                    user.setCurrentUname(userinfo.getUserName());
                    logger.info("检测到玩家" + userinfo.getUserName() + "改名，已登记");
                }
                //如果用户在mp4组
                if (Arrays.asList(user.getRole().split(",")).contains("mp4")) {
                    //并且刷超了
                    if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                        CqMsg cqMsg = new CqMsg();
                        cqMsg.setMessageType("group");
                        cqMsg.setGroupId(564679329L);
                        //回溯昨天这时候检查到的pp
                        Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(2));
                        //如果昨天这时候的PP存在，并且也超了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                            //继续回溯前天这时候的PP
                            lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(3));
                            //如果前天这时候的PP存在，并且也超了
                            if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                                //回溯大前天的PP
                                lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(4));
                                //如果大前天这个时候也超了，就飞了
                                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setUserId(user.getQq());
                                        cqMsg.setMessageType("kick");
                                        cqManager.sendMsg(cqMsg);
                                        cqMsg.setMessageType("private");
                                        cqMsg.setMessage("由于PP超限，已将你移出MP4群。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                } else {
                                    //大前天没超
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                }
                            } else {
                                //前天没超
                                if (!user.getQq().equals(0L)) {
                                    cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。");
                                    cqManager.sendMsg(cqMsg);
                                }
                                continue;
                            }
                        } else {
                            //昨天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。");
                                cqManager.sendMsg(cqMsg);
                            }

                        }
                        continue;
                    }

                }

                if (Arrays.asList(user.getRole().split(",")).contains("mp5")) {
                    //并且刷超了
                    if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                        CqMsg cqMsg = new CqMsg();
                        cqMsg.setMessageType("group");
                        cqMsg.setGroupId(201872650L);
                        //回溯昨天这时候检查到的pp
                        Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(2));
                        //如果昨天这时候的PP存在，并且也超了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                            //继续回溯前天这时候的PP
                            lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(3));
                            //如果前天这时候的PP存在，并且也超了
                            if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                                //回溯大前天的PP
                                lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(4));
                                //如果大前天这个时候也超了，就飞了
                                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setUserId(user.getQq());
                                        cqMsg.setMessageType("kick");
                                        cqManager.sendMsg(cqMsg);
                                        cqMsg.setMessageType("private");
                                        cqMsg.setMessage("由于PP超限，已将你移出MP5群。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                } else {
                                    //大前天没超
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                }
                            } else {
                                //前天没超
                                if (!user.getQq().equals(0L)) {
                                    cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。");
                                    cqManager.sendMsg(cqMsg);
                                }
                                continue;
                            }
                        } else {
                            //昨天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。");
                                cqManager.sendMsg(cqMsg);
                            }

                        }
                        continue;
                    }

                }

                //如果能获取到userinfo，就把banned设置为0
                user.setBanned(false);
                userDAO.updateUser(user);
            } else {
                //将null的用户直接设为banned
                user.setBanned(true);
                logger.info("检测到玩家" + user.getUserId() + "被Ban，已登记");
                userDAO.updateUser(user);
            }
        }
    }

    /**
     * Clear today images.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get(OverallConsts.CABBAGE_CONFIG.getString("path") + "/data/image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path2 = Paths.get(OverallConsts.CABBAGE_CONFIG.getString("path") + "/data/record");
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
            logger.error("清空临时文件时出现异常，" + e.getMessage());
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
