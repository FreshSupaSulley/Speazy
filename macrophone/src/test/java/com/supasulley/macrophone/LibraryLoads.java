/*
 * This source file was generated by the Gradle 'init' task
 */
package com.supasulley.macrophone;

import org.junit.jupiter.api.Test;

class LibraryLoads {
	
	@Test
	void nativeLibLoads()
	{
		// Ensuring loading native lib works
		AVAuthorizationStatus status = Macrophone.getMicrophonePermission();
		System.out.println(status);
	}
}
