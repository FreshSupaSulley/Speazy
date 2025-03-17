package com.supasulley.speazy;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import net.lingala.zip4j.ZipFile;

/**
 * Transcriber waits for new audio samples, then processes then as fast as possible.
 */
public class Transcriber extends Thread implements Runnable, SampleListener {
	
	private ConcurrentLinkedQueue<File> samples = new ConcurrentLinkedQueue<File>();
	
	private final WhisperJNI whisper;
	private final WhisperContext ctx;
	private final StringBuffer buffer;
	
	private boolean running = true;
	
	public Transcriber(Path modelPath) throws IOException
	{
		this.buffer = new StringBuffer();
		
		// Point to directory
		// System.setProperty("io.github.givimad.whisperjni.libdir", new File("lib/macos-amd64").getAbsolutePath());
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();
		
		System.out.println("OS: " + osName);
		System.out.println("Arch: " + osArch);
		
		String resourceName = null;
		
		// Mac
		if(osName.contains("mac") || osName.contains("darwin"))
		{
			System.out.println("On Mac");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "macos-amd64";
			}
			else if(osArch.contains("aarch64") || osArch.contains("arm64"))
			{
				resourceName = "macos-arm64";
			}
		}
		else if(osName.contains("win"))
		{
			System.out.println("On Windows");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "win-amd64";
			}
		}
		else if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
		{
			System.out.println("On Linux");
			
			if(osArch.contains("amd64") || osArch.contains("x86_64"))
			{
				resourceName = "debian-amd64";
			}
			else if(osArch.contains("aarch64") || osArch.contains("arm64"))
			{
				resourceName = "debian-arm64";
			}
			else if(osArch.contains("armv7") || osArch.contains("arm"))
			{
				resourceName = "debian-armv7l";
			}
		}
		
		if(resourceName == null)
		{
			throw new IllegalStateException("Native libraries not available for this OS: " + osName + ", Arch: " + osArch);
		}
		
		System.out.println("Loading libraries for " + resourceName);
		
		Stream.of(extractZipToTemp("lib/" + resourceName).listFiles()).forEach(file ->
		{
			System.out.println("Loading library at " + file);
			System.load(file.getAbsolutePath());
		});
		
		// Bad native libraries (for me)!
		// WhisperJNI.loadLibrary();
		
		// I don't care
		WhisperJNI.setLibraryLogger(null);
		
		this.whisper = new WhisperJNI();
		this.ctx = whisper.init(modelPath);
		
		// Thread init
		setName("Transcriber");
		setDaemon(true);
		start();
	}
	
	private static File extractZipToTemp(String zipName) throws IOException
	{
		Path tempZip = Files.createTempFile("temp", ".zip");
		Files.copy(AudioRecorder.class.getClassLoader().getResourceAsStream(zipName + ".zip"), tempZip, StandardCopyOption.REPLACE_EXISTING);
		
		try(ZipFile zip = new ZipFile(tempZip.toFile()))
		{
			Path destination = Files.createTempDirectory("temp");
			destination.toFile().deleteOnExit();
			
			// Extract it at the same destination
			zip.extractAll(destination.toString());
			
			// Pass in child name so we're not giving the parent folder back
			return destination.toFile();
		} finally
		{
			// We can delete the copied, unzipped zip
			tempZip.toFile().delete();
		}
	}
	
	@Override
	public void run()
	{
		while(running)
		{
			File audio = samples.poll();
			if(audio == null)
				continue;
			
			try
			{
				float[] samples = getSamples(audio);
				if(samples == null)
					continue;
				
				var params = new WhisperFullParams();
				int result = whisper.full(ctx, params, samples, samples.length);
				
				if(result != 0)
				{
					throw new RuntimeException("Transcription failed with code " + result);
				}
				
				int numSegments = whisper.fullNSegments(ctx);
				
				for(int i = 0; i < numSegments; i++)
				{
					String text = whisper.fullGetSegmentText(ctx, 0).trim();
					if(text.equals("[BLANK_AUDIO]"))
						continue;
					
					buffer.append(text);
				}
			} catch(UnsupportedAudioFileException e)
			{
				e.printStackTrace();
			} catch(IOException e)
			{
				e.printStackTrace();
			} finally
			{
				// No longer need it
				audio.delete();
			}
		}
	}
	
	/**
	 * Gets all transcribed words and clears the buffer.
	 * 
	 * @return buffer of transcribed words
	 */
	public String getBuffer()
	{
		String result = buffer.toString();
		buffer.setLength(0);
		return result;
	}
	
	public void shutdown()
	{
		running = false;
	}
	
	// private static void convertAudio(String inputFilePath, String outputFilePath)
	// {
	// try
	// {
	// File inputFile = new File(inputFilePath);
	// AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
	//
	// // Specify the desired format (16-bit PCM, mono, 16000 Hz)
	// AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	//
	// // Convert the audio
	// AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
	//
	// // Write to the output file (e.g., WAV or AIFF)
	// File outputFile = new File(outputFilePath);
	// AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, outputFile);
	// System.out.println("Audio conversion completed.");
	// } catch(Exception e)
	// {
	// e.printStackTrace();
	// }
	// }
	
	private float[] getSamples(File file) throws UnsupportedAudioFileException, IOException
	{
		// convertAudio(file.getAbsolutePath(), file.getAbsolutePath());
		
		// sample is a 16 bit int 16000hz little endian wav file
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
		// read all the available data to a little endian capture buffer
		ByteBuffer captureBuffer = ByteBuffer.allocate(audioInputStream.available());
		captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
		int read = audioInputStream.read(captureBuffer.array());
		if(read == -1)
			return null;
		// obtain the 16 int audio samples, short type in java
		var shortBuffer = captureBuffer.asShortBuffer();
		// transform the samples to f32 samples
		float[] samples = new float[captureBuffer.capacity() / 2];
		var i = 0;
		while(shortBuffer.hasRemaining())
		{
			samples[i++] = Float.max(-1f, Float.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f));
		}
		return samples;
	}
	
	@Override
	public void newSample(File file)
	{
		samples.add(file);
	}
}
