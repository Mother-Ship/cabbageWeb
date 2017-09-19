package top.mothership.cabbage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
public class UploadController {
    @RequestMapping(value = "/uploadFile",method = RequestMethod.POST)
    public void upload(@RequestParam(value = "myfile", required = true) MultipartFile file, HttpServletRequest request,
                       HttpServletResponse response)
    {
        String path = "C:\\CoolQ Pro\\data\\image\\resource\\db\\";
        String fileName = file.getOriginalFilename();
        System.out.println("文件上传路径为：" + path);
        File targetFile = new File(path, fileName);
        if (!targetFile.exists())
        {
            targetFile.mkdirs();
        }
        // 保存
        try
        {
            file.transferTo(targetFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}