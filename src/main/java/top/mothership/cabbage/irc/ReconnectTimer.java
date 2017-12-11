package top.mothership.cabbage.irc;


public class ReconnectTimer extends Thread {
	private static final long MESSAGE_TIMEOUT = 100 * 1000;
	private static final long PING_TIMEOUT = 128 * 1000;

	private final Object lockObject = new Object();
	private final IRCBot bot;

	private long    lastMessageAt = System.currentTimeMillis();
	private boolean waitingForPong = false;
	private long    pingSentAt = 0;

	public ReconnectTimer(IRCBot bot) {
		this.bot = bot;
	}

	public void messageReceived() {
		synchronized (lockObject) {
			waitingForPong = false;
			lastMessageAt = System.currentTimeMillis();
		}
	}

	@Override
	public void run() {
		while (true) {
			synchronized (lockObject) {
				long now = System.currentTimeMillis();
				if (waitingForPong) {
					if (now - pingSentAt > PING_TIMEOUT) {
						System.out.println("Bancho didn't reply to our ping. Reconnecting.");
						waitingForPong = false;
						lastMessageAt = System.currentTimeMillis();
//						bot.reconnect();
					}
				} else {
					if (now - lastMessageAt > MESSAGE_TIMEOUT) {
						System.out.println("Sending a ping! (" + now + ")");
						waitingForPong = true;
						pingSentAt = now;
//						bot.getClient().write("PING " + now);
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
