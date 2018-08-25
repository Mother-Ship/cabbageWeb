package top.mothership.cabbage.service;

import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.UserAuthorityControl;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@UserAuthorityControl({1335734657L})
public class ShadowSocksCmdServiceImpl {
    private final String baseURL = "https://cmd.gogosu.moe/api/";
    private final CqManager cqManager;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public ShadowSocksCmdServiceImpl(CqManager cqManager) {
        this.cqManager = cqManager;
    }

    public void service(CqMsg cqMsg) {
        if ("getcode".equals(cqMsg.getArgument().getSubCommandLowCase()))
            cqMsg.getArgument().setSubCommandLowCase("code");
        HttpURLConnection httpConnection;
        try {
            httpConnection =
                    (HttpURLConnection) new URL(baseURL + cqMsg.getArgument().getSubCommandLowCase()).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            //防止转义
            //折腾了半天最后是少了UTF-8………………我tm想给自己一巴掌

            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg.getArgument().getSsr()).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //这里不用用到下划线转驼峰
            cqMsg.setMessage(tmp2.toString());
            cqManager.sendMsg(cqMsg);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }
}
