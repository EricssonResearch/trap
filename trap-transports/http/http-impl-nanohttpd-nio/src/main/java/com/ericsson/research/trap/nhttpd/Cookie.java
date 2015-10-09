package com.ericsson.research.trap.nhttpd;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Cookie {
    private String n, v, e;

    public Cookie(String name, String value, String expires) {
        n = name;
        v = value;
        e = expires;
    }

    public Cookie(String name, String value) {
        this(name, value, 30);
    }

    public Cookie(String name, String value, int numDays) {
        n = name;
        v = value;
        e = getHTTPTime(numDays);
    }

    public String getHTTPHeader() {
        String fmt = "%s=%s; expires=%s";
        return String.format(fmt, n, v, e);
    }

    public static String getHTTPTime(int days) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return dateFormat.format(calendar.getTime());
    }
}