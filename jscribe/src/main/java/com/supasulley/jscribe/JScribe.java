package com.supasulley.jscribe;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JScribe implements UncaughtExceptionHandler {
	
	protected static Logger logger;
	
	private final Path modelPath;
	private boolean running;
	private AudioRecorder recorder;
	private Transcriber transcriber;
	
	private String exitReason;
	
	public JScribe(Logger logger, Path modelPath)
	{
		JScribe.logger = logger;
		this.modelPath = modelPath;
		
		try
		{
			// Load natives
			Transcriber.loadNatives();
		} catch(IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	public JScribe(Path modelPath)
	{
		this(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME), modelPath);
	}
	
	/**
	 * Starts live audio transcription.
	 * 
	 * @return true if transcription started, false if it was already running
	 */
	public boolean start() throws Exception
	{
		if(running)
		{
			return false;
		}
		
		logger.info("Starting JScribe");
		exitReason = null;
		
		// where do i put running = true lol
		recorder = new AudioRecorder(transcriber = new Transcriber(modelPath), "", 2000, 500);
		
		// Report errors to this thread
		recorder.setUncaughtExceptionHandler(this);
		transcriber.setUncaughtExceptionHandler(this);
		
		running = true;
		recorder.start();
		transcriber.start();
		return true;
	}
	
	/**
	 * Stops live audio transcription.
	 * 
	 * @return true if transcription stopped, false if it wasn't running
	 */
	public boolean stop()
	{
		if(!running)
		{
			return false;
		}
		
		logger.info("Stopping JScribe");
		
		transcriber.shutdown();
		recorder.shutdown();
		running = false;
		return true;
	}
	
	/**
	 * @return true if running, false otherwise (error or simply stopped)
	 */
	public boolean isRunning()
	{
		return running;
	}
	
	/**
	 * Gets and clears the error that caused JScribe to stop.
	 * 
	 * @return string representing the reason why JScribe stopped early, null if no error
	 */
	public String getError()
	{
		// Strings are immutable so this is fine?
		String reason = exitReason;
		exitReason = null;
		return reason;
	}
	
	/**
	 * @return number of queued audio files needed to be transcribed
	 */
	public int getBacklog()
	{
		return transcriber.getBacklog();
	}
	
	/**
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words
	 */
	public String getBuffer()
	{
		return transcriber.getBuffer();
	}
	
	@Override
	public void uncaughtException(Thread t, Throwable e)
	{
		JScribe.logger.error("JScribe ended early due to an unhandled error in thread " + t.getName(), e);
		this.exitReason = e.getMessage();
		stop();
	}
}
