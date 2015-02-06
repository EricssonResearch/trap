package com.ericsson.research.trap.nhttpd;


/**
 * Default strategy for creating and cleaning up temporary files.
 */
class DefaultTempFileManagerFactory implements TempFileManagerFactory {
    @Override
    public TempFileManager create() {
        return new DefaultTempFileManager();
    }
}