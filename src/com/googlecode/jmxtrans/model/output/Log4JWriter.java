package com.googlecode.jmxtrans.model.output;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.NumberUtils;
import com.googlecode.jmxtrans.util.ValidationException;

import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * 
 * A writer for Log4J. It may be a nice way to send JMX metrics to Logstash for example. <br /> <br />
 * 
 * Here is an example of MDC variables that are set by this class. <br> <br> server: localhost_9003 <br /> metric:
 * sun_management_MemoryImpl.HeapMemoryUsage_committed <br /> value: 1251999744 <br /> resultAlias: myHeapMemoryUsage
 * <br /> attributeName: HeapMemoryUsage <br /> key: committed <br /> epoch: 1388343325728 <br />
 * 
 * @author Yannick Robin
 */
public class Log4JWriter extends BaseOutputWriter
{
	private Logger log;

	/** */
	public Log4JWriter() { }

	/**
	 * Get the logger
	 */
	public void validateSetup(Server server, final Query query) throws ValidationException
	{
		String loggerName = (String) this.getSettings().get("logger");

		if (loggerName == null || loggerName.equals(""))
		{
			loggerName = "Log4JWriter";
		}

		log = Logger.getLogger("Log4JWriter." + loggerName);
	}

	/**
	 * Set the log context and log
	 */
	public void doWrite(Server server, final Query query, ImmutableList<Result> results) throws Exception
	{
		final List<String> typeNames = getTypeNames();

		for (final Result result : results)
		{
			final Map<String, Object> resultValues = result.getValues();
			if (resultValues != null)
			{
				for (final Entry<String, Object> values : resultValues.entrySet())
				{
					if (NumberUtils.isNumeric(values.getValue()))
					{
						String alias = null;
						if (server.getAlias() != null)
						{
							alias = server.getAlias();
						}
						else
						{
							alias = server.getHost() + "_" + server.getPort();
							alias = cleanupStr(alias);
						}

						MDC.put("server", alias);
						MDC.put("metric", JmxUtils.getKeyString(server, query, result, values, typeNames, null));
						MDC.put("value", values.getValue());
						if (result.getClassNameAlias() != null)
						{
							MDC.put("resultAlias", result.getClassNameAlias());
						}
						MDC.put("attributeName", result.getAttributeName());
						MDC.put("key", values.getKey());
						MDC.put("Epoch", String.valueOf(result.getEpoch()));
						log.info("");
					}
				}
			}
		}
	}
}
