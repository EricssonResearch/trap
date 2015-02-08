package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

import com.ericsson.research.trap.nhttpd.IHTTPSession;
import com.ericsson.research.trap.nhttpd.NanoHTTPD;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.Response.Status;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 5/19/13 at 5:36 PM
 */
public class POSTIntegrationTests extends IntegrationTestBase<POSTIntegrationTests.TestServer>
{
	
	public class BasicResponseHandler implements ResponseHandler<byte[]> {

	    /**
	     * Returns the response body as a String if the response was successful (a
	     * 2xx status code). If no response body exists, this returns null. If the
	     * response was unsuccessful (>= 300 status code), throws an
	     * {@link HttpResponseException}.
	     */
	    public byte[] handleResponse(final HttpResponse response)
	            throws HttpResponseException, IOException {
	        StatusLine statusLine = response.getStatusLine();
	        HttpEntity entity = response.getEntity();
	        if (statusLine.getStatusCode() >= 300) {
	            EntityUtils.consume(entity);
	            throw new HttpResponseException(statusLine.getStatusCode(),
	                    statusLine.getReasonPhrase());
	        }
	        return entity == null ? null : EntityUtils.toByteArray(entity);
	    }

	}


	@Test
	public void testLargePost() throws Exception
	{
		testServer.chunked = false;
		byte[] testData = new byte[128*1024];
		for (int i=0; i<testData.length; i++)
			testData[i] = (byte) (i%255);
		
		HttpPost post = new HttpPost("http://localhost:8192/");
		ResponseHandler<byte[]> responseHandler = new BasicResponseHandler();
		
		post.setEntity(new ByteArrayEntity(testData));
		
		byte[] responseBody = httpclient.execute(post, responseHandler);

		Assert.assertArrayEquals(testData, responseBody);
	}


	@Test
	public void testLargePostChunked() throws Exception
	{

		byte[] testData = new byte[128*1024];
		for (int i=0; i<testData.length; i++)
			testData[i] = (byte) (i%255);
		
		HttpPost post = new HttpPost("http://localhost:8192/");
		ResponseHandler<byte[]> responseHandler = new BasicResponseHandler();
		
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setChunked(true);
		entity.setContent(new ByteArrayInputStream(testData));
		post.setEntity(entity);
		
		byte[] responseBody = httpclient.execute(post, responseHandler);

		Assert.assertArrayEquals(testData, responseBody);
	}


	@Test
	public void testLargePostChunkedReply() throws Exception
	{

		testServer.chunked = true;
		byte[] testData = new byte[128*1024];
		for (int i=0; i<testData.length; i++)
			testData[i] = (byte) (i%255);
		
		HttpPost post = new HttpPost("http://localhost:8192/");
		ResponseHandler<byte[]> responseHandler = new BasicResponseHandler();
		
		post.setEntity(new ByteArrayEntity(testData));
		
		byte[] responseBody = httpclient.execute(post, responseHandler);

		Assert.assertArrayEquals(testData, responseBody);
	}
	
	@Test
	public void testPipelinedPost() throws Exception
	{
		for (int i=0; i<10; i++)
			testLargePost();
	}

	@Override
	public TestServer createTestServer()
	{
		return new TestServer();
	}

	public static class TestServer extends NanoHTTPD
	{
		public boolean	chunked;

		public TestServer()
		{
			super(8192);
		}

		@Override
		public Response serve(IHTTPSession session)
		{

			try
			{
				InputStream is = session.getInputStream();
				byte[] bs = new byte[4096];
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				int read = 0;
				while ((read = is.read(bs)) > -1)
					bos.write(bs, 0, read);

				Response r = new Response();
				r.setStatus(Status.OK);

				if (chunked)
					r.setData(new ByteArrayInputStream(bos.toByteArray()));
				else
					r.setData(bos.toByteArray());
				return r;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

	}
}
