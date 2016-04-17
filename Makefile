# Name of the jarfile.
JARNAME=BEAM4.jar

# Java version number.
JAVAVER=$(shell javac -version 2>&1)
STR="s/\(^    static final String  COMPILER   = \"Compiler was: \).*\(\";.*$$\)/\1${JAVAVER}\2/g"
	
all:
	@# Replace Java version number string in source code.
	sed -i ${STR} Sources/B4constants.java
	
	@# Compile java files.
	javac -Xlint Sources/*.java

	@# Create jar archive.
	install -d com/stellarsoftware/beam/
	install -D Sources/*.class com/stellarsoftware/beam/
	jar cfe "${JARNAME}" com.stellarsoftware.beam.B4 com/

clean:
	rm -rf Sources/*.class com/
