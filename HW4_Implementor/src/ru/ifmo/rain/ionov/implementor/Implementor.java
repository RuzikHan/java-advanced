package ru.ifmo.rain.ionov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.jar.*;

/**
 * Implementation for interfaces {@link Impler} and {@link JarImpler}
 */
public class Implementor implements Impler, JarImpler {
    /**
     * This method create .jar or .java file depends on arguments
     *
     * @param args arguments which need for main method
     */
    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Error: args must be not null");
            return;
        }
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3 && args[0] != null && args[1] != null && args[2] != null) {
                if (args[0].equals("-jar") && args[2].endsWith(".jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    System.out.println("Error: for 3 args, args[0] must be -jar && args[2] must end with .jar");
                }
                return;
            }
            if (args.length == 2 && args[0] != null && args[1] != null) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                System.out.println("Error: for 2 args, args[0] must be classname");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error: can't find class");
        } catch (ImplerException e) {
            System.out.println("Error: can't implement given interface");
        }
    }

    /**
     * Produces code implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <tt>root</tt> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <tt>$root/java/util/ListImpl.java</tt>
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if method can't create realization for given interface in these cases:
     *                         token not interface, token is null, root is null, wrong folderPath, error while creating Impl file.
     */
    public void implement(Class<?> token, Path root) throws ImplerException {
        tokenCheck(token, root);
        String interfacePackage = null;
        if (token.getPackage() != null) {
            interfacePackage = token.getPackage().getName();
        }
        String interfaceName = token.getSimpleName() + "Impl";
        Path folderPath;
        if (token.getPackage() != null) {
            folderPath = root.resolve(token.getPackage().getName().replace('.', '/') + '/');
        } else {
            folderPath = root.resolve("");
        }
        Path filePath = folderPath.resolve(interfaceName + ".java");
        directoryCreator(folderPath);
        try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(filePath))) {
            getPackageAndClassLine(interfacePackage, printWriter, interfaceName, token);
            getMethods(token, printWriter);
        } catch (IOException e) {
            throw new ImplerException("Error: Can't write Impl file");
        }
    }

    /**
     * This method get package and class line from from given token and write them in given printwriter
     *
     * @param interfacePackage package name of given token
     * @param printWriter      in which you write received package and class line
     * @param interfaceName    name of given interface
     * @param token            interface in which you want get methods
     */
    private void getPackageAndClassLine(String interfacePackage, PrintWriter printWriter, String interfaceName, Class<?> token) {
        if (interfacePackage != null) {
            printWriter.println(toUnicode("package " + token.getPackage().getName() + ";"));
            printWriter.println();
        }
        String modifiersLine = Modifier.toString(token.getModifiers() & ~(Modifier.ABSTRACT | Modifier.INTERFACE));
        printWriter.println(toUnicode(modifiersLine + " class " + interfaceName + " implements " + token.getSimpleName() + " {"));
    }

    /**
     * This method check token and root if it correct to implement
     *
     * @param token which you want to check for correctness
     * @param root  which you want to check for correctness
     * @throws ImplerException if class or root not correct
     */
    private void tokenCheck(Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Error: Required not null interface name");
        }
        if (!token.isInterface()) {
            throw new ImplerException("Error: Required interface");
        }
        if (root == null) {
            throw new ImplerException("Error: Required correct path");
        }
    }

    /**
     * This method get methods from given token and write them in given printwriter
     *
     * @param token       interface in which you want get methods
     * @param printWriter in which you write received methods
     */
    private void getMethods(Class<?> token, PrintWriter printWriter) {
        for (Method method : token.getMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                printWriter.println("@" + annotation.annotationType().getSimpleName());
            }
            String methodModifiersLine = Modifier.toString(method.getModifiers() & ~(Modifier.ABSTRACT | Modifier.INTERFACE | Modifier.TRANSIENT));
            printWriter.print(toUnicode(methodModifiersLine + " " + method.getReturnType().getCanonicalName() + " " + method.getName() + "("));
            getParameters(method, printWriter);
            getExceptions(method, printWriter);
            printWriter.println(toUnicode("{return " + defaultValue(method) + ";}"));
        }
        printWriter.println("}");
    }

    /**
     * This method get parameters from given method andwrite them in given printwriter
     *
     * @param method      methods in which you want get parameters
     * @param printWriter in which you write received parameters
     */
    private void getParameters(Method method, PrintWriter printWriter) {
        boolean f = false;
        for (Parameter token1 : method.getParameters()) {
            if (!f) {
                printWriter.print(toUnicode(token1.getType().getCanonicalName() + " " + token1.getName()));
                f = true;
            } else {
                printWriter.print(toUnicode(", " + token1.getType().getCanonicalName() + " " + token1.getName()));
            }
        }
        printWriter.print(") ");
    }

    /**
     * This method get exceptions from given method andwrite them in given printwriter
     *
     * @param method      methods in which you want get exceptions
     * @param printWriter in which you write received parameters
     */
    private void getExceptions(Method method, PrintWriter printWriter) {
        boolean f = false, f1 = false;
        for (Class<?> token1 : method.getExceptionTypes()) {
            if (!f) {
                f = true;
                printWriter.print("throws ");
            }
            if (!f1) {
                printWriter.print(toUnicode(token1.getCanonicalName()));
                f1 = true;
            } else {
                printWriter.print(toUnicode(", " + token1.getCanonicalName()));
            }
        }
    }

    /**
     * This method create empty directory in given path
     *
     * @param path folder in which you want create directory
     * @throws ImplerException if can't create directory in given path
     */
    private void directoryCreator(Path path) throws ImplerException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new ImplerException("Error: Can't create folder");
        }
    }

    /**
     * Generate string containing default return value of the required type for given method
     *
     * @param method for which we want to get default value
     * @return {@link java.lang.String} default value for given method
     */
    private String defaultValue(Method method) {
        if (method.getReturnType().equals(boolean.class)) {
            return "false";
        } else if (method.getReturnType().equals(void.class)) {
            return "";
        } else if (method.getReturnType().isPrimitive()) {
            return "0";
        } else return "null";
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException when implementation cannot be generated in these cases:
     *                         wrong folderPath, program can't compile, error while creating jar file.
     */
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path mainPath = Paths.get("D:/Programms/Java/Java_Technologies");
        mainPath = mainPath.resolve("HW4_Implementor_Tests");
        directoryCreator(mainPath);
        implement(token, mainPath);
        Path folderPath = mainPath.resolve(token.getPackage().getName().replace('.', '/') + '/');
        Path javaPath = folderPath.resolve(token.getSimpleName() + "Impl.java");
        Path classPath = folderPath.resolve(token.getSimpleName() + "Impl.class");
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compilationCode = compiler.run(null, null, null, javaPath.toString(), "-cp", System.getProperty("java.class.path"));
            if (compilationCode != 0) {
                throw new ImplerException("Compilation error");
            }
        } catch (NullPointerException e) {
            throw new ImplerException("Compilation error");
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest);
             BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(classPath.toString()))) {
            String zipPath = token.getPackage().getName().replace('.', '/') + '/' + token.getSimpleName() + "Impl.class";
            jarOutputStream.putNextEntry(new JarEntry(zipPath));
            byte[] buffer = new byte[1024];
            while (inputStream.available() > 0) {
                jarOutputStream.write(buffer, 0, inputStream.read(buffer));
            }
            jarOutputStream.closeEntry();
        } catch (IOException e) {
            throw new ImplerException("Error");
        }
        deleteFiles(mainPath.toFile());
    }

    /**
     * This method delete directory which was created by implementJar
     *
     * @param file directory which you want delete
     * @throws ImplerException if something go wrong while deleting directories
     */
    private void deleteFiles(File file) throws ImplerException {
        if (file.isDirectory()) {
            for (File a : Objects.requireNonNull(file.listFiles())) {
                deleteFiles(a);
            }
        }
        if (!file.delete()) {
            throw new ImplerException("Error: can't delete directories");
        }
    }

    /**
     * This method convert string to Unicode format
     *
     * @param in string which you want to convert to Unicode
     * @return {@link java.lang.String} Unicode string
     */
    private String toUnicode(String in) {
        StringBuilder b = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (c >= 128) {
                b.append("\\u").append(String.format("%04X", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}