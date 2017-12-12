package top.mothership.cabbage.util.irc;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class IRCClient extends Thread{
    private static final int DEFAULT_DELAY = 200;
    private final String address = "irc.ppy.sh";
    private final int port = 6667;
    private final String user = "Mother_Ship";
    private final String password = "94af15ce";
    private ReconnectTimer reconnectTimer;
    private Socket socket;
    private PrintStream outStream;
    private boolean shouldStop;
    private RateLimitedFlusher flusher;
    private Map<String, RateLimitedChannel> channels;
    private int delay;
    private boolean disconnected;


    public IRCClient() {
        channels = new HashMap<>();
        delay = DEFAULT_DELAY;
        disconnected = true;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public RateLimitedChannel getChannel(String channel) {
        return channels.get(channel);
    }

    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    Map<String, RateLimitedChannel> getChannels() {
        return channels;
    }
    @PostConstruct
    public void doStart(){
        this.start();
    }

    @Override
    public void run(){
        connect();
    }

    public void connect() {
        if (!disconnected) {
            System.out.println("Attempt to connect the IRCClient without first disconnecting.");
            return;
        }
        if (reconnectTimer == null) {
            reconnectTimer = new ReconnectTimer(this);
        }
        while (true) {
            reconnectTimer.messageReceived();
            try {
                //新建socket，获取输出流，新建一个Flusher
                socket = new Socket(address, port);
                outStream = new PrintStream(socket.getOutputStream());
                //打开flusher
                //flusher好像是一个线程
                flusher = new RateLimitedFlusher(this, delay);
                flusher.start();
                disconnected = false;
                //发送用户名和密码
                write("PASS" + " " + password, true);
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
                    long now = System.currentTimeMillis();
                    reconnectTimer.messageReceived();
//对消息内容进行判断，001是登录，ping和pong应该是心跳包，其他的就是正常消息
                    if (!msg.contains("cho@ppy.sh QUIT")) {
                        if (msg.contains("001")) {
                            System.out.println("Logged in");
                            System.out.println("Line: " + msg);
                        } else if (msg.startsWith("PING")) {
                            String pingResponse = msg.replace("PING", "PONG");
                            write(pingResponse);
                        } else if (msg.startsWith("PONG")) {
                            System.out.println("Got pong at " + now + ": " + msg);
                        } else {
                            System.out.println("RECV(" + new Date(now) + "): " + msg);
                            try {
                                //处理逻辑
                                log(msg);

                            } catch (Exception e) {
                                System.err.println("Unhandled exception thrown!");
                                e.printStackTrace();
                            }
                        }
                    }
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

    public void disconnect() throws IOException {
        if (disconnected) {
            System.out.println("Attempt to disconnect without first connecting.");
            return;
        }
        flusher.interrupt();
        outStream.close();
        socket.close();
        disconnected = true;
    }

    public void write(String message) {
        write(message, false);
    }

    /**censor。。如果是true就不在控制台输出。。
     *
     * @param message
     * @param censor
     */
    private void write(String message, boolean censor) {
        if (!censor && !message.startsWith("PING") && !message.startsWith("PONG")) {
            System.out.println("SEND(" + new Date(System.currentTimeMillis()) + "): " + message);
        }
        outStream.println(message);
    }

    public void sendMessage(String channel, String message) {
        synchronized (channels) {
            if (!channels.containsKey(channel)) {
                channels.put(channel, new RateLimitedChannel(channel, delay));
            }

            channels.get(channel).addMessage(message);
        }
    }
    public void log(String line) {
        Pattern endOfMotd = Pattern.compile(":cho.ppy.sh 376 (.+)");
        Matcher endofmotdmatch = endOfMotd.matcher(line);
        try {
            if (endofmotdmatch.matches()) {
                //如果消息符合376，表示登录成功
                System.out.println("End of motd, we're connected.");
//                for (Lobby lobby : m_lobbies.values()) {
//                    m_lobbies.remove(lobby.channel);
//                    reconnectLobby(lobby);
//                }
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
//        // :cho.ppy.sh 401 AutoHost #mp_32349656 :No such nick
//        // :cho.ppy.sh 401 AutoHost #mp_35465451 :No such nick
//        Pattern ChannelNo = Pattern.compile(":cho.ppy.sh 401 (.+) #mp_(.+) :No such nick");
//        Matcher channelded = ChannelNo.matcher(line);
//        if (channelded.matches()) {
//            if (m_lobbies.containsKey("#mp_" + channelded.group(2))) {
//                Lobby lobby = m_lobbies.get("#mp_" + channelded.group(2));
//                if (lobby.channel.equalsIgnoreCase("#mp_" + channelded.group(2))) {
//                    lobby.timer.stopTimer();
//                    removeLobby(lobby);
//                    lobby = null;
//                }
//            }
//            if (m_permanentLobbies.containsKey("#mp_" + channelded.group(2))) {
//                Lobby lobby = m_permanentLobbies.get("#mp_" + channelded.group(2)).lobby;
//                m_permanentLobbies.get("#mp_" + channelded.group(2)).stopped = true;
//                m_permanentLobbies.get("#mp_" + channelded.group(2)).lobby.timer.stopTimer();
//                m_permanentLobbies.remove("#mp_" + channelded.group(2));
//                createNewLobby(lobby.name, lobby.minDifficulty, lobby.maxDifficulty, lobby.creatorName, lobby.OPLobby,
//                        true);
//                lobby = null;
//            }
//        }
//
//        // RECV(Mon Oct 23 16:14:35 ART 2017): :BanchoBot!cho@ppy.sh PRIVMSG AutoHost :
//        // You cannot create any more tournament matches. Please close any previous
//        // tournament matches you have open.
//        Pattern staph = Pattern.compile(
//                ":BanchoBot!cho@ppy.sh PRIVMSG AutoHost :You cannot create any more tournament matches. Please close any previous tournament matches you have open.");
//        Matcher staphM = staph.matcher(line);
//        if (staphM.matches()) {
//            if (staphM.group(1).equalsIgnoreCase(m_client.getUser())) {
//                String lobbyChannel = staphM.group(2);
//                noMore(lobbyChannel);
//                System.out.println("Lobby cancelled due to limit: " + lobbyChannel);
//                return;
//            }
//        }
//
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
//        // :HyPeX!cho@ppy.sh JOIN :#mp_29904363
//        Pattern pattern = Pattern.compile(":(.+)!cho@ppy.sh JOIN :(.+)");
//        Matcher matcher = pattern.matcher(line);
//        if (matcher.matches()) {
//            if (matcher.group(1).equalsIgnoreCase(m_client.getUser())) {
//                String lobbyChannel = matcher.group(2);
//                newLobby(lobbyChannel);
//                System.out.println("New lobby: " + lobbyChannel);
//            }
//        }
//
//        // :AutoHost!cho@ppy.sh PART :#mp_35457515
//        Pattern part = Pattern.compile(":(.+)!cho@ppy.sh PART :(.+)");
//        Matcher partM = part.matcher(line);
//        if (partM.matches()) {
//            if (partM.group(1).equalsIgnoreCase(m_client.getUser())) {
//                if (m_lobbies.containsKey("#mp_" + partM.group(2))) {
//                    Lobby lobby = m_lobbies.get("#mp_" + partM.group(2));
//                    if (lobby.channel.equalsIgnoreCase("#mp_" + partM.group(2))) {
//                        lobby.timer.stopTimer();
//                        removeLobby(lobby);
//                    }
//                }
//                if (m_permanentLobbies.containsKey("#mp_" + partM.group(2))) {
//                    Lobby lobby = m_permanentLobbies.get("#mp_" + partM.group(2)).lobby;
//                    m_permanentLobbies.get("#mp_" + partM.group(2)).stopped = true;
//                    m_permanentLobbies.remove("#mp_" + partM.group(2));
//                    createNewLobby(lobby.name, lobby.minDifficulty, lobby.maxDifficulty, lobby.creatorName,
//                            lobby.OPLobby, true);
//                }
//            }
//        }
    }

}
