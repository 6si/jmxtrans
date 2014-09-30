package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Maps.newHashMap;
import static com.googlecode.jmxtrans.model.output.BaseOutputWriter.PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphiteWriterTests {

	@Test(expected = ValidationException.class)
	public void validationExceptionIfNoHostConfigured() throws ValidationException {
		try {
			GraphiteWriter writer = new GraphiteWriter();
			Map<String, Object> settings = newHashMap();
			settings.put(PORT, 123);
			writer.setSettings(settings);
			writer.validateSetup(null, null);
		} catch (ValidationException ve) {
			assertThat(ve).hasMessage("Host and port can't be null");
			throw ve;
		}
	}

	@Test(expected = ValidationException.class)
	public void validationExceptionIfNoPortConfigured() throws ValidationException {
		try {
			GraphiteWriter writer = new GraphiteWriter();
			Map<String, Object> settings = newHashMap();
			settings.put(BaseOutputWriter.HOST, "localhost");
			writer.setSettings(settings);
			writer.validateSetup(null, null);
		} catch (ValidationException ve) {
			assertThat(ve).hasMessage("Host and port can't be null");
			throw ve;
		}
	}

	@Test
	public void writeSingleResult() throws Exception {
		// a lot of setup for not much of a test ...
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result("attributeName", "className", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = new GraphiteWriter();
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));

		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host_123.classNameAlias.attributeName_key 1 ");
	}

}
