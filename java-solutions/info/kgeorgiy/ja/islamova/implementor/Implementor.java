package info.kgeorgiy.ja.islamova.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Implementation of {@link Impler} and {@link JarImpler} interfaces
 * The class provides some functions like:
 * - Generate default implementations of classes and interfaces
 * - Compile and package the generated files into JAR files
 */
public class Implementor implements Impler, JarImpler {

    /** Default constructor for class Implementor*/
    public Implementor() {

    }

    /** String for indent (4 spaces)*/
    private static final String TAB = "    ";

    /** String for lineSeparator*/
    private static final String LS = System.lineSeparator();

    /** String for double indent (8 spaces)*/
    private static final String TAB2 = TAB.repeat(2);

    /**
     * Generates a default implementation of given interface or extension оf given class
     *
     * @param token the class or interface to implement
     * @param root the root directory where the implementation will be generated
     * @throws ImplerException if:
     * - The token is a primitive type, array, private, record, final class or enum
     * - Package name of token is wrong (java.* or javax.*)
     * - Failed to create package directory
     * - There are IO errors during generating and writing source code into file
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (token.isPrimitive() || token.isArray() || Modifier.isPrivate(token.getModifiers())
                || token.isRecord() || Modifier.isFinal(token.getModifiers()) || token.isAssignableFrom(Enum.class)) {
            throw new ImplerException("Cannot implement this type of class" + token.getTypeName());
        }

        String className = token.getSimpleName() + "Impl";
        String packageName = token.getPackageName();
        if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
            throw new ImplerException("Cannot use package starting with java or javac");
        }
        Path packagePath = root.resolve(packageName.replace('.', File.separatorChar));

        String sourceCode = generateSourceCode(token, className, packageName);

        Path sourceFile = packagePath.resolve(className + ".java");
        try {
            Path parentDir = sourceFile.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
        } catch (IOException e) {
            throw new ImplerException("Failed to create package directory", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(sourceFile, StandardCharsets.UTF_8)) {
            writer.write(toUnicode(sourceCode));
        } catch (IOException e) {
            throw new ImplerException("Failed to write source code to file.", e);
        }
    }

    /**
     * Adds parameters to a method or constructor signature
     *
     * @param sb the StringBuilder to append it to main source
     * @param parameters the list of parameters to add
     * @throws ImplerException if any argument is private
     */
    private void addParameters(StringBuilder sb, List<Parameter> parameters) throws ImplerException {
        sb.append("(");

        try {
            sb.append(parameters.stream()
                    .map(parameter -> {
                        if (Modifier.isPrivate(parameter.getModifiers())) {
                            throw new RuntimeException();
                        }
                        return parameter.getType().getCanonicalName() + " " + parameter.getName();
                    })
                    .collect(Collectors.joining(", ")));
        } catch (RuntimeException e) {
            throw new ImplerException("Cannot implement method or constructor with private argument");
        }

        sb.append(") ");
    }

    /**
     * Adds a super constructor call to constructor
     *
     * @param sb the StringBuilder to append it to main source
     * @param parameters the list of parameters for the super call
     */
    private void addSuper(StringBuilder sb, List<Parameter> parameters) {
        sb.append("{").append(LS);
        sb.append(TAB.repeat(2)).append("super(");

        // :NOTE: streams
        sb.append(parameters.stream().map(Parameter::getName).collect(Collectors.joining(", ")));

        sb.append(");").append(LS);
        sb.append(TAB).append("}").append(LS.repeat(2));
    }

    /**
     * Adds exceptions to a method or constructor signature.
     *
     * @param sb the StringBuilder to append it to main source
     * @param exceptions the list of exception types to add
     */
    private void addExceptions(StringBuilder sb, List<Class <?>> exceptions) {
        if (!exceptions.isEmpty()) {
            sb.append(" throws ");
            sb.append(exceptions.stream().map(Class::getCanonicalName).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Adds constructors to the generated class.
     *
     * @param sb the StringBuilder to append it to main source
     * @param token the class for which constructors are generated
     * @throws ImplerException all constructors are private
     */
    private void addConstructors(StringBuilder sb, Class<?> token) throws ImplerException {
        boolean isAny = false;
        for (Constructor<?> constructor: token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            isAny = true;
            sb.append(TAB).append("public ").append(constructor.getDeclaringClass().getSimpleName()).append("Impl");

            addParameters(sb, List.of(constructor.getParameters()));

            addExceptions(sb, List.of(constructor.getExceptionTypes()));

            addSuper(sb, List.of(constructor.getParameters()));

        }
        if (!isAny) {
            throw new ImplerException("Cannot create any constructors");
        }
    }

    /**
     * Adds a return statement to a method body
     *
     * @param sb the StringBuilder to append it to main source
     * @param returned the return value
     */
    private void addReturn(StringBuilder sb, String returned) {
        sb.append(TAB2).append("return ").append(returned).append(";\n");
    }

    /**
     * Adds a method body with default return values
     *
     * @param sb  the StringBuilder to append it to main source
     * @param method the method for which the body is generated
     */
    private void addBody(StringBuilder sb, Method method) {
        sb.append("{").append(LS);

        if (method.getReturnType() == boolean.class) {
            addReturn(sb, "false");
        } else if (method.getReturnType() == void.class){
            addReturn(sb, "");
        } else if (method.getReturnType().isPrimitive()) {
            addReturn(sb, "0");
        } else {
            addReturn(sb, "null");
        }
        sb.append(TAB).append("}").append(LS.repeat(2));
    }

    /**
     * Creates implemented methods for all abstract methods
     *
     * @param methods the set of methods to implement
     * @param sb the StringBuilder to append it to main source
     * @throws ImplerException if method or return type is private
     */
    private void createMethods(HashSet<Method> methods, StringBuilder sb) throws ImplerException {
        for (Method method : methods) {
            if (Modifier.isPrivate(method.getModifiers())
                    || Modifier.isPrivate(method.getReturnType().getModifiers())) {
                throw new ImplerException("Cannot implement private method or method with private return type");
            }

            sb.append(TAB).append("@Override\n");
            sb.append(TAB).append("public ").append(method.getReturnType().getCanonicalName()).append(" ")
                    .append(method.getName());

            addParameters(sb, List.of(method.getParameters()));

            addExceptions(sb, List.of(method.getExceptionTypes()));

            addBody(sb, method);

        }
    }

    /**
     * Filters and adds method implementations to the generated class.
     *
     * @param sb the StringBuilder to append it to main source
     * @param token the class or interface which methods are implemented
     * @throws ImplerException if any method cannot be implemented
     */
    private void addMethods(StringBuilder sb, Class<?> token) throws ImplerException {
        HashSet<Method> methods = new HashSet<>();
        methods.addAll(Arrays.stream(token.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers())).toList());
        methods.addAll(Arrays.stream(token.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers())).toList());

        createMethods(methods, sb);
    }

    /**
     * Generates the source code for implemented class
     *
     * @param token the class or interface to implement
     * @param className the name of the Impl class
     * @param packageName the package name for the new class
     * @return the generated source code as a String
     * @throws ImplerException if there are errors during code generation
     */
    private String generateSourceCode(Class<?> token, String className, String packageName) throws ImplerException {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(packageName).append(";").append(LS.repeat(2));

        sb.append("public class ").append(className).append(" ");

        if (token.isInterface()) {
            sb.append("implements ").append(token.getCanonicalName()).append(" {").append(LS);
        } else {
            sb.append("extends ").append(token.getCanonicalName()).append(" {").append(LS);
        }

        if (!token.isInterface()) {
            addConstructors(sb, token);
        }

        addMethods(sb, token);

        sb.append("}").append(LS);
        return sb.toString();
    }

    /**
     * Compiles files with the given dependencies
     *
     * @param files the list of files to compile
     * @param dependencies the list of dependency classes required for compilation
     * @param charset the charset for compilation
     */
    public static void compile(
            final List<Path> files,
            final List<Class<?>> dependencies,
            final Charset charset
    ) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String classpath = getClassPath(dependencies).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        final String[] args = Stream.concat(
                Stream.of("-cp", classpath, "-encoding", charset.name()),
                files.stream().map(Path::toString)
        ).toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
    }

    /**
     * Gets classpath notes for the given dependencies.
     *
     * @param dependencies the list of dependency classes
     * @return the list of paths
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
                    } catch (final URISyntaxException e) {
                        try {
                            throw new ImplerException("Syntax exception", e);
                        } catch (ImplerException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                })
                .toList();
    }

    /**
     * Converts a string to its Unicode
     *
     * @param s the string to convert
     * @return the Unicode string
     */
    private String toUnicode(String s) {
        return s.chars().mapToObj(ch -> ch < 128 ? String.valueOf((char)ch) : String.format("\\u%04x", ch))
                .collect(Collectors.joining());
    }

    /**
     * Deletes files and directories in given directory
     *
     * @param dir the directory and files there to delete
     * @throws IOException if there are errors while deleting
     */
    private void deleteFiles(Path dir) throws IOException {
        Files.walkFileTree(dir, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Generates a JAR file which have the implementation of the given class or interface.
     *
     * @param token the class or interface to implement
     * @param jarFile the path for the created JAR file
     * @throws ImplerException if:
     * - Token or jarFile is null
     * - There are errors while creating of temporary directory
     * - There are errors during implementation generation or compilation
     * - There are errors during JAR file creation
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {

        if (token == null || jarFile == null) {
            throw new ImplerException("Token or jarFile is null");
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(Path.of(""), "File");
        } catch (IOException e) {
            throw new ImplerException("Can't create directory", e);
        }

        implement(token, tempDir);

        String packageName = token.getPackageName();
        String className = token.getSimpleName() + "Impl";
        Path pathPackage = tempDir.resolve(packageName.replace(".", File.separator));
        compile(List.of(pathPackage.resolve(className + ".java")),
                List.of(token), StandardCharsets.UTF_8);

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            jarOutputStream.putNextEntry(new ZipEntry(packageName.replace(".", "/") + "/"
                    + className + ".class"));
            Files.copy(pathPackage.resolve(className + ".class"), jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("Failed to create jar file", e);
        }

        try {
            deleteFiles(tempDir);
        } catch (IOException e) {
            throw new ImplerException("failed to delete created files after work");
        }

    }

    /**
     * Main method for command-line interface.
     *
     * Usage:
     * - For implementation: className outputPath
     * - For JAR implementation: -jar className jarFile
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 3) {
            if (Objects.equals(args[0], "-jar")) {
                Implementor implementor = new Implementor();
                try {
                    implementor.implementJar(Class.forName(args[0]), Path.of(args[1]));
                } catch (ClassNotFoundException e) {
                    System.err.println("wrong name of given class");
                } catch (ImplerException e) {
                    System.err.println("some exception while creating class:" + e);
                }
            } else {
                System.err.println("wrong arguments");
            }
        } else if (args.length == 2) {
            Implementor implementor = new Implementor();
            try {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } catch (ClassNotFoundException e) {
                System.err.println("wrong name of given class");
            } catch (ImplerException e) {
                System.err.println("some exception while creating class:" + e);
            }
        } else {
            System.err.println("wrong number of arguments");
        }
    }
}
