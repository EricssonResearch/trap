package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;

import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.impl.NanoHTTPDImpl;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 5/19/13 at 5:36 PM
 */
public class GetAndPostIntegrationTest extends IntegrationTestBase<GetAndPostIntegrationTest.TestServer> {

    @Test
    public void testSimpleGetRequest() throws Exception {
        testServer.response = "testSimpleGetRequest";

        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testSimpleGetRequest", responseBody);
    }

    @Test
    public void testGetRequestWithParameters() throws Exception {
        testServer.response = "testGetRequestWithParameters";

        HttpGet httpget = new HttpGet("http://localhost:8192/?age=120&gender=Male");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testGetRequestWithParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostWithNoParameters() throws Exception {
        testServer.response = "testPostWithNoParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostWithNoParameters", responseBody);
    }

    @Override public TestServer createTestServer() {
        return new TestServer();
    }

    public static class TestServer extends NanoHTTPDImpl implements RequestHandler {
        public String response;

        public TestServer() {
            super(8192);
            setHandler(this);
        }

        @Override
        public void handleRequest(Request request, Response response) {
            StringBuilder sb = new StringBuilder(String.valueOf(request.getMethod()) + ':' + this.response);
			Map<String, String> parms = request.getParms();
            if (parms.size() > 1) {
                parms.remove("NanoHttpd.QUERY_STRING");
                sb.append("-params=").append(parms.size());
                List<String> p = new ArrayList<String>(parms.keySet());
                Collections.sort(p);
                for (String k : p) {
                    sb.append(';').append(k).append('=').append(parms.get(k));
                }
            }
            
            response.setData(sb.toString()).setStatus(200);

        }

    }
}
