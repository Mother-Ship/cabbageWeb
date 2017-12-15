package top.mothership.cabbage.util.irc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.Around;
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
public class IrcClient extends Thread {
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
    private final MpServiceImpl mpService;

    @Autowired
    public IrcClient(MpServiceImpl mpService) {
        this.mpService = mpService;
    }


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

    /**
     * Do start.
     */
    @PostConstruct
    public void doStart() {
        this.start();
    }

    @Override
    public void run() {
        connect();
    }

    /**
     * Connect.
     */
    public void connect() {
        if (!disconnected) {
            logger.info("已经登录，请先退出登录");
            return;
        }
        if (reconnectTimer == null) {
            reconnectTimer = new ReconnectTimer(this);
        }
        while (true) {
            reconnectTimer.messageReceived();
            try {
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
                    mpService.handleMsg(msg);
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
