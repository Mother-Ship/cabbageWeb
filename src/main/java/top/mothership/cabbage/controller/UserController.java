package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.serviceImpl.UserServiceImpl;

@RestController
@RequestMapping(value = "/api", produces = {"application/json;charset=UTF-8"})
public class UserController {
    private final UserServiceImpl userService;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }




    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
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