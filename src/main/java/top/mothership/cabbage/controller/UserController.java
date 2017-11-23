package top.mothership.cabbage.controller;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.WebResponse;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.serviceImpl.UserServiceImpl;
import top.mothership.cabbage.util.osu.ApiUtil;

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
    private final ApiUtil apiUtil;
    @Autowired
    public UserController(UserServiceImpl userService, UserInfoDAO userInfoDAO, ApiUtil apiUtil) {
        this.userService = userService;
        this.userInfoDAO = userInfoDAO;
        this.apiUtil = apiUtil;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
    }

    @RequestMapping(value = "/userinfo/{username}", method = RequestMethod.GET)
//    @CrossOrigin(origins = "http://localhost")
    public String userInfo(@PathVariable String username, @RequestParam("start") @DateTimeFormat(pattern="yyyyMMdd")Date start, @RequestParam("limit") int limit) {
        Userinfo now = apiUtil.getUser(username,null);
        if (now == null){
            return new Gson().toJson(new WebResponse<>("user not found", null));
        }
        if(new Date(start.getTime()+(1000L*3600L*24L*(long)(limit-1))).after(Calendar.getInstance().getTime())){
            return new Gson().toJson(new WebResponse<>("end date is too late", null));
        }
        if(start.before(new Date(1503936000000L))){
            return new Gson().toJson(new WebResponse<>("start date is too early", null));
        }
        List<Userinfo> list = new ArrayList<>();
        for(int i=0;i<limit;i++){
            Userinfo tmp = userInfoDAO.getUserInfo(now.getUserId(),new java.sql.Date(start.getTime()+(1000*3600*24*i)));
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