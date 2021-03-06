package com.hackdiary.gmail;

import com.google.api.services.gmail.model.Filter;
import com.google.api.services.gmail.model.FilterAction;
import com.google.api.services.gmail.model.FilterCriteria;
import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.hash.Hashing;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class Addresses {
  List<Address> addresses = new LinkedList<Address>();

  String shibboleth;

  public Addresses(String shibboleth) {
    this.shibboleth = shibboleth;
  }

  public void add(Address a) {
    addresses.add(a);
  }

  public List<Filter> buildFilters() {
    List<Filter> filters = new LinkedList<Filter>();
    var addressesByLabel = Multimaps.index(addresses, a -> a.labelId);

    for (var labelId : addressesByLabel.keySet()) {
      var addresses = addressesByLabel.get(labelId);
      filters.addAll(this.buildFilter(labelId, addresses));
    }
    return filters;
  }

  Function<String, Integer> partioner(int partitionCount) {
    var hasher = Hashing.murmur3_32();
    var utf = Charset.forName("UTF-8");

    return s ->
        (Math.abs(hasher.hashString(s, utf).asInt()) / (Integer.MAX_VALUE / partitionCount));
  }

  public boolean isRuleQuery(String query) {
    return query != null && query.contains(this.shibboleth);
  }

  String addressesToQuery(Iterable<String> emails) {
    var joinedEmails = String.join(" ", emails);
    return "from:{" + joinedEmails + " " + this.shibboleth + "} OR to:{" + joinedEmails + "}";
  }

  Filter makeFilter(String labelId, boolean skipInbox, boolean important, Iterable<String> emails) {
    var addLabels = new LinkedList<String>();
    addLabels.add(labelId);
    if (important) {
      addLabels.add("IMPORTANT");
    }

    var skipSpam = true;
    var removeLabels = new LinkedList<String>();
    if (skipInbox) {
      removeLabels.add("INBOX"):
    }
    if (skipSpam) {
      removeLabels.add("SPAM"):
    }

    var f = new Filter();

    var c = new FilterCriteria();
    c.setQuery(this.addressesToQuery(emails));
    f.setCriteria(c);

    var a = new FilterAction();
    a.setAddLabelIds(addLabels);
    a.setRemoveLabelIds(removeLabels);
    f.setAction(a);

    return f;
  }

  List<Filter> buildFilter(String labelId, List<Address> addresses) {
    boolean skipInbox = false;
    boolean important = false;
    if (addresses.get(0).skipInbox) {
      skipInbox = true;
    }
    if (addresses.get(0).important) {
      important = true;
    }
    List<Filter> filters = new LinkedList<Filter>();
    List<String> emails = addresses.stream().map(a -> a.email).collect(Collectors.toList());
    TreeMultimap<Integer, String> partitions = TreeMultimap.create();

    int partitionCount = 0;
    boolean done = false;
    while (!done) {
      partitionCount++;
      partitions = TreeMultimap.create(Multimaps.index(emails, this.partioner(partitionCount)));
      done = true;
      for (var k : partitions.keySet()) {
        if (this.addressesToQuery(partitions.get(k)).length() > 1500) {
          done = false;
        }
      }
    }
    for (var k : partitions.keySet()) {
      filters.add(this.makeFilter(labelId, skipInbox, important, partitions.get(k)));
    }
    return filters;
  }
}
