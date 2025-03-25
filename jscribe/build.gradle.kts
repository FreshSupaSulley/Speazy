/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.13/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    id("java-library");
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
	// Use macrophone for permissions
	implementation(project(":macrophone"))
	
    // Whisper JNI
    implementation("io.github.givimad:whisper-jni:1.7.1")
    
    // https://mvnrepository.com/artifact/net.lingala.zip4j/zip4j
    api("net.lingala.zip4j:zip4j:2.11.5")
    
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.16")
	
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
