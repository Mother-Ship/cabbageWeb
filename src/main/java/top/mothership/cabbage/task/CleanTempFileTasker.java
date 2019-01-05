package top.mothership.cabbage.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Component
public class CleanTempFileTasker {
    private Logger logger = LogManager.getLogger(this.getClass());
    /**
     * 清理每天生成的临时文件。
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get("/root/coolq/data/image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path2 = Paths.get("/root/coolq/data/record");
        SimpleFileVisitor<Path> finder2 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path3 = Paths.get("/root/coolq2/data/image");
        SimpleFileVisitor<Path> finder3 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path4 = Paths.get("/root/coolq2/data/record");
        SimpleFileVisitor<Path> finder4 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };

        try {
            Files.walkFileTree(path, finder);
            Files.walkFileTree(path2, finder2);
            Files.walkFileTree(path3, finder3);
            Files.walkFileTree(path4, finder4);
        } catch (IOException e) {
            logger.error("清空临时文件时出现异常，" + e.getMessage());
        }

    }

}
