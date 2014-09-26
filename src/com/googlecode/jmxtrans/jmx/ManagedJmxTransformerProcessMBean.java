package com.googlecode.jmxtrans.jmx;

import java.io.File;

import com.googlecode.jmxtrans.util.LifecycleException;

/**
 * The Interface ManagedJmxTransformerProcessMBean.
 */
public interface ManagedJmxTransformerProcessMBean {
	
	/**
	 * Start the JmxProcess.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	void start() throws LifecycleException;
	
	/**
	 * Stop the JmxProcess.
	 *
	 * @throws LifecycleException the lifecycle exception
	 */
	void stop() throws LifecycleException;
	
	/**
	 * Gets the quart properties file.
	 *
	 * @return the quart properties file
	 */
	String getQuartPropertiesFile();
	
	/**
	 * Sets the quart properties file.
	 *
	 * @param quartPropertiesFile the quart properties file
	 */
	void setQuartPropertiesFile(String quartPropertiesFile);
	
	/**
	 * Gets the run period.
	 *
	 * @return the run period
	 */
	int getRunPeriod();
	
	/**
	 * Sets the run period.
	 *
	 * @param runPeriod the run period
	 */
	void setRunPeriod(int runPeriod);
	
	/**
	 * Sets the json dir or file.
	 *
	 * @param jsonDirOrFile the json dir or file
	 */
	void setJsonDirOrFile(File jsonDirOrFile);
	
	/**
	 * Gets the json dir or file.
	 *
	 * @return the json dir or file
	 */
	File getJsonDirOrFile();
}
