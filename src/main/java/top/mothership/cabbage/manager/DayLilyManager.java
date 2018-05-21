package top.mothership.cabbage.manager;

import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class DayLilyManager {
    private final String baseURL = "http://123.206.100.246:23333/api/getresponse";
    private Logger logger = LogManager.getLogger(this.getClass());

    public void sendMsg(CqMsg cqMsg) {
        HttpURLConnection httpConnection;
        try {
            httpConnection =
                    (HttpURLConnection) new URL(baseURL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
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
            logger.debug(tmp2.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
