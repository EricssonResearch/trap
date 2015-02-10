package com.ericsson.research.trap.nhttpd;


public final class ResponseException extends Exception
{

	private static final long	serialVersionUID	= 1L;
	private final int	      status;

	public ResponseException(int status, String message)
	{
		super(message);
		this.status = status;
	}

	public ResponseException(int status, String message, Exception e)
	{
		super(message, e);
		this.status = status;
	}

	public int getStatus()
	{
		return status;
	}
}