package com.googlecode.jmxtrans.model.output;

import com.google.common.collect.ImmutableMap;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.ValidationException;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GraphiteWriterTests {

	@Test(expected = NullPointerException.class)
	public void hostIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setPort(123)
					.build();
		} catch (NullPointerException npe) {
			assertThat(npe).hasMessage("Host cannot be null.");
			throw npe;
		}
	}

	@Test(expected = NullPointerException.class)
	public void portIsRequired() throws ValidationException {
		try {
			GraphiteWriter.builder()
					.setHost("localhost")
					.build();
		} catch (NullPointerException npe) {
			assertThat(npe).hasMessage("Port cannot be null.");
			throw npe;
		}
	}
	
	private static GraphiteWriter getGraphiteWriter(OutputStream out, boolean useObjDomain) throws Exception {
		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);
		
		when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.setUseObjDomain(useObjDomain)
				.build();
		writer.setPool(pool);
		
		return writer;
	}

	@Test
	public void writeSingleResult() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GraphiteWriter writer = getGraphiteWriter(out, false);

		writer.doWrite(server, query, of(result));

		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host_123.classNameAlias.attributeName_key 1 ");
	}
	
	@Test
	public void useObjDomainWorks() throws Exception {
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", null, "typeName", ImmutableMap.of("key", (Object)1));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		// Set useObjDomain to true
		GraphiteWriter writer = getGraphiteWriter(out, true);

		writer.doWrite(server, query, of(result));

		// check that Graphite format is respected
		assertThat(out.toString()).startsWith("servers.host_123.objDomain.attributeName_key 1 ");
	}


	@Test
	public void socketInvalidatedWhenError() throws Exception {
		// a lot of setup for not much of a test ...
		Server server = Server.builder().setHost("host").setPort("123").build();
		Query query = Query.builder().build();
		Result result = new Result(System.currentTimeMillis(), "attributeName", "className", "objDomain", "classNameAlias", "typeName", ImmutableMap.of("key", (Object)1));

		GenericKeyedObjectPool<InetSocketAddress, Socket> pool = mock(GenericKeyedObjectPool.class);
		Socket socket = mock(Socket.class);
		when(pool.borrowObject(any(InetSocketAddress.class))).thenReturn(socket);
		UnflushableByteArrayOutputStream out = new UnflushableByteArrayOutputStream();
		when(socket.getOutputStream()).thenReturn(out);

		GraphiteWriter writer = GraphiteWriter.builder()
				.setHost("localhost")
				.setPort(2003)
				.build();
		writer.setPool(pool);

		writer.doWrite(server, query, of(result));
		verify(pool).invalidateObject(any(InetSocketAddress.class), eq(socket));
		verify(pool, never()).returnObject(any(InetSocketAddress.class), eq(socket));
	}

	private static class UnflushableByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public void flush() throws IOException {
			throw new IOException();
		}
	}

}
