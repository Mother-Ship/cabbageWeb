package top.mothership.cabbage.util.irc;

import java.util.LinkedList;
import java.util.Queue;

class RateLimitedChannel {
	private int           delay;
	private String        channel;
	private Queue<String> messages;
	private long          lastSentTime;
	RateLimitedChannel(String channel, int delay) {
		this.channel = channel;
		this.delay = delay;
		this.messages = new LinkedList<>();
	}

	void addMessage(String message) {
		messages.add(message);
	}

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

	boolean hasNext() {
		return !messages.isEmpty();
	}
}
