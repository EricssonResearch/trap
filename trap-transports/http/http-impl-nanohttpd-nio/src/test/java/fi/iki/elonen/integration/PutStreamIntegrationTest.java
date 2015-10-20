package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;

import com.ericsson.research.trap.nhttpd.Method;
import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.impl.NanoHTTPDImpl;

public class PutStreamIntegrationTest extends IntegrationTestBase<PutStreamIntegrationTest.TestServer> {

    @Test
    public void testSimplePutRequest() throws Exception {
        String expected = "This HttpPut request has a content-length of 48.";

        HttpPut httpput = new HttpPut("http://localhost:8192/");
        httpput.setEntity(new ByteArrayEntity(expected.getBytes()));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpput, responseHandler);

        assertEquals("PUT:" + expected, responseBody);
    }

    @Override public TestServer createTestServer() {
        return new TestServer();
    }

    public static class TestServer extends NanoHTTPDImpl implements RequestHandler {
        public TestServer() {
            super(8192);
            setHandler(this);
        }

        @Override
        public void handleRequest(Request session, Response response) {
            Method method = session.getMethod();
            Map<String, String> headers = session.getHeaders();
            int contentLength = Integer.parseInt(headers.get("content-length"));

            byte[] body;
            try {
                DataInputStream dataInputStream = new DataInputStream(session.getInputStream());
                body = new byte[contentLength];
                dataInputStream.readFully(body, 0, contentLength);
            }
            catch(IOException e) {
            	response.setStatus(500).setData(e.getMessage());
            	return;
            }
            
            String rv = String.valueOf(method) + ':' + new String(body);
            response.setData(rv).setStatus(200);
        }
    }
}
