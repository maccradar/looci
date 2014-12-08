# Makefile to build Example Application.

PACKAGE=be.kuleuven.mech.rsg.gui
NAMEAPP=RsgApp
FOLDER=.
TARGET=1

build: clean
	@echo "Customizing the environment to your system..."
	$(LOOCI_ANDROID)/android-sdk/tools/android update project --name $(NAMEAPP) --target $(TARGET) --path $(FOLDER)

	@echo "Creating Android application (.apk)..."
	ant debug

	mv $(FOLDER)/bin/$(NAMEAPP)-debug.apk ./$(NAMEAPP).apk

install: uninstall
	@echo "Install by executing:"
	$(LOOCI_ANDROID)/android-sdk/platform-tools/adb install $(NAMEAPP).apk

uninstall:
	@echo "Uninstall by executing:"
	$(LOOCI_ANDROID)/android-sdk/platform-tools/adb uninstall $(PACKAGE)

clean:
	@echo "Clean up the folder..."
	rm -rf $(FOLDER)/bin
	rm -rf $(FOLDER)/assets
	rm -rf $(FOLDER)/gen
	rm -rf $(FOLDER)/libs
	rm -rf $(FOLDER)/build.xml
	rm -rf $(FOLDER)/local.properties
	rm -rf $(FOLDER)/proguard-project.txt
	rm -rf $(FOLDER)/project.properties
	rm -rf $(FOLDER)/$(NAMEAPP).apk
