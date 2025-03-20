JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home
CONTROLLER = src/com/supasulley/macrophone/Macrophone.java
LIB_FILE = lib/macrophone.dylib

# Build library by default
all: clean build

# Generate JNI header
header:
	mkdir -p jni
	javac -h jni src/com/supasulley/macrophone/*.java

# Generate signature for a particular class
sig:
	@bash -c 'read -p "Class name: " CLASSNAME && javap -s $$CLASSNAME'

# Generate signatures for our class
signatures:
	javac ${CONTROLLER} -d bin
	javap -s -p bin/com/supasulley/macrophone/Macrophone.class

# Build library
build:
#	gcc -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/darwin/" -o libjniexample.jnilib -shared jni/*.cpp
#	clang -dynamiclib -framework AVFoundation -framework Foundation -o jni.dylib jni/*.cpp -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
#	clang -dynamiclib -framework AVFoundation -framework Foundation -o jni.dylib jni/*.cpp jni/*.m -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
#	clang -x objective-c++ -framework Foundation -framework AVFoundation -fPIC -dynamiclib -o jni.dylib jni/*.cpp jni/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
#	clang++ -std=c++17 -framework AVFoundation jni/*.mm -fPIC -dynamiclib -o jni.dylib jni/*.cpp jni/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
	clang++ -std=c++17 -framework Cocoa -framework AVFoundation -framework CoreFoundation -fvisibility=hidden -Wall -shared -o ${LIB_FILE} jni/*.cpp jni/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
	# Sanity checks
	file ${LIB_FILE}
	nm ${LIB_FILE} | grep Java

# Remove built library
clean:
	rm -f ${LIB_FILE}
