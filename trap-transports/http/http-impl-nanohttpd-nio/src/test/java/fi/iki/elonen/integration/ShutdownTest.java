package fi.iki.elonen.integration;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.ericsson.research.trap.nhttpd.impl.NanoHTTPDImpl;

public class ShutdownTest
{

	@Test
	public void connectionsAreClosedWhenServerStops() throws IOException
	{
		TestServer server = new TestServer();
		server.start();
		try
		{
			makeRequest();
		}
		catch (FileNotFoundException e)
		{
		}
		server.stop();
		try
		{
			makeRequest();
			fail("Connection should be closed!");
		}
		catch (IOException e)
		{
			// Expected exception
		}
	}

	private void makeRequest() throws MalformedURLException, IOException
	{
		HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8092/").openConnection();
		// Keep-alive seems to be on by default, but just in case that changes.
		connection.addRequestProperty("Connection", "keep-alive");
		InputStream in = connection.getInputStream();
		while (in.available() > 0)
		{
			in.read();
		}
		in.close();
	}

	private class TestServer extends NanoHTTPDImpl
	{

		public TestServer()
		{
			super(8092);
		}

	}

}
