package top.mothership.cabbage.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.pojo.CqResponse;
import top.mothership.cabbage.service.CqService;
import top.mothership.cabbage.util.*;

@Service
public class CqServiceImpl implements CqService {

    private final ApiUtil apiUtil;
    private final CqUtil cqUtil;
    private final ImgUtil imgUtil;
    private final SmokeUtil smokeUtil;
    private final WebPageUtil webPageUtil;
    private BaseMapper baseMapper;
    @Autowired
    public CqServiceImpl(ApiUtil apiUtil, CqUtil cqUtil, ImgUtil imgUtil, SmokeUtil smokeUtil, WebPageUtil webPageUtil, BaseMapper baseMapper) {
        this.apiUtil = apiUtil;
        this.cqUtil = cqUtil;
        this.imgUtil = imgUtil;
        this.smokeUtil = smokeUtil;
        this.webPageUtil = webPageUtil;
        this.baseMapper = baseMapper;
    }


    @Override
    public void praseMsg(CqMsg cqMsg) {
        //业务逻辑
       CqResponse cqResponse =  cqUtil.sendMsg(cqMsg);

    }

    @Override
    public void praseNewsPaper(CqMsg cqMsg) {

    }

    @Override
    public void praseGroupInvite(CqMsg cqMsg) {

    }
}
