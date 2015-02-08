package com.ericsson.research.trap.nhttpd;

import java.io.OutputStream;

/**
 * A temp file.
 * <p/>
 * <p>
 * Temp files are responsible for managing the actual temporary storage and
 * cleaning themselves up when no longer needed.
 * </p>
 */
public interface TempFile
{
	OutputStream open() throws Exception;

	void delete() throws Exception;

	String getName();

}