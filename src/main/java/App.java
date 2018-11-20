import com.hackdiary.gmail.*;
import java.util.Set;

public class App {
  static Set<String> IMPORTANT_LABELS = Set.of("people", "work");
  static boolean REBUILD_ALL = false;

  public static void main(String[] args) throws Exception {
    var gm = GmailClient.getGmail();
    var sync = new GmailSync(gm);

    var sources = new FileSources("/Users/mattb/Setup/sieve", IMPORTANT_LABELS);
    var labelIds = sync.ensureLabels(sources.getLabelnames());

    var addresses = new Addresses("x@y.z");
    for (Address a : sources.getAddresses(labelIds)) {
      addresses.add(a);
    }

    sync.ensureFilters(addresses.buildFilters(), addresses::isRuleQuery, REBUILD_ALL);
  }
}
