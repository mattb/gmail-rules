import com.hackdiary.gmail.*;
import java.util.Set;

class Config {
  static String mode = "s3"; // or "dir"
  static Set<String> IMPORTANT_LABELS = Set.of("people", "work");
  static boolean REBUILD_ALL = false;
  static String S3_BUCKET_NAME = "mattb-mail-filters";
  static String FILTER_DIR = "/Users/mattb/Setup/sieve";
}

public class App {
  public static void main(String[] args) throws Exception {
    var gm = GmailClient.getGmail();
    var sync = new GmailSync(gm);

    Sources sources;
    if (Config.mode.equals("s3")) {
      sources = new S3Sources(Config.S3_BUCKET_NAME, Config.IMPORTANT_LABELS);
    } else {
      sources = new FileSources(Config.FILTER_DIR, Config.IMPORTANT_LABELS);
    }
    var labelIds = sync.ensureLabels(sources.getLabelnames());

    var addresses = new Addresses("x@y.z");
    for (Address a : sources.getAddresses(labelIds)) {
      addresses.add(a);
    }

    sync.ensureFilters(addresses.buildFilters(), addresses::isRuleQuery, Config.REBUILD_ALL);
  }
}
