package com.ericsson.research.trap.nhttpd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides rudimentary support for cookies.
 * Doesn't support 'path', 'secure' nor 'httpOnly'.
 * Feel free to improve it and/or add unsupported features.
 *
 * @author LordFokas
 */
public class CookieHandler implements Iterable<String> {
    private HashMap<String, String> cookies = new HashMap<String, String>();
    private ArrayList<Cookie> queue = new ArrayList<Cookie>();

    public CookieHandler(Map<String, String> httpHeaders) {
        String raw = httpHeaders.get("cookie");
        if (raw != null) {
            String[] tokens = raw.split(";");
            for (String token : tokens) {
                String[] data = token.trim().split("=");
                if (data.length == 2) {
                    cookies.put(data[0], data[1]);
                }
            }
        }
    }

    @Override public Iterator<String> iterator() {
        return cookies.keySet().iterator();
    }

    /**
     * Read a cookie from the HTTP Headers.
     *
     * @param name The cookie's name.
     * @return The cookie's value if it exists, null otherwise.
     */
    public String read(String name) {
        return cookies.get(name);
    }

    /**
     * Sets a cookie.
     *
     * @param name    The cookie's name.
     * @param value   The cookie's value.
     * @param expires How many days until the cookie expires.
     */
    public void set(String name, String value, int expires) {
        queue.add(new Cookie(name, value, Cookie.getHTTPTime(expires)));
    }

    public void set(Cookie cookie) {
        queue.add(cookie);
    }

    /**
     * Set a cookie with an expiration date from a month ago, effectively deleting it on the client side.
     *
     * @param name The cookie name.
     */
    public void delete(String name) {
        set(name, "-delete-", -30);
    }

    /**
     * Internally used by the webserver to add all queued cookies into the Response's HTTP Headers.
     *
     * @param response The Response object to which headers the queued cookies will be added.
     */
    public void unloadQueue(Response response) {
        for (Cookie cookie : queue) {
            response.addHeader("Set-Cookie", cookie.getHTTPHeader());
        }
    }
}