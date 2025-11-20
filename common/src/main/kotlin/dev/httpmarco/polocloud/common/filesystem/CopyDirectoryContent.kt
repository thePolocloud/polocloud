package dev.httpmarco.polocloud.common.filesystem

import java.io.IOException
import java.nio.file.CopyOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

fun copyDirectory(sourcePath: Path, targetPath: Path) {
    Files.walkFileTree(sourcePath, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            val targetDir = targetPath.resolve(sourcePath.relativize(dir))
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir)
            }
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            try {
                Files.copy(
                    file,
                    targetPath.resolve(sourcePath.relativize(file)),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: IOException) {
                System.err.println("Cannot copy file ${file.fileName}: ${e.message}")
            }
            return FileVisitResult.CONTINUE
        }
    })
}

fun deleteDirectory(path: Path) {
    if (!Files.exists(path)) {
        return
    }
    try {
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    } catch (e: IOException) {
        e.printStackTrace()
    }
}