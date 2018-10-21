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

  String addressesToQuery(Iterable<String> emails) {
    return "from:{" + String.join(" ", emails) + "}";
  }

  Filter makeFilter(String labelId, boolean skipInbox, Iterable<String> emails) {
    var f = new Filter();

    var c = new FilterCriteria();
    c.setQuery(this.addressesToQuery(emails));
    f.setCriteria(c);

    var a = new FilterAction();
    a.setAddLabelIds(List.of(labelId));
    if (skipInbox) {
      a.setRemoveLabelIds(List.of("INBOX"));
    }
    f.setAction(a);

    return f;
  }

  List<Filter> buildFilter(String labelId, List<Address> addresses) {
    boolean skipInbox = false;
    if (addresses.get(0).skipInbox) {
      skipInbox = true;
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
      filters.add(this.makeFilter(labelId, skipInbox, partitions.get(k)));
    }
    return filters;
  }
}
