package top.mothership.cabbage.irc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class IRCClient {
	private static final int DEFAULT_DELAY = 200;

	private final String address;
	private final int port;
	private final String user;
	private final String password;

	private Socket socket;
	private PrintStream outStream;

	private RateLimitedFlusher flusher;
	private Map<String, RateLimitedChannel> channels;
	private int delay;

	private boolean disconnected;

	public IRCClient(String user, String password) {
		this.address = "irc.ppy.sh";
		this.port = 6667;
		this.user = "Mother_Ship";
		this.password = "94af15ce";

		channels = new HashMap<>();
		delay = DEFAULT_DELAY;

		disconnected = true;
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	public void setDelay(int delay) {
		delay = delay;
	}

	public RateLimitedChannel getChannel (String channel) {
		return channels.get(channel);
	}
	
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	public String getUser() {
		return user;
	}

	Map<String, RateLimitedChannel> getChannels() {
		return channels;
	}

	public void connect() throws IOException {
		if (!disconnected) {
			System.out.println("Attempt to connect the IRCClient without first disconnecting.");
			return;
		}

		socket = new Socket(address, port);
		outStream = new PrintStream(socket.getOutputStream());

		flusher = new RateLimitedFlusher(this, delay);
		flusher.start();

		disconnected = false;

		register();
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

	private void write(String message, boolean censor) {
		if (!censor && !message.startsWith("PING") && !message.startsWith("PONG")) {
			System.out.println("SEND(" + new Date(System.currentTimeMillis()) + "): " + message);
		}
		outStream.println(message);
	}

	private void register() {
		write("PASS" + " " + password, true);
		write("NICK" + " " + user);
		write("USER" + " " + user);
	}

	public void sendMessage(String channel, String message) {
		synchronized (channels) {
			if (!channels.containsKey(channel)) {
				channels.put(channel, new RateLimitedChannel(channel, delay));
			}

			channels.get(channel).addMessage(message);
		}
	}
}
