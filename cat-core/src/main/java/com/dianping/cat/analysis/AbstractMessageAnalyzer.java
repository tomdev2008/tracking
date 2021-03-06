package com.dianping.cat.analysis;

import org.codehaus.plexus.logging.Logger;
import org.unidal.lookup.ContainerHolder;

import com.dianping.cat.Cat;
import com.dianping.cat.ServerConfigManager;
import com.dianping.cat.message.spi.MessageQueue;
import com.dianping.cat.message.spi.MessageTree;

public abstract class AbstractMessageAnalyzer<R> extends ContainerHolder implements MessageAnalyzer {
	public static final long MINUTE = 60 * 1000L;

	public static final long ONE_HOUR = 60 * 60 * 1000L;

	public static final long ONE_DAY = 24 * ONE_HOUR;

	private long m_extraTime;

	protected long m_startTime;

	protected long m_duration;

	protected Logger m_logger;

	private long m_errors = 0;

	private volatile boolean m_active = true;

	@Override
	public void analyze(MessageQueue queue) {
		while (!isTimeout() && isActive()) {
			MessageTree tree = queue.poll();

			if (tree != null) {
				try {
					process(tree);
				} catch (Throwable e) {
					m_errors++;

					if (m_errors == 1 || m_errors % 10000 == 0) {
						Cat.logError(e);
					}
				}
			}
		}

		while (true) {
			MessageTree tree = queue.poll();

			if (tree != null) {
				try {
					process(tree);
				} catch (Throwable e) {
					m_errors++;

					if (m_errors == 1 || m_errors % 10000 == 0) {
						Cat.logError(e);
					}
				}
			} else {
				break;
			}
		}
	}

	@Override
	public void destroy() {
		super.release(this);
	}

	@Override
	public abstract void doCheckpoint(boolean atEnd);

	protected long getExtraTime() {
		return m_extraTime;
	}

	public abstract R getReport(String domain);

	@Override
	public long getStartTime() {
		return m_startTime;
	}

	@Override
	public void initialize(long startTime, long duration, long extraTime) {
		m_extraTime = extraTime;
		m_startTime = startTime;
		m_duration = duration;

		loadReports();
	}

	protected boolean isActive() {
		synchronized (this) {
			return m_active;
		}
	}

	protected boolean isLocalMode() {
		ServerConfigManager manager = lookup(ServerConfigManager.class);

		return manager.isLocalMode();
	}

	protected boolean isTimeout() {
		long currentTime = System.currentTimeMillis();
		long endTime = m_startTime + m_duration + m_extraTime;

		return currentTime > endTime;
	}

	protected  void loadReports() {
		// to be overridden
	}

	protected abstract void process(MessageTree tree);

	public void shutdown() {
		synchronized (this) {
			m_active = false;
		}
	}
	
}
