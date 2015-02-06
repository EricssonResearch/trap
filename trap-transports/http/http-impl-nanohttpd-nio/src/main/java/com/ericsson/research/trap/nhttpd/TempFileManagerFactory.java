package com.ericsson.research.trap.nhttpd;


/**
 * Factory to create temp file managers.
 */
public interface TempFileManagerFactory {
    TempFileManager create();
}