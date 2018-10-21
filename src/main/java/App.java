import com.google.common.io.Files;
import com.hackdiary.gmail.*;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;

public class App {
  public static void main(String[] args) throws Exception {
    var gm = GmailClient.getGmail();
    var sync = new GmailSync(gm);

    var dir = new File("/Users/mattb/Setup/sieve");
    FilenameFilter fileFilter = new WildcardFileFilter("*.txt");
    var labelNames = new LinkedList<String>();
    for (var f : dir.listFiles(fileFilter)) {
      labelNames.add(FilenameUtils.getBaseName(f.getName()));
    }

    var labels = sync.ensureLabels(labelNames);
    var addresses = new Addresses();
    for (var f : dir.listFiles(fileFilter)) {
      for (var l : Files.readLines(f, Charset.forName("UTF-8"))) {
        var labelName = FilenameUtils.getBaseName(f.getName());
        var a = new Address();
        a.labelId = labels.get(labelName);
        a.email = l;
        if (labelName.startsWith("Lists")) {
          a.skipInbox = true;
        }
        addresses.add(a);
      }
    }

    sync.ensureFilters(addresses.buildFilters());
  }
}
