package com.hackdiary.gmail;

import com.google.common.io.Files;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;

public class FileSources implements Sources {
  File directory;
  Set<String> importantLabels;

  public FileSources(String dir, Set<String> importantLabels) throws IOException {
    this.directory = new File(dir);
    this.importantLabels = importantLabels;
    if (!this.directory.isDirectory()) {
      throw new IOException(dir + " is not a directory");
    }
  }

  File[] getFilenames() {
    FilenameFilter fileFilter = new WildcardFileFilter("*.txt");
    return this.directory.listFiles(fileFilter);
  }

  public List<String> getLabelnames() throws IOException {
    return Stream.of(this.getFilenames())
        .map(File::getName)
        .map(FilenameUtils::getBaseName)
        .collect(Collectors.toList());
  }

  public List<Address> getAddresses(Map<String, String> labelIds) throws IOException {
    var addresses = new LinkedList<Address>();
    for (var f : this.getFilenames()) {
      var labelName = FilenameUtils.getBaseName(f.getName());
      Files.readLines(f, Charset.forName("UTF-8"))
          .stream()
          .map(
              line -> {
                var a = new Address();
                a.labelId = labelIds.get(labelName);
                a.email = line;
                if (labelName.startsWith("Lists")) {
                  a.skipInbox = true;
                }
                if (this.importantLabels.contains(labelName)) {
                  a.important = true;
                }
                return a;
              })
          .forEach(addresses::add);
    }
    return addresses;
  }
}
