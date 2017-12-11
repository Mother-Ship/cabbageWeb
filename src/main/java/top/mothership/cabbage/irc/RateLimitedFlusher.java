package top.mothership.cabbage.irc;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

class RateLimitedFlusher extends Thread {
	private IRCClient client;
	private int delay;

	RateLimitedFlusher(IRCClient client, int delay) {
		client = client;
		delay = delay;
	}

	@Override
	public void run() {
		while (true) {
			try {
				Iterator<RateLimitedChannel> it = client.getChannels().values().iterator();
				synchronized (it) {
					while (it.hasNext()) {
						RateLimitedChannel limiter = it.next();
						String line = limiter.poll();
						if (line != null)
							client.write(line);
					}
				}
				it = null;
				Thread.sleep(delay);
			} catch (ConcurrentModificationException e) {
				e.printStackTrace();
			} catch (InterruptedException ignore) {
			}
		}
	}
}
