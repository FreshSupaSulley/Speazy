package com.supasulley.speazy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Whispa {
	
	public static void main(String[] args) throws IOException
	{
		// First (and only) argument provided indicates the preferred microphone
		String preferredMic = args.length > 0 ? args[0] : null;
		
		// Setup transcriber / natives
		// Transfer zipped model to temp directory
		Path tempZip = Files.createTempFile("model", ".en.bin");
		tempZip.toFile().deleteOnExit();
		Files.copy(Whispa.class.getClassLoader().getResourceAsStream("ggml-tiny.en.bin"), tempZip, StandardCopyOption.REPLACE_EXISTING);
		System.out.println("Copying GGML model to " + tempZip);
		
		Transcriber.loadNatives();
		
		AudioRecorder recorder = new AudioRecorder(new Transcriber(tempZip), preferredMic, 2000, 500);
		recorder.start();
	}
}
