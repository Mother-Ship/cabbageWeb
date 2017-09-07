package top.mothership.cabbage.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.pojo.CqRequest;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;

@RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
@RestController
public class CqController {
    @Autowired
    private CqServiceImpl cqService;
    public void cqMsgPrase(@RequestBody CqRequest cqRequest){
        //待整理业务逻辑
    }

}
