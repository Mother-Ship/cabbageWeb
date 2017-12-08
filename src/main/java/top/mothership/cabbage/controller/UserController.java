package top.mothership.cabbage.controller;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.WebResponse;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.serviceImpl.UserServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {"application/json;charset=UTF-8"})
public class UserController {
    private final UserServiceImpl userService;
    private Logger logger = LogManager.getLogger(this.getClass());
    private final UserInfoDAO userInfoDAO;
    private final ApiManager apiManager;
    @Autowired
    public UserController(UserServiceImpl userService, UserInfoDAO userInfoDAO, ApiManager apiManager) {
        this.userService = userService;
        this.userInfoDAO = userInfoDAO;
        this.apiManager = apiManager;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
    }

    @RequestMapping(value = "/userinfo/{username}", method = RequestMethod.GET)
//    @CrossOrigin(origins = "http://localhost")
    public String userInfo(@PathVariable String username, @RequestParam("start") @DateTimeFormat(pattern="yyyyMMdd")LocalDate start,
                           @RequestParam("limit") int limit) {
        //去osu api验证用户名是否存在
        Userinfo now = apiManager.getUser(username,null);
        if (now == null) {
            return new Gson().toJson(new WebResponse<>("user not found", null));
        }
        //取到最接近8.29的那条记录
        Userinfo earliest = userInfoDAO.getNearestUserInfo(now.getUserId(), LocalDate.of(2017,8,29));
        if (earliest == null) {
            return new Gson().toJson(new WebResponse<>("user not registered", null));
        }
        //判断传入日期是否比最早的记录早，是则返回错误+最早的记录时间
        if(start.isBefore(earliest.getQueryDate())) {
            return new Gson().toJson(new WebResponse<>("start date is too early", earliest.getQueryDate()));
        }
        //判断最早的记录+传入的天数是否比今天晚
        if(start.plusDays(limit-1).isAfter(LocalDate.now())) {
            return new Gson().toJson(new WebResponse<>("end date is too late", null));
        }
        List<Userinfo> list = new ArrayList<>();
        for(long i=0;i<limit;i++){
            Userinfo tmp = userInfoDAO.getUserInfo(now.getUserId(),start.plusDays(i));
            list.add(tmp);
        }

        return new Gson().toJson(new WebResponse<List<Userinfo>>("ok",list));
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