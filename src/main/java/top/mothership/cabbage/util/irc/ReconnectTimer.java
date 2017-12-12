package top.mothership.cabbage.util.irc;


import java.io.IOException;

public class ReconnectTimer extends Thread {
	private static final long MESSAGE_TIMEOUT = 100 * 1000;
	private static final long PING_TIMEOUT = 128 * 1000;

	private final Object lockObject = new Object();
	private final IRCClient ircClient;

	private long    lastMessageAt = System.currentTimeMillis();
	private boolean waitingForPong = false;
	private long    pingSentAt = 0;

	public ReconnectTimer(IRCClient ircClient) {
		this.ircClient = ircClient;
	}

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
