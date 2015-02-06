package com.ericsson.research.trap.nhttpd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Default strategy for creating and cleaning up temporary files.
 * <p/>
 * <p></p></[>By default, files are created by <code>File.createTempFile()</code> in
 * the directory specified.</p>
 */
public class DefaultTempFile implements TempFile {
    private File file;
    private OutputStream fstream;

    public DefaultTempFile(String tempdir) throws IOException {
        file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
        fstream = new FileOutputStream(file);
    }

    @Override
    public OutputStream open() throws Exception {
        return fstream;
    }

    @Override
    public void delete() throws Exception {
        NanoHTTPD.safeClose(fstream);
        file.delete();
    }

    @Override
    public String getName() {
        return file.getAbsolutePath();
    }
}