package com.supasulley.speazy;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JarController {
	
	private Process process;
	private String jarPath;
	
	private SocketCommunicator communicator;
	private AudioRecorder writer;
	
	private StringBuffer buffer;
	
	private boolean running;
	
	public JarController(String jarPath)
	{
		this.jarPath = jarPath;
		
//		Runtime.getRuntime().addShutdownHook(new Thread(() ->
//		{
//			stop();
//		}));
	}
	
	public void start() throws IOException
	{
		if(running)
		{
			Speazy.LOGGER.warn("Tried to start controller when already running");
			return;
		}
		
		Speazy.LOGGER.info("Starting socket connection");
		
		// bad
		running = true;
		
		// Reset buffer
		buffer = new StringBuffer();
		
		// Start server
		process = new ProcessBuilder("java", "-jar", jarPath).start();
		
		// Create a thread to read the process output and print it to the console
		Thread outputThread = new Thread(() ->
		{
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				String line;
				while((line = reader.readLine()) != null)
				{
					// Print each line of the output to the console
					System.out.println("PIPE -- " + line);
				}
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		});
		
		// Start the output thread
		outputThread.start();
		
		Thread outputThread2 = new Thread(() ->
		{
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream())))
			{
				String line;
				while((line = reader.readLine()) != null)
				{
					// Print each line of the output to the console
					System.out.println("PIPE -- " + line);
				}
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		});
		
		// Start the output thread
		outputThread2.start();
		
		// Start client
		communicator = new SocketCommunicator();
		
		// Start recording
		writer = new AudioRecorder(communicator, 2000, 500);
		writer.start();
	}
	
	public void stop()
	{
		if(!running)
		{
			Speazy.LOGGER.warn("Tried to stop controller when already stopped");
			return;
		}
		
		Speazy.LOGGER.info("Closing socket connection");
		
		if(process != null)
		{
			process.destroyForcibly();
			writer.shutdown();
		}
		
		running = false;
	}
	
	/**
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words
	 */
	public String getBuffer()
	{
		if(buffer == null)
			return "";
		
		String result = buffer.toString();
		buffer.setLength(0);
		return result;
	}
	
	public boolean isRunning()
	{
		return running;
	}
	
	private class SocketCommunicator extends Thread implements Runnable, SampleListener, Closeable {
		
		private final Socket socket;
		private ConcurrentLinkedQueue<File> samples = new ConcurrentLinkedQueue<File>();
		
		public SocketCommunicator() throws IOException
		{
			socket = new Socket();
			
			setDaemon(true);
			setName("Speazy Socket Communicator");
			start();
		}
		
		@Override
		public void run()
		{
			// Setup client
			final int port = 6666;
			
			try
			{
				Thread.sleep(3000);
			} catch(InterruptedException e)
			{
				// FIXME Auto-generated catch block
				e.printStackTrace();
			}
			while(running)
			{
				try(socket)
				{
					socket.connect(new InetSocketAddress(port));
					// Connected
					break;
				} catch(SocketException e)
				{
					Speazy.LOGGER.warn("Port " + port + " is not available. Retrying...");
					try
					{
						Thread.sleep(1000);
					} catch(InterruptedException ie)
					{
						throw new IllegalStateException(ie);
					}
				} catch(IOException e1)
				{
					e1.printStackTrace();
				}
			}
			
			if(!socket.isConnected())
			{
				Speazy.LOGGER.error("Failed to connect to socket");
				return;
			}
			
			Speazy.LOGGER.info("Connected to socket");
			
			Thread thread = new Thread(() ->
			{
				try(BufferedReader stream = new BufferedReader(new InputStreamReader(socket.getInputStream())))
				{
					while(socket.isConnected())
					{
						// Add to queue
						buffer.append(stream.readLine());
						System.out.print(getBuffer());
					}
				} catch(IOException e)
				{
					e.printStackTrace();
				}
			}, "Whispa Socket Reader");
			
			thread.setDaemon(true);
			thread.start();
			
			Thread writer = new Thread(() ->
			{
				try(PrintWriter output = new PrintWriter(socket.getOutputStream()))
				{
					// Feed each file to socket
					while(socket.isConnected())
					{
						File file = samples.poll();
						
						if(file != null)
						{
							System.out.println("writing");
							output.println("/Users/eboschert/Desktop/jfk.wav");
							// output.println(file.getAbsolutePath());
							output.flush();
						}
					}
				} catch(IOException e)
				{
					// FIXME Auto-generated catch block
					e.printStackTrace();
				}
			}, "Whispa Socket Writer");
			
			writer.setDaemon(true);
			writer.start();
		}
		
		@Override
		public void close() throws IOException
		{
			// socket.close();
		}
		
		@Override
		public void newSample(File file)
		{
			if(socket == null || !socket.isConnected())
			{
				Speazy.LOGGER.warn("Server is not ready. Wasting sample at " + file);
				return;
			}
			
			System.out.println("ADDING");
			samples.add(file);
		}
	}
}
