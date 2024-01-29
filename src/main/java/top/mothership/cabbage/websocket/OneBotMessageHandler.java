package top.mothership.cabbage.websocket;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.mothership.cabbage.controller.CqController;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.OneBotApiRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class OneBotMessageHandler extends TextWebSocketHandler {
    @Autowired
    private CqController cqController;
    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100);

    private Logger log = LogManager.getLogger(this.getClass());

    //用来保存连接进来session
    private Map<String, WebSocketSession> map = new ConcurrentHashMap<>();
    private Map<String, String> cqResponseMap = new ConcurrentHashMap<>();
    /**
     * 关闭连接进入这个方法处理，将session从 list中删除
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        map.remove(getClientQQ(session));
        log.info("{} 连接已经关闭，现从list中删除 ,状态信息{}", session, status);
    }

    /**
     * 三次握手成功，进入这个方法处理，将session 加入list 中
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        map.put(getClientQQ(session), session);
        log.info("用户{}连接成功.... ", session);
    }

    /**
     * 处理客户发送的信息
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        log.info("收到来自客户端的信息: {}", message.getPayload());
        if (message.getPayload().toString().contains("echo")
                && !message.getPayload().toString().contains("post_type")) {
            // 这是onebot api调用的回复
            CqResponse response = new Gson().fromJson(message.getPayload().toString(), CqResponse.class);
            cqResponseMap.put(response.getEcho(), message.getPayload().toString());

        } else {
            CqMsg cqMsg = new Gson().fromJson(message.getPayload().toString(), CqMsg.class);
            if (!StringUtils.isEmpty(cqMsg.getRawMessage())){
                cqMsg.setMessage(cqMsg.getRawMessage());
            }
            fixedThreadPool.submit(() -> cqController.doHandle(cqMsg));
        }

    }

    private String getClientQQ(WebSocketSession session) {
        // 按one bot文档，取第一个X-Self-ID请求头
        return session.getHandshakeHeaders().get("X-Self-ID").get(0);
    }

    @SneakyThrows
    public String callApi(OneBotApiRequest request){
        WebSocketSession session = map.get(String.valueOf(request.getMsg().getSelfId()));
        if (session != null){
            session.sendMessage(new TextMessage(new Gson().toJson(request)));
        }
        // 自旋等待返回值map里出现要的返回值，直到次数上限
        int retry = 0;
        while(true){
            String response = cqResponseMap.get(request.getEcho());
            if (response != null){
                return response;
            }
            TimeUnit.SECONDS.sleep(1);

            retry ++;
            if (retry > 10){
                return null;
            }
        }
    }
    @SneakyThrows
    public void sendMessage(CqMsg cqMsg) {
        String action = null;
        switch (cqMsg.getMessageType()) {
            case "group":
                action = "send_group_msg";
                break;
            case "discuss":
                action = "send_discuss_msg";
                break;
            case "private":
                action = "send_private_msg";
                break;
            case "smoke":
                action = "set_group_ban";
                break;
            case "smokeAll":
                action = "set_group_whole_ban";
                break;
            case "handleInvite":
                action = "set_group_add_request";
                break;
            case "kick":
                action = "set_group_kick";
                break;
            default:
                return;
        }
        
        WebSocketSession session = map.get(String.valueOf(cqMsg.getSelfId()));
        if (session != null){
            OneBotApiRequest request = new OneBotApiRequest();
            request.setMsg(cqMsg);
            request.setAction(action);
            session.sendMessage(new TextMessage(new Gson().toJson(request)));
        }
    }

}
