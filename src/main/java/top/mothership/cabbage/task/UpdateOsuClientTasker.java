package top.mothership.cabbage.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pojo.coolq.osu.ClientFile;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UpdateOsuClientTasker {
    public static volatile boolean IS_UPDAING = false;
    private WebPageManager webPageManager;

    @Autowired
    public void setWebPageManager(WebPageManager webPageManager) {
        this.webPageManager = webPageManager;
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void updateOsuClient() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        //请求 获取客户端信息
        List<ClientFile> clientFileList = webPageManager.getOsuClientInfo();
        if (clientFileList !=null){
            //筛选出最新的一个文件
            LocalDateTime time = LocalDateTime.MIN;
            for (ClientFile clientFile : clientFileList) {
                LocalDateTime time2 = LocalDateTime.from(df.parse(clientFile.getTimestamp()));
                if(time2.isAfter(time)){
                    time = time2;
                }
            }
            System.out.println(time);
            //和指定文本中存储的时间进行对比
            //如果有更新，对所有获取到的文件进行下载并且存储，并且将标志变量存储为TRUE

        }


    }
}
