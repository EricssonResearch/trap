package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;

import com.ericsson.research.trap.nhttpd.Cookie;
import com.ericsson.research.trap.nhttpd.CookieHandler;
import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.StatusCodes;
import com.ericsson.research.trap.nhttpd.impl.NanoHTTPDImpl;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 9/2/13 at 10:10 PM
 */
public class CookieIntegrationTest extends IntegrationTestBase<CookieIntegrationTest.CookieTestServer> {

    @Test
    public void testNoCookies() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        httpclient.execute(httpget, responseHandler);

        CookieStore cookies = httpclient.getCookieStore();
        assertEquals(0, cookies.getCookies().size());
    }

    @Test
    public void testCookieSentBackToClient() throws Exception {
        testServer.cookiesToSend.add(new Cookie("name", "value", 30));
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        httpclient.execute(httpget, responseHandler);

        CookieStore cookies = httpclient.getCookieStore();
        assertEquals(1, cookies.getCookies().size());
        assertEquals("name", cookies.getCookies().get(0).getName());
        assertEquals("value", cookies.getCookies().get(0).getValue());
    }

    @Test
    public void testServerReceivesCookiesSentFromClient() throws Exception {
        BasicClientCookie clientCookie = new BasicClientCookie("name", "value");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 100);
        clientCookie.setExpiryDate(calendar.getTime());
        clientCookie.setDomain("localhost");
        httpclient.getCookieStore().addCookie(clientCookie);
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        httpclient.execute(httpget, responseHandler);

        assertEquals(1, testServer.cookiesReceived.size());
        assertTrue(testServer.cookiesReceived.get(0).getHTTPHeader().contains("name=value"));
    }

    @Override public CookieTestServer createTestServer() {
        return new CookieTestServer();
    }

    public static class CookieTestServer extends NanoHTTPDImpl implements RequestHandler {
        List<Cookie> cookiesReceived = new ArrayList<Cookie>();
        List<Cookie> cookiesToSend = new ArrayList<Cookie>();

        public CookieTestServer() {
            super(8192);
            setHandler(this);
        }

        @Override public void handleRequest(Request session, Response response) {
            CookieHandler cookies = session.getCookies();
            for (String cookieName : cookies) {
                cookiesReceived.add(new Cookie(cookieName, cookies.read(cookieName)));
            }
            for (Cookie c : cookiesToSend) {
                cookies.set(c);
            }
            response.setStatus(StatusCodes.OK).setData("Cookies");
        }

    }
}
