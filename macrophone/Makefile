JAVA_HOME = /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home
CONTROLLER = src/com/supasulley/macrophone/Macrophone.java
LIB_FILE = src/main/resources/macrophone.dylib
NATIVE_DIR = src/main/native

# Build library by default
all: clean buildLib

# Generate JNI header
header:
	mkdir -p ${NATIVE_DIR}
	javac -h ${NATIVE_DIR} src/com/supasulley/macrophone/*.java

# Generate signature for a particular class
sig:
	@bash -c 'read -p "Class name: " CLASSNAME && javap -s $$CLASSNAME'

# Generate signatures for our class
signatures:
	javac ${CONTROLLER} -d bin
	javap -s -p bin/com/supasulley/macrophone/Macrophone.class

# Build library
buildLib:
	clang++ -std=c++17 -framework Cocoa -framework AVFoundation -framework CoreFoundation -fvisibility=hidden -Wall -shared -o ${LIB_FILE} ${NATIVE_DIR}/*.cpp ${NATIVE_DIR}/*.mm -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin
	# Sanity checks
	file ${LIB_FILE}
	nm ${LIB_FILE} | grep Java

# Remove built library
clean:
	rm -f ${LIB_FILE}
