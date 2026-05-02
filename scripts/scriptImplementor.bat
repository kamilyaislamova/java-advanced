@echo off
setlocal
set pathImpl=..\java-solutions\info\kgeorgiy\ja\islamova\implementor\Implementor.java
set pathImplTests=..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor
set pathImplToolsTests=..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor.tools

@REM javac ..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\ImplerException.java
@REM javac -cp "%pathImplTests%" ..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\Impler.java
javac -cp "%pathImplTests%;%pathImplToolsTests%" "%pathImpl%"

echo Manifest-Version: 1.0 > MANIFEST.MF
echo Created-By: 1.7.0_06 (Oracle Corporation) >> MANIFEST.MF
echo Main-Class: info.kgeorgiy.ja.islamova.implementor.Implementor >> MANIFEST.MF

jar cfm "ImplementorJar.jar" MANIFEST.MF -C ..\java-solutions \info\kgeorgiy\ja\islamova\implementor\Implementor.class -C %pathImplTests% \info\kgeorgiy\java\advanced\implementor\Impler.class -C %pathImplTests% \info\kgeorgiy\java\advanced\implementor\ImplerException.class -C %pathImplToolsTests% \info\kgeorgiy\java\advanced\implementor\tools\JarImpler.class
endlocal