@echo off
setlocal
set DIRNAME=%~dp0
if exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
  java -Xmx64m -Xms64m -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>&1
if %ERRORLEVEL%==0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo Open NoorAlIslamAndroid in Android Studio and press Build ^> Build APK(s).
pause
