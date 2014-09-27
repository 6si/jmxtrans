package com.googlecode.jmxtrans.example;

import com.googlecode.jmxtrans.JmxTransformer;
import com.googlecode.jmxtrans.model.JmxProcess;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.BaseOutputWriter;
import com.googlecode.jmxtrans.model.output.GraphiteWriter;
import com.googlecode.jmxtrans.util.JsonPrinter;

/**
 * This example shows how to query hibernate for its statistics information.
 * 
 * @author jon
 */
public class Hibernate {

	private static final String GW_HOST = "192.168.192.133";
	private static final JsonPrinter printer = new JsonPrinter(System.out);

	/** */
	public static void main(String[] args) throws Exception {

		Server.Builder serverBuilder = Server.builder()
				.setHost("w2")
				.setPort("1099")
				.setAlias("w2_hibernate_1099");

		GraphiteWriter gw = new GraphiteWriter();
		gw.addSetting(BaseOutputWriter.HOST, GW_HOST);
		gw.addSetting(BaseOutputWriter.PORT, 2003);

		// use this to add data to GW path
		gw.addTypeName("name");

		gw.addSetting(BaseOutputWriter.DEBUG, true);

		Query q = Query.builder()
				.setObj("org.hibernate.jmx:name=*,type=StatisticsService")
				.addAttr("EntityDeleteCount")
				.addAttr("EntityInsertCount")
				.addAttr("EntityLoadCount")
				.addAttr("EntityFetchCount")
				.addAttr("EntityUpdateCount")
				.addAttr("QueryExecutionCount")
				.addAttr("QueryCacheHitCount")
				.addAttr("QueryExecutionMaxTime")
				.addAttr("QueryCacheMissCount")
				.addAttr("QueryCachePutCount")
				.addAttr("FlushCount")
				.addAttr("ConnectCount")
				.addAttr("SecondLevelCacheHitCount")
				.addAttr("SecondLevelCacheMissCount")
				.addAttr("SecondLevelCachePutCount")
				.addAttr("SessionCloseCount")
				.addAttr("SessionOpenCount")
				.addAttr("CollectionLoadCount")
				.addAttr("CollectionFetchCount")
				.addAttr("CollectionUpdateCount")
				.addAttr("CollectionRemoveCount")
				.addAttr("CollectionRecreateCount")
				.addAttr("SuccessfulTransactionCount")
				.addAttr("TransactionCount")
				.addAttr("CloseStatementCount")
				.addAttr("PrepareStatementCount")
				.addAttr("OptimisticFailureCount")
				.addOutputWriter(gw)
				.build();
		serverBuilder.addQuery(q);

		JmxProcess process = new JmxProcess(serverBuilder.build());
		printer.prettyPrint(process);
		JmxTransformer transformer = new JmxTransformer();
		transformer.executeStandalone(process);

		// for (int i = 0; i < 160; i++) {
		// JmxUtils.processServer(server);
		// Thread.sleep(1000);
		// }

	}
}
