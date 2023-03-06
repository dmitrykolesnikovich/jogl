@echo off

gradlew jogl:desktop-official-default-example:dist && java -jar desktop-official/dist/default.jar