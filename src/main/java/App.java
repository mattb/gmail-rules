import com.google.common.io.Files;
import com.hackdiary.gmail.*;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import org.apache.commons.io.filefilter.*;

public class App {
  public String getGreeting() {
    return "Hello world.";
  }

  public static void main(String[] args) throws java.io.IOException {
    var dir = new File("/Users/mattb/Setup/sieve");
    FilenameFilter fileFilter = new WildcardFileFilter("*.txt");
    var addresses = new Addresses();
    for (var f : dir.listFiles(fileFilter)) {
      for (var l : Files.readLines(f, Charset.forName("UTF-8"))) {
        var a = new Address();
        a.labelId = f.getName();
        a.email = l;
        addresses.add(a);
      }
    }
    for (var f : addresses.buildFilters()) {
      System.out.println(f);
      System.out.println();
    }
  }
}
