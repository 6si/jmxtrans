package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableList;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.googlecode.jmxtrans.connections.SocketFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.naming.KeyUtils;
import com.googlecode.jmxtrans.monitoring.ManagedObject;
import com.googlecode.jmxtrans.pool.ManagedGenericKeyedObjectPool;
import com.googlecode.jmxtrans.pool.PoolUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.NumberUtils;
import com.googlecode.jmxtrans.util.ValidationException;

import static com.google.common.base.Charsets.UTF_8;

/**
 * This low latency and thread save output writer sends data to a host/port combination
 * in the Graphite format.
 *
 * @see <a
 *      href="http://graphite.wikidot.com/getting-your-data-into-graphite">http://graphite.wikidot.com/getting-your-data-into-graphite</a>
 *
 * @author jon
 */
public class GraphiteWriter extends BaseOutputWriter {

	private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);
	public static final String ROOT_PREFIX = "rootPrefix";

	private String rootPrefix = "servers";

	private static KeyedObjectPool pool = null;
	private static AtomicInteger activeServers = new AtomicInteger(0);

	private Lock statusLock = new ReentrantLock();
	private Condition statusConditionStarted = statusLock.newCondition();	
	private Condition statusConditionStopped = statusLock.newCondition();
	private GraphiteWriterStatus status = GraphiteWriterStatus.STOPPED;
	
	private ManagedObject mbean;
	private InetSocketAddress address;

	/**
	 * Uses JmxUtils.getDefaultPoolMap()
	 */
	public GraphiteWriter() { }

	/**
	 * If pool already started, just increase number of activeServers,
	 * if it is not, start the pool and register it on JMX
	 */
	@Override
	public void start() throws LifecycleException {
		statusLock.lock();
		try {
			if (status != GraphiteWriterStatus.STOPPED) {
				throw new LifecycleException("GraphiteWriter instance should be stopped");
			}
		
			int activeServersLocal = activeServers.incrementAndGet();
		
			// Start the pool when the first instance starts
			if (activeServersLocal == 1) {
				startPool();
			}
		
			status = GraphiteWriterStatus.STARTED;
			statusConditionStarted.signal();
		} finally {
			statusLock.unlock();
		}
	}
	
	@Override
	public void stop() throws LifecycleException {
		statusLock.lock();
		try {
			if (status != GraphiteWriterStatus.STARTED) {
				throw new LifecycleException("GraphiteWriter instance shold be started");
			}
		
			// Stop the pool if there is no more started instances
			if(activeServers.decrementAndGet() == 0) { 
				stopPool();
			}
		
			status = GraphiteWriterStatus.STOPPED;
			statusConditionStopped.signal();
		} finally {
			statusLock.unlock();
		}
	}
		
	/** */
	public void validateSetup(Server server, Query query) throws ValidationException {
		String host = (String) this.getSettings().get(HOST);
		Object portObj = this.getSettings().get(PORT);
		Integer port = null;
		if (portObj instanceof String) {
			port = Integer.parseInt((String) portObj);
		} else if (portObj instanceof Integer) {
			port = (Integer) portObj;
		}

		if (host == null || port == null) {
			throw new ValidationException("Host and port can't be null", query);
		}

		String rootPrefixTmp = (String) this.getSettings().get(ROOT_PREFIX);
		if (rootPrefixTmp != null) {
			rootPrefix = rootPrefixTmp;
		}

		this.address = new InetSocketAddress(host, port);
	}

	/** */
	public void doWrite(Server server, Query query, ImmutableList<Result> results) throws Exception {
		Socket socket = null;
		statusLock.lock();
		try {
			while (status == GraphiteWriterStatus.STARTING) {
				statusConditionStarted.await();
			}
			if (status != GraphiteWriterStatus.STARTED) {
				throw new LifecycleException("GraphiteWriter instance should be started");
			}
			socket = (Socket) pool.borrowObject(address);
		} finally {
			statusLock.unlock();
		}

		try {
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);

			List<String> typeNames = this.getTypeNames();

			for (Result result : results) {
				if (isDebugEnabled()) {
					log.debug("Query result: " + result.toString());
				}
				Map<String, Object> resultValues = result.getValues();
				if (resultValues != null) {
					for (Entry<String, Object> values : resultValues.entrySet()) {
						Object value = values.getValue();
						if (NumberUtils.isNumeric(value)) {

							String line = KeyUtils.getKeyString(server, query, result, values, typeNames, rootPrefix)
									.replaceAll("[()]", "_") + " " + value.toString() + " "
									+ result.getEpoch() / 1000 + "\n";
							if (isDebugEnabled()) {
								log.debug("Graphite Message: " + line.trim());
							}
							writer.write(line);
							writer.flush();
						} else {
							if (log.isWarnEnabled()) {
								log.warn("Unable to submit non-numeric value to Graphite: \"" + value + "\" from result " + result);
							}
						}
					}
				}
			}
		} finally {
			pool.returnObject(address, socket);
		}
	}
	
	/**
	 * Starts the pool and register it with JMX
	 * 
	 * @throws LifecycleException
	 */
	private void startPool() throws LifecycleException {
		pool = PoolUtils.getObjectPool(new SocketFactory());
		
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName objectName = new ObjectName("com.googlecode.jmxtrans:Type=GenericKeyedObjectPool,PoolName=SocketFactory,Name=GWManagedGenericKeyedObjectPool");

			if (!mbs.isRegistered(objectName) ) {
				this.mbean = new ManagedGenericKeyedObjectPool((GenericKeyedObjectPool)pool, PoolUtils.SOCKET_FACTORY_POOL);
				this.mbean.setObjectName(objectName);
				ManagementFactory.getPlatformMBeanServer()
						.registerMBean(this.mbean, this.mbean.getObjectName());
			}
			log.debug("GraptiteWriter connection pool is started");
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

	/**
	 * Stops the pool and removes it from JMX
	 * 
	 * @throws LifecycleException
	 */
	private void stopPool() throws LifecycleException {
		try {
			if (this.mbean != null) {
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.mbean.getObjectName());
				this.mbean = null;
			}
			if (pool != null) {
				pool.close();
				pool = null;
			}
			log.debug("GraptiteWriter connection pool is stopped");
		} catch (Exception e) {
			throw new LifecycleException(e);
		}
	}

}
