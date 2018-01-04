package top.mothership.cabbage.controller;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.WebResponse;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;
import top.mothership.cabbage.serviceImpl.UserServiceImpl;
import top.mothership.cabbage.util.qq.ImgUtil;
import top.mothership.cabbage.util.qq.RoleUtil;

import javax.imageio.ImageIO;
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
public class UserController {
    private final UserServiceImpl userService;
    private Logger logger = LogManager.getLogger(this.getClass());
    private final UserInfoDAO userInfoDAO;
    private final ApiManager apiManager;
    private final ImgUtil imgUtil;
    private final UserDAO userDAO;
    private final RoleUtil roleUtil;
    private final WebPageManager webPageManager;

    @Autowired
    public UserController(UserServiceImpl userService, UserInfoDAO userInfoDAO, ApiManager apiManager, ImgUtil imgUtil, UserDAO userDAO, CqServiceImpl cqService, RoleUtil roleUtil, WebPageManager webPageManager) {
        this.userService = userService;
        this.userInfoDAO = userInfoDAO;
        this.apiManager = apiManager;
        this.imgUtil = imgUtil;
        this.userDAO = userDAO;
        this.roleUtil = roleUtil;
        this.webPageManager = webPageManager;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
    }

    @RequestMapping(value = "/userinfo/{username}", method = RequestMethod.GET)
    public String userInfo(@PathVariable String username, @RequestParam("start") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate start,
                           @RequestParam("limit") int limit) {
        //去osu api验证用户名是否存在
        Userinfo now = apiManager.getUser(username, null);
        if (now == null) {
            return new Gson().toJson(new WebResponse<>("user not found", null));
        }
        //取到最接近8.29的那条记录
        Userinfo earliest = userInfoDAO.getNearestUserInfo(now.getUserId(), LocalDate.of(2017, 8, 29));
        if (earliest == null) {
            return new Gson().toJson(new WebResponse<>("user not registered", null));
        }
        //判断传入日期是否比最早的记录早，是则返回错误+最早的记录时间
        if (start.isBefore(earliest.getQueryDate())) {
            return new Gson().toJson(new WebResponse<>("start date is too early", earliest.getQueryDate()));
        }
        //判断最早的记录+传入的天数是否比今天晚
        if (start.plusDays(limit - 1).isAfter(LocalDate.now())) {
            return new Gson().toJson(new WebResponse<>("end date is too late", null));
        }
        List<Userinfo> list = new ArrayList<>();
        for (long i = 0; i < limit; i++) {
            Userinfo tmp = userInfoDAO.getUserInfo(now.getUserId(), start.plusDays(i));
            list.add(tmp);
        }

        return new Gson().toJson(new WebResponse<>("ok", list));
    }

    @RequestMapping(value = "/stat/{uid}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public void getStat(HttpServletResponse response, @PathVariable Integer uid) {
        String role;
        Integer scoreRank;
        User user = userDAO.getUser(null, uid);
        if (user == null) {
            role = "creep";
        } else {
            List<String> list = roleUtil.sortRoles(user.getRole());
            role = list.get(0);
        }

        Userinfo userFromAPI = apiManager.getUser(null, uid);
        if (userFromAPI == null) {
            return;
        }
        //获取score rank
        //gust？
        if (userFromAPI.getUserId() == 1244312
                || userFromAPI.getUserId() == 6149313
                || userFromAPI.getUserId() == 3213720
                //MFA
                || userFromAPI.getUserId() == 6854920) {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 10000);
        } else {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        String result = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
        byte[] bytes = Base64.getDecoder().decode(result);
        response.setContentType("image/png");
        try (InputStream in = new ByteArrayInputStream(bytes);
             OutputStream out = response.getOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            BufferedImage img2 = new BufferedImage(600, 288, BufferedImage.TYPE_INT_RGB);
            img2.getGraphics().drawImage(img.getScaledInstance(600, 288, Image.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(img2, "png", out);
            out.write(bytes);
        } catch (IOException ignore) {

        }
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