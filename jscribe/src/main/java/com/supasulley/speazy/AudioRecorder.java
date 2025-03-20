package com.supasulley.speazy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class AudioRecorder extends Thread implements Runnable {
	
	/** Format Whisper wants (also means wave file) */
	private static final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	private static final AudioFileFormat.Type FILE_TYPE = AudioFileFormat.Type.WAVE;
	
	private final SampleListener listener;
	private final int recordTime, overlapTime;
	private final File parentFolder;
	private final AudioWriterWorker[] workers;
	
	private boolean running = true;
	
	private Mixer.Info device;
	
	public AudioRecorder(SampleListener listener, String micName, int recordTime, int overlapTime) throws IOException
	{
		this.listener = listener;
		this.workers = new AudioWriterWorker[5];
		this.recordTime = recordTime;
		this.overlapTime = overlapTime;
		this.parentFolder = Files.createTempDirectory("samples").toFile();
		this.parentFolder.deleteOnExit();
		
		if(overlapTime >= recordTime)
		{
			throw new IllegalStateException("Overlap time may not be greater than or equal to record time");
		}
		
		List<Mixer.Info> microphones = getMicrophones();
		
		if(microphones.isEmpty())
		{
			throw new IllegalStateException("No microphones detected");
		}
		
		this.device = microphones.stream().filter(mic -> mic.getName().equals(micName)).findFirst().orElse(microphones.getFirst());
		System.out.println("Using microphone " + device.getName());
	}
	
	public static List<Mixer.Info> getMicrophones()
	{
		List<Mixer.Info> names = new ArrayList<Mixer.Info>();
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		
		for(Mixer.Info mixerInfo : mixers)
		{
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
			
			if(mixer.isLineSupported(lineInfo))
			{
				names.add(mixerInfo);
			}
		}
		
		return names;
	}
	
	@Override
	public void run()
	{
		long startTime = System.currentTimeMillis();
		newSample();
		
		while(running)
		{
			if(System.currentTimeMillis() - startTime > recordTime - overlapTime)
			{
				startTime = System.currentTimeMillis();
				newSample();
			}
		}
	}
	
	private void newSample()
	{
		// Find open slot
		boolean found = false;
		
		for(int i = 0; i < workers.length; i++)
		{
			if(workers[i] == null || !workers[i].isAlive())
			{
				found = true;
				workers[i] = new AudioWriterWorker();
				break;
			}
		}
		
		if(!found)
			throw new IllegalStateException("Failed to start new AudioWriterWorker. All slots already running");
	}
	
	public void shutdown()
	{
		running = false;
	}
	
	// @Override
	// public void close() throws Exception
	// {
	// for(int i = 0; i < workers.length; i++)
	// {
	// workers[i].join();
	// }
	//
	// model.close();
	// recognizer.close();
	// }
	
	class AudioWriterWorker extends Thread implements Runnable {
		
		private File outputFile;
		
		private AudioWriterWorker()
		{
			try
			{
				this.outputFile = File.createTempFile("speazy", "." + FILE_TYPE.getExtension(), parentFolder);
			} catch(IOException e)
			{
				throw new IllegalStateException(e);
			}
			
			setDaemon(true);
			setName("Audio Writer Worker");
			start();
		}
		
		/**
		 * Captures the sound and record into a WAV file
		 */
		@Override
		public void run()
		{
			// JavaxMicrophone microphone = new JavaxMicrophone(selected);
			
			// try(TargetDataLine line = AudioSystem.getTargetDataLine(format))
			try(TargetDataLine line = AudioSystem.getTargetDataLine(format, device))
			{
				// line = (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, format));
				// DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				
				// checks if system supports the data line
				if(!AudioSystem.isLineSupported(line.getLineInfo()))
				{
					throw new IllegalStateException("Line not supported");
				}
				
				// Ensure parent folders exist
				outputFile.getParentFile().mkdirs();
				
				line.open();
				line.start();
				
				// Another thread does the writing to not choke this one, which watches it
				Thread thread = new Thread(() ->
				{
					try
					{
						AudioSystem.write(new AudioInputStream(line), FILE_TYPE, outputFile);
					} catch(IOException e)
					{
						e.printStackTrace();
					}
				}, "Audio Writer Worker");
				
				thread.setDaemon(true);
				thread.start();
				
				// Record for this long
				Thread.sleep(recordTime);
				
				// End it
				line.stop();
				line.close();
			} catch(LineUnavailableException | InterruptedException e)
			{
				System.err.println("Failed to record audio to file");
				e.printStackTrace();
			}
			
			// Invoke the listener
			listener.newSample(outputFile);
		}
	}
}