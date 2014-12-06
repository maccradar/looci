# Makefile to build a LooCI Component (with android.jar) and copy the .jar.

JARFILE=RsgComponent.jar
CODEBASEFILE=be/kuleuven/mech/rsg/looci/android/zmq/RsgCodebase.java
COMPONENTFILE=be/kuleuven/mech/rsg/looci/android/zmq/RsgComponent.java
TOOLS=../../looci2-android/android-sdk/build-tools/android-4.4.2

all: clean $(JARFILE)
 
$(JARFILE): ./src/$(CODEBASEFILE) ./src/$(COMPONENTFILE)
	@echo "Building..."
	ant -f build.xml

	@echo "Adapting to Android devices..."
	cp ./build/jar/$(JARFILE) ./
	$(TOOLS)/dx --dex --output=classes.dex $(JARFILE)
	$(TOOLS)/aapt add $(JARFILE) classes.dex
	rm classes.dex

	@echo "Moving to folder for components..."
	mkdir -p ${LOOCI}/components/
	cp $(JARFILE) ${LOOCI}/components/

clean:
	@echo "Cleaning up the folder..."
	rm -f $(JARFILE)
	ant clean

