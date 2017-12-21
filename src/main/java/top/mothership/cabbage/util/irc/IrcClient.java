package top.mothership.cabbage.util.irc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.serviceImpl.MpServiceImpl;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;

/**
 * IRC工具类。感谢AutoHost项目。
 *
 * @author HyPeX
 */
@Component
public class IrcClient extends Thread{
    private static final int DEFAULT_DELAY = 200;
    private ReconnectTimer reconnectTimer;
    private Socket socket;
    private PrintStream outStream;
    private boolean shouldStop;
    private RateLimitedFlusher flusher;
    private Map<String, RateLimitedChannel> channels = new HashMap<>();
    private int delay = DEFAULT_DELAY;
    private boolean disconnected = true;
    private Logger logger = LogManager.getLogger(this.getClass());
//    private final MpServiceImpl mpService;


//    @Autowired
//    public IrcClient(MpServiceImpl mpService) {
//        this.mpService = mpService;
//    }


    /**
     * Is disconnected boolean.
     *
     * @return the boolean
     */
    public boolean isDisconnected() {
        return disconnected;
    }

    /**
     * Gets channel.
     *
     * @param channel the channel
     * @return the channel
     */
    public RateLimitedChannel getChannel(String channel) {
        return channels.get(channel);
    }

    /**
     * Gets input stream.
     *
     * @return the input stream
     * @throws IOException the io exception
     */
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    /**
     * Gets channels.
     *
     * @return the channels
     */
    Map<String, RateLimitedChannel> getChannels() {
        return channels;
    }

    @PostConstruct
    public void doStart(){
        this.start();
    }

    @Override
    public void run() {
        try {
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connect.
     */
    public void connect() throws IOException {

        if (reconnectTimer == null) {
            reconnectTimer = new ReconnectTimer(this);
        }
        while (true) {
            reconnectTimer.messageReceived();
            try {
                if (!disconnected) {
                    disconnect();
                }
                //新建socket，获取输出流，新建一个Flusher
                String address = "irc.ppy.sh";
                int port = 6667;
                socket = new Socket(address, port);
                outStream = new PrintStream(socket.getOutputStream());
                //打开flusher
                //flusher好像是一个线程
                flusher = new RateLimitedFlusher(this, delay);
                flusher.start();
                disconnected = false;
                //发送用户名和密码
                String password = "94af15ce";
                write("PASS" + " " + password, true);
                String user = "Mother_Ship";
                write("NICK" + " " + user);
                write("USER" + " " + user);
                //获取输入流
                BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));

                String msg;
                boolean safeShouldStop;
                synchronized (this) {
                    safeShouldStop = shouldStop;
                }
                //当输入流有内容，并且safe的值是false（推测是防止禁言）
                while ((msg = reader.readLine()) != null && !safeShouldStop) {
                    //通知ping线程，有消息抵达
                    reconnectTimer.messageReceived();
                    //传入消息处理函数
                    if (msg.startsWith("PING")) {
                        String pingResponse = msg.replace("PING", "PONG");
                        write(pingResponse);
                        return;
                    }
                    handleMsg(msg);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (this) {
                shouldStop = false;
            }

        }

    }

    /**
     * Reconnect.
     */
    public void reconnect() {
        try {
            if (!isDisconnected()) {
                disconnect();
            }
        } catch (IOException e) {
            // Do nothing.
        }
        synchronized (this) {
            shouldStop = true;
        }
    }

    /**
     * Disconnect.
     *
     * @throws IOException the io exception
     */
    public void disconnect() throws IOException {
        if (disconnected) {
            logger.info("注销失败：尚未登录");
            return;
        }
        flusher.interrupt();
        outStream.close();
        socket.close();
        disconnected = true;
    }

    /**
     * Write.
     *
     * @param message the message
     */
    public void write(String message) {
        write(message, false);
    }

    /**
     * censor。。如果是true就不在控制台输出。。
     *
     * @param message
     * @param censor
     */
    private void write(String message, boolean censor) {
        if (!censor && !message.startsWith("PING") && !message.startsWith("PONG")) {
            logger.info("已发送消息：" + message);
        }
        outStream.println(message);
    }

    /**
     * Send message.
     *
     * @param channel the channel
     * @param message the message
     */
    public void sendMessage(String channel, String message) {
        synchronized (channels) {
            if (!channels.containsKey(channel)) {
                channels.put(channel, new RateLimitedChannel(channel, delay));
            }

            channels.get(channel).addMessage(message);
        }
    }

    public void handleMsg(String msg) {

        //对消息内容进行判断，001是登录，ping和pong应该是心跳包，其他的就是正常消息
        if (!msg.contains("cho@ppy.sh QUIT")) {
            if (msg.startsWith("PONG")) {
                logger.info("收到pong消息。");
            } else {
                //处理消息
                Matcher serverMsg = PatternConsts.IRC_SERVER_MSG.matcher(msg);
                Matcher regularMsg = PatternConsts.IRC_PRIVATE_MSG.matcher(msg);
                if (serverMsg.find()) {
                    switch (serverMsg.group(1)) {
                        case "001":
                            logger.info("开始登录……");
                            break;
                        case "376":
                            logger.info("登录成功！");
//                            mpService.reconnectAllLobby();
                            break;
                        case "401":
                            // :cho.ppy.sh 401 AutoHost #mp_32349656 :No such nick
                            logger.info("指定的房间已关闭：" + msg);
                            Matcher shutdownLobby = PatternConsts.ROOM_NOT_EXIST.matcher(msg);
//                            mpService.shutdownLobby(Integer.valueOf(shutdownLobby.group(1)));
                            break;
                        default:
                            break;
                    }

                } else if (regularMsg.find()) {
                    logger.info("收到私聊消息：" + msg);
                    // RECV(Mon Oct 23 16:14:35 ART 2017): :BanchoBot!cho@ppy.sh PRIVMSG AutoHost :
                    // You cannot create any more tournament matches. Please close any previous
                    // tournament matches you have open.

                    // :HyPeX!cho@ppy.sh JOIN :#mp_29904363、
                    //加入房间频道之后，刷新房间状态

//                    if (matcher.matches()) {
//                        if (matcher.group(1).equalsIgnoreCase(m_client.getUser())) {
//                            String lobbyChannel = matcher.group(2);
//                            newLobby(lobbyChannel);
//                            System.out.println("New lobby: " + lobbyChannel);
//                        }
//                    }

                    // :AutoHost!cho@ppy.sh PART :#mp_35457515
//                    group1:登录用户名 group2：房间频道名
//                    if (partM.matches()) {
//                        if (partM.group(1).equalsIgnoreCase(m_client.getUser())) {
                    //如果是bancho发给登录号的消息
//                            if (m_lobbies.containsKey("#mp_" + partM.group(2))) {
                    //如果房间数据库里有消息体的房间
//                                Lobby lobby = m_lobbies.get("#mp_" + partM.group(2));
//                                if (lobby.channel.equalsIgnoreCase("#mp_" + partM.group(2))) {
                    //停止计时器 并且移除它
//                                    lobby.timer.stopTimer();
//                                    removeLobby(lobby);
//                                }
//                            }
                    //如果是一个常住房间，重新开启它

//                            if (m_permanentLobbies.containsKey("#mp_" + partM.group(2))) {
//                                Lobby lobby = m_permanentLobbies.get("#mp_" + partM.group(2)).lobby;
//                                m_permanentLobbies.get("#mp_" + partM.group(2)).stopped = true;
//                                m_permanentLobbies.remove("#mp_" + partM.group(2));
//                                createNewLobby(lobby.name, lobby.minDifficulty, lobby.maxDifficulty, lobby.creatorName,
//                                        lobby.OPLobby, true);
//                            }
//                        }
//                    }
                } else {
                    logger.info("出现了不能理解的消息：" + msg);
                }
            }
        }


//        Pattern channel = Pattern.compile(":(.+)!cho@ppy.sh PRIVMSG (.+) :(.+)");
//        Matcher channelmatch = channel.matcher(line);
//        if (channelmatch.find()) {
//            // :AutoHost!cho@ppy.sh PRIVMSG #lobby :asd
//            String user = channelmatch.group(1);
//            String target = channelmatch.group(2);
//            String message = channelmatch.group(3);
//            if (target.startsWith("#")) {
//                new ChannelMessageHandler(this).handle(target, user, message);
//            } else {
//                if (LOCK_NAME != null && !user.equalsIgnoreCase(LOCK_NAME) && !user.equalsIgnoreCase("BanchoBot")) {
//                    m_client.sendMessage(user, "hypex is currently testing / fixing AutoHost. "
//                            + "He'll announce in the [https://discord.gg/UDabf2y AutoHost Discord] when he's done");
//                } else {
//                    new PrivateMessageHandler(this).handle(user, message);
//                }
//            }
//        }
//
    }

    /**
     * The type Rate limited channel.
     */
    class RateLimitedChannel {
        private int delay;
        private String channel;
        private Queue<String> messages;
        private long lastSentTime;

        /**
         * Instantiates a new Rate limited channel.
         *
         * @param channel the channel
         * @param delay   the delay
         */
        RateLimitedChannel(String channel, int delay) {
            this.channel = channel;
            this.delay = delay;
            this.messages = new LinkedList<>();
        }

        /**
         * Add message.
         *
         * @param message the message
         */
        void addMessage(String message) {
            messages.add(message);
        }

        /**
         * Poll string.
         *
         * @return the string
         */
        String poll() {
            try {
                long currentTime = System.currentTimeMillis();
                if ((currentTime - lastSentTime) >= delay) {
                    String msg = messages.poll();
                    if (msg != null) {
                        lastSentTime = currentTime;
                        return "PRIVMSG " + channel + " " + msg;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Has next boolean.
         *
         * @return the boolean
         */
        boolean hasNext() {
            return !messages.isEmpty();
        }
    }

    /**
     * The type Rate limited flusher.
     */
    class RateLimitedFlusher extends Thread {
        private IrcClient m_client;
        private int m_delay;

        /**
         * Instantiates a new Rate limited flusher.
         *
         * @param client the client
         * @param delay  the delay
         */
        RateLimitedFlusher(IrcClient client, int delay) {
            m_client = client;
            m_delay = delay;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //拿到迭代器，锁住它
                    Iterator<RateLimitedChannel> it = m_client.getChannels().values().iterator();
                    synchronized (it) {
                        //把所有的频道拿出来
                        while (it.hasNext()) {

                            RateLimitedChannel limiter = it.next();
                            //从这个频道的队列中取出消息
                            String line = limiter.poll();
                            if (line != null)
                            //写入输入流
                            {
                                m_client.write(line);
                            }
                        }
                    }
                    it = null;
                    Thread.sleep(1000);
                } catch (ConcurrentModificationException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * The type Reconnect timer.
     */
    class ReconnectTimer extends Thread {
        private static final long MESSAGE_TIMEOUT = 100 * 1000;
        private static final long PING_TIMEOUT = 128 * 1000;

        private final Object lockObject = new Object();
        private final IrcClient ircClient;

        private long lastMessageAt = System.currentTimeMillis();
        private boolean waitingForPong = false;
        private long pingSentAt = 0;

        /**
         * Instantiates a new Reconnect timer.
         *
         * @param ircClient the irc client
         */
        public ReconnectTimer(IrcClient ircClient) {
            this.ircClient = ircClient;
        }

        /**
         * Message received.
         */
        public void messageReceived() {
            synchronized (lockObject) {
                //登记最后的消息抵达时间，标记“不需要等待pong”
                waitingForPong = false;
                lastMessageAt = System.currentTimeMillis();
            }
        }

        @Override
        public void run() {
            while (true) {
                //锁住某个莫名的对象
                synchronized (lockObject) {
                    long now = System.currentTimeMillis();
                    //如果在等待pong
                    if (waitingForPong) {
                        if (now - pingSentAt > PING_TIMEOUT) {
                            //并且当前时间和ping发送的时间大于超时时间
                            System.out.println("Bancho didn't reply to our ping. Reconnecting.");
                            waitingForPong = false;
                            lastMessageAt = System.currentTimeMillis();
                            //重连
                            ircClient.reconnect();
                        }
                    } else {
                        if (now - lastMessageAt > MESSAGE_TIMEOUT) {
                            //如果最后一条消息和现在的间隔超时，就发送一个ping消息
                            System.out.println("Sending a ping! (" + now + ")");
                            waitingForPong = true;
                            pingSentAt = now;
                            ircClient.write("PING " + now);
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }

}
