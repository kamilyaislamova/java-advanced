@echo off
setlocal
set pathImplTests=..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor
set pathImplToolsTests=..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor.tools
set pathImpl=..\java-solutions\info\kgeorgiy\ja\islamova\implementor\Implementor.java
set pathImplBaseTests=..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.base
set pathImplLib=..\..\java-advanced-2025\lib\*

javadoc -private -cp "%pathImplLib%;%pathImplBaseTests%;%pathImplToolsTests%;%pathImplTests%" -d "..\javadoc" "..\java-solutions\info\kgeorgiy\ja\islamova\implementor\Implementor.java" "..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\Impler.java" "..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\ImplerException.java" "..\..\java-advanced-2025\modules\info.kgeorgiy.java.advanced.implementor.tools\info\kgeorgiy\java\advanced\implementor\tools\JarImpler.java"