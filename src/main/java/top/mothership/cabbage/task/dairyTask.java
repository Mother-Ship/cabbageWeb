package top.mothership.cabbage.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class dairyTask {
    @Scheduled(cron="0 0 4 * * ?")
    public void importUserInfo(){

    }
}
