import com.google.common.io.Files;
import com.hackdiary.gmail.*;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;

public class App {
  public static void main(String[] args) throws Exception {
    var gm = GmailClient.getGmail();
    var sync = new GmailSync(gm);

    var dir = new File("/Users/mattb/Setup/sieve");
    FilenameFilter fileFilter = new WildcardFileFilter("*.txt");
    var filenames = dir.listFiles(fileFilter);

    var labelNames = new LinkedList<String>();
    Stream.of(filenames)
        .map(f -> f.getName())
        .map(FilenameUtils::getBaseName)
        .forEach(labelNames::add);

    var labels = sync.ensureLabels(labelNames);

    var addresses = new Addresses();
    for (var f : filenames) {
      var labelName = FilenameUtils.getBaseName(f.getName());
      Files.readLines(f, Charset.forName("UTF-8"))
          .stream()
          .map(
              line -> {
                var a = new Address();
                a.labelId = labels.get(labelName);
                a.email = line;
                if (labelName.startsWith("Lists")) {
                  a.skipInbox = true;
                }
                return a;
              })
          .forEach(addresses::add);
    }

    sync.ensureFilters(addresses.buildFilters());
  }
}
