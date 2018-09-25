package wala.entrypoint;

import wala.entrypoint.generator.GenerateModule2ThreadEntryPoint;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by adann on 24.08.16.
 */
public class WalaJDK9ModuleEntryPointGenerator {


    public static void main(String[] args) throws IOException {


        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        if (args.length < 2) {
            System.err.println("Argument" + args[0] + " must be an target path.");
            System.err.println("Argument" + args[1] + " must be a module to analyse.");
            System.exit(1);
        }


        String targetPath = args[0];

        for (int i = 1; i < args.length; i++) {
            String moduleName = args[i];


            System.out.println("[Start] Create WALA Thread Entrypoint for Module " + moduleName);
            Path path = fs.getPath("modules", moduleName);


            Path tmpPath = Files.createTempDirectory(Long.toString(System.nanoTime()));
            Path targetDir = tmpPath.resolve(moduleName);
            if (!Files.exists(targetDir)) {
                try {
                    Files.createDirectory(targetDir);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }

            Files.walkFileTree(path, new CopyDirVisitor(path, targetDir));

            GenerateModule2ThreadEntryPoint entryPoint = new GenerateModule2ThreadEntryPoint(moduleName, tmpPath.toString(), targetPath);
            entryPoint.generateEntryPoint();


            Files.walkFileTree(tmpPath, new DeleteDirVistor());
        }

    }


    public static class CopyDirVisitor extends SimpleFileVisitor<Path> {

        private Path source;
        private Path target;
        private StandardCopyOption copyOption = StandardCopyOption.REPLACE_EXISTING;

        public CopyDirVisitor(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {


            Path targetDirectory = target.resolve(source.relativize(dir).toString());
            try {
                Files.copy(dir, targetDirectory);
            } catch (FileAlreadyExistsException e) {
                if (!Files.isDirectory(targetDirectory)) {
                    throw e;
                }
            }
            return FileVisitResult.CONTINUE;

        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, target.resolve(source.relativize(file).toString()), copyOption);
            return FileVisitResult.CONTINUE;

        }
    }

    public static class DeleteDirVistor extends SimpleFileVisitor<Path> {
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

    }

}
