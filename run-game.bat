@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-23
set PATH=%JAVA_HOME%\bin;%PATH%

set JAVAFX_PATH=C:\Program Files\Java\openjfx-21.0.7_windows-x64_bin-sdk\javafx-sdk-21.0.7\lib

set MODULE_PATH=%JAVAFX_PATH%\javafx.base.jar;%JAVAFX_PATH%\javafx.controls.jar;%JAVAFX_PATH%\javafx.fxml.jar;%JAVAFX_PATH%\javafx.graphics.jar;%JAVAFX_PATH%\javafx.media.jar;%JAVAFX_PATH%\javafx.swing.jar;%JAVAFX_PATH%\javafx.web.jar

java ^
--module-path "%MODULE_PATH%" ^
--add-modules javafx.controls,javafx.fxml,javafx.media ^
--enable-preview ^
--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED ^
-cp "target/classes;target/dependency/*" ^
com.dungeon.Main 