/*
 * Copyright (c) [2016] [ <ether.camp> ] This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with the ethereumJ
 * library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.erachain.dbs.rocksDB.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j //(topic = "utils")
public class FileUtil {

  public static List<String> recursiveList(String path) throws IOException {

    final List<String> files = new ArrayList<>();

    Files.walkFileTree(Paths.get(path), new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        files.add(file.toString());
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    });

    return files;
  }

  public static boolean recursiveDelete(String fileName) {
    File file = new File(fileName);
    if (file.exists()) {
      // check if the file is a directory
      if (file.isDirectory()) {
        // call deletion of file individually
        Arrays.stream(Objects.requireNonNull(file.list()))
            .map(s -> fileName + System.getProperty("file.separator") + s)
            .forEachOrdered(FileUtil::recursiveDelete);
      }

      if (!file.setWritable(true)){
        logger.warn("failed to setWritable: " + fileName);
      }
      
      return file.delete();
    }
    return false;
  }

  public static void saveData(String filePath, String data, boolean append) {
    File priFile = new File(filePath);
    try {
      if (!priFile.createNewFile()){
        logger.warn("failed to create new file: " + filePath);
        return;
      }

      try (BufferedWriter bw = new BufferedWriter(new FileWriter(priFile, append))) {
        bw.write(data);
        bw.flush();
      }
    } catch (IOException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public static int readData(String filePath, char[] buf) {
    int len;
    File file = new File(filePath);
    try (BufferedReader bufRead = new BufferedReader(new FileReader(file))) {
      len = bufRead.read(buf, 0, buf.length);
    } catch (IOException ex) {
      logger.warn(ex.getMessage());
      return 0;
    }
    return len;
  }

  /**
   * delete directory.
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          logger.warn("can't delete dir:" + dir);
          return false;
        }
      }
    }
    return dir.delete();
  }

  public static boolean createFileIfNotExists(String filepath) {
    File file = new File(filepath);
    if (!file.exists()) {
      try {
        if (!file.createNewFile()){
          logger.warn("failed to create new file" + filepath);
        }
      } catch (Exception e) {
        return false;
      }
    }
    return true;
  }

  public static boolean createDirIfNotExists(String dirPath) {
    File dir = new File(dirPath);
    if (!dir.exists()) {
      return dir.mkdirs();
    }
    return true;
  }
}
