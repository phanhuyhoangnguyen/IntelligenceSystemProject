@ECHO OFF
CLS
echo.
echo Home Energy Trading System
echo --------------------------



echo Press ENTER to start.

set /p input=

cd Compiled

echo Running...
java -jar ./HomeEnergySys.jar

echo Application finished.

pause