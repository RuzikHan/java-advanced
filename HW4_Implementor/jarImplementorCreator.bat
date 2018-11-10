mkdir jarCreate
javac -d jarCreate -cp artifacts\JarImplementorTest.jar src\ru\ifmo\rain\ionov\implementor\*.java
chdir jarCreate
jar xvf ..\artifacts\JarImplementorTest.jar info\kgeorgiy\java\advanced\implementor\Impler.class info\kgeorgiy\java\advanced\implementor\JarImpler.class info\kgeorgiy\java\advanced\implementor\ImplerException.class
jar cfe ..\Implementor.jar ru.ifmo.rain.ionov.implementor.Implementor ru\ifmo\rain\ionov\implementor\*.class info\kgeorgiy\java\advanced\implementor\*.class
chdir ..
rd /s/q D:\Programms\Java\Java_Technologies\HW4_Implementor\jarCreate
pause