import com.google.common.io.Files;
import com.hackdiary.gmail.*;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
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

    var labelNames =
        Stream.of(filenames)
            .map(File::getName)
            .map(FilenameUtils::getBaseName)
            .collect(Collectors.toList());

    var labels = sync.ensureLabels(labelNames);

    var addresses = new Addresses("x@y.z");
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
                if (labelName.equals("people")) {
                  a.important = true;
                }
                return a;
              })
          .forEach(addresses::add);
    }

    sync.ensureFilters(addresses.buildFilters(), addresses::isRuleQuery, false);
  }
}
