package com.ericsson.research.trap.nhttpd;

/**
 * Temp file manager.
 * <p/>
 * <p>Temp file managers are created 1-to-1 with incoming requests, to create and cleanup
 * temporary files created as a result of handling the request.</p>
 */
public interface TempFileManager {
    TempFile createTempFile() throws Exception;

    void clear();
}