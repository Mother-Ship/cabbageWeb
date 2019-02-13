package top.mothership.cabbage.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UpdateOsuClientTasker {
    @Scheduled(cron = "0 0/10 * * * ?")
    public void updateOsuClient(){

    }
}
