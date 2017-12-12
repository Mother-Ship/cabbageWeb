package top.mothership.cabbage.util.irc;


import java.util.ConcurrentModificationException;
import java.util.Iterator;

class RateLimitedFlusher extends Thread {
	private IRCClient m_client;
	private int m_delay;

	RateLimitedFlusher(IRCClient client, int delay) {
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
							m_client.write(line);
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
