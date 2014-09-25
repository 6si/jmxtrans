package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.model.output.StdOutWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

/**
 * This class produces the json that is in example.json.
 * 
 * @author jon
 */
public class InterestingInfo {

	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = new Server("w2", "1099");
		server.setNumQueryThreads(2);

		GraphiteWriter gw = new GraphiteWriter();
		gw.addSetting(GraphiteWriter.HOST, "192.168.192.133");
		gw.addSetting(GraphiteWriter.PORT, 2003);

		StdOutWriter sw = new StdOutWriter();

		Query q = Query.builder()
				.setObj("java.lang:type=Memory")
				.addAttr("HeapMemoryUsage")
				.addAttr("NonHeapMemoryUsage")
				.addOutputWriters(gw, sw)
				.build();
		server.addQuery(q);

		Query q2 = Query.builder()
				.setObj("java.lang:type=Threading")
				.addAttr("DaemonThreadCount")
				.addAttr("PeakThreadCount")
				.addAttr("ThreadCount")
				.addOutputWriters(gw, sw)
				.build();
		server.addQuery(q2);

		Query q3 = Query.builder()
				.setObj("java.lang:name=ConcurrentMarkSweep,type=GarbageCollector")
				.addAttr("LastGcInfo")
				.addOutputWriters(gw, sw)
				.build();
		server.addQuery(q3);

		Query q4 = Query.builder()
				.setObj("java.lang:name=ParNew,type=GarbageCollector")
				.addAttr("LastGcInfo")
				.addOutputWriters(gw, sw)
				.build();
		server.addQuery(q4);

		JmxProcess process = new JmxProcess(server);
		printer.prettyPrint(process);
		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);
	}

}
