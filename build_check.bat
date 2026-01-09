@echo off
echo === Compiling Kotlin Code ===
call gradlew.bat :app:compileDebugKotlin --console=plain
echo.
echo === Build Complete ===
pause
