package top.mothership.cabbage.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.WebResponse;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;
import top.mothership.cabbage.serviceImpl.UserServiceImpl;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.ImgUtil;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {"application/json;charset=UTF-8"})
public class ApiController {
    private final UserServiceImpl userService;
    private Logger logger = LogManager.getLogger(this.getClass());
    private final UserInfoDAO userInfoDAO;
    private final ApiManager apiManager;
    private final ImgUtil imgUtil;
    private final UserDAO userDAO;
    private final UserUtil userUtil;
    private final WebPageManager webPageManager;

    @Autowired
    public ApiController(UserServiceImpl userService, UserInfoDAO userInfoDAO, ApiManager apiManager, ImgUtil imgUtil, UserDAO userDAO, CqServiceImpl cqService, UserUtil userUtil, WebPageManager webPageManager) {
        this.userService = userService;
        this.userInfoDAO = userInfoDAO;
        this.apiManager = apiManager;
        this.imgUtil = imgUtil;
        this.userDAO = userDAO;
        this.userUtil = userUtil;
        this.webPageManager = webPageManager;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
    }

    @RequestMapping(value = "/userinfo/{uid}", method = RequestMethod.GET)
    public String userInfo(@PathVariable Integer uid,
                           @RequestParam("start") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate start,
                           @RequestParam("limit") Integer limit,
                           @RequestParam(value = "mode", required = false) Integer mode) {
        User user = userDAO.getUser(null, uid);
        if (user != null && mode == null) {
            //如果没有指定模式，而且用户
            mode = user.getMode();
        }
        //去osu api验证用户名是否存在
        Userinfo now = apiManager.getUser(mode, uid);
        if (now == null) {
            return new Gson().toJson(new WebResponse<>(1, "user not found", null));
        }
        //取到最接近8.29的那条记录
        Userinfo earliest = userInfoDAO.getNearestUserInfo(mode, now.getUserId(), LocalDate.of(2017, 8, 29));
        if (earliest == null) {
            return new Gson().toJson(new WebResponse<>(2, "user not registered", null));
        }
        //判断传入日期是否比最早的记录早，是则返回错误+最早的记录时间
        if (start.isBefore(earliest.getQueryDate())) {
            return new Gson().toJson(new WebResponse<>(3, "start date is too early", earliest.getQueryDate()));
        }
        //判断最早的记录+传入的天数是否比今天晚
        //这里不必-1，否则传入当天日期会返回0+null
        if (start.plusDays(limit).isAfter(LocalDate.now())) {
            return new Gson().toJson(new WebResponse<>(4, "end date is too late", null));
        }
        List<Userinfo> list = new ArrayList<>();
        for (long i = 0; i < limit; i++) {
            Userinfo tmp = userInfoDAO.getUserInfo(mode, now.getUserId(), start.plusDays(i));
            list.add(tmp);
        }

        return new Gson().toJson(new WebResponse<>(0, "ok", list));
    }

    @RequestMapping(value = "/user/qq/{qq}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String userRole(@PathVariable Long qq) {
        User user = userDAO.getUser(qq, null);
        if (user == null) {
            return new Gson().toJson(new WebResponse<>(1, "user not found", null));
        } else if (user.isBanned()) {
            return new Gson().toJson(new WebResponse<>(2, "user is banned", user));
        } else {
            return new Gson().toJson(new WebResponse<>(0, "success", user));
        }
    }

    @RequestMapping(value = "/stat/{uid}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public void getStat(HttpServletResponse response, @PathVariable Integer uid, @RequestParam(value = "mode", defaultValue = "0", required = false) Integer mode) {
        String role;
        Integer scoreRank;
        User user = userDAO.getUser(null, uid);
        Userinfo userFromAPI = apiManager.getUser(mode, uid);
        Userinfo userInDB = null;
        boolean near = false;
        int day = 1;
        if (user == null) {
            if (userFromAPI == null) {
                return;
            } else {
                logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                userUtil.registerUser(userFromAPI.getUserId(), mode, 0L, OverallConsts.DEFAULT_ROLE);
                userInDB = userFromAPI;
            }
            role = "creep";
        } else if (user.isBanned()) {
            //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
            userFromAPI = userInfoDAO.getNearestUserInfo(mode, user.getUserId(), LocalDate.now());
            //尝试补上当前用户名
            if (user.getCurrentUname() != null) {
                userFromAPI.setUserName(user.getCurrentUname());
            } else {
                List<String> list = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                }.getType());
                if (list.size() > 0) {
                    userFromAPI.setUserName(list.get(0));
                } else {
                    userFromAPI.setUserName(String.valueOf(user.getUserId()));
                }
            }
            role = user.getRole();
            day = 0;
        } else {
            List<String> list = userUtil.sortRoles(user.getRole());
            role = list.get(0);
            userInDB = userInfoDAO.getUserInfo(mode, userFromAPI.getUserId(), LocalDate.now().minusDays(day));
            if (userInDB == null) {
                userInDB = userInfoDAO.getNearestUserInfo(mode, userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                near = true;
            }
        }


        //获取score rank
        //gust？
        if (userFromAPI.getUserId() == 1244312
                //怕他
                || userFromAPI.getUserId() == 6149313
                //小飞菜
                || userFromAPI.getUserId() == 3995056
                //苏娜小苏娜
                || userFromAPI.getUserId() == 3213720
                //MFA
                || userFromAPI.getUserId() == 6854920) {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 10000);
        } else {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, role, day, near, scoreRank, user.getMode());
        byte[] bytes = Base64.getDecoder().decode(result);
        response.setContentType("image/png");
        try (InputStream in = new ByteArrayInputStream(bytes);
             OutputStream out = response.getOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            BufferedImage img2 = new BufferedImage(600, 288, BufferedImage.TYPE_INT_RGB);
            img2.getGraphics().drawImage(img.getScaledInstance(600, 288, Image.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(img2, "png", new MemoryCacheImageOutputStream(out));
            out.write(bytes);
        } catch (IOException ignore) {

        }
    }

    @RequestMapping(value = "/userinfo/nearest/{uid}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String nearestUserInfo(@PathVariable Integer uid, @RequestParam(value = "mode", defaultValue = "0", required = false) Integer mode) {
        Userinfo userInDB = userInfoDAO.getNearestUserInfo(mode, uid, LocalDate.now().minusDays(1));
        if (userInDB == null) {
            userInDB = apiManager.getUser(mode, uid);
            userUtil.registerUser(userInDB.getUserId(), mode, 0L, OverallConsts.DEFAULT_ROLE);
            return new Gson().toJson(new WebResponse<>(1, "user not registered", null));
        }
        return new Gson().toJson(new WebResponse<>(0, "ok", userInDB));
    }


//    @RequestMapping(value = "/upload",method = RequestMethod.POST)
//    @CrossOrigin(origins = "http://localhost")
//    public String upload(@RequestParam(value = "myfile") MultipartFile file) throws Exception {
//        String path = "C:\\CoolQ Pro\\data\\image\\resource\\db\\";
//        String fileName = file.getOriginalFilename();
//        String resp;
//
//        if(!fileName.matches("(.*).db$")){
//            logger.warn("检测到文件扩展名错误的文件上传请求：文件名为"+fileName);
//            //返回字符串给JS进行弹窗
//            return "文件类型错误";
////            throw new Exception("类型错误");
//        }
//        File targetFile = new File(path, fileName);
//        if (!targetFile.exists()){
//            targetFile.mkdirs();
//            // 保存
//            resp = "服务端消息：上传成功";
//        }else{
//            resp = "服务端消息：覆盖成功";
//        }
//        try {
//            logger.info("检测到文件上传请求：文件名为"+fileName);
//            file.transferTo(targetFile);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        return resp;
//    }
}