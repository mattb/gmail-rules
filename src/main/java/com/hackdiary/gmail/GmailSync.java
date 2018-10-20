package com.hackdiary.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Filter;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GmailSync {
  private Gmail service;

  public GmailSync(Gmail service) {
    this.service = service;
  }

  public void ensureLabels(List<String> labelNames) throws IOException {
    var labels = getLabels().keySet();
    for (var name : labelNames) {
      if (!labels.contains(name)) {
        var l = new Label();
        l.setName(name);
        this.service.users().labels().create("me", l).execute();
        System.out.println("Creating label " + name);
      }
    }
  }

  public Map<String, String> getLabels() throws IOException {
    var labelToId = new HashMap<String, String>();
    var user = "me";

    ListLabelsResponse listResponse = this.service.users().labels().list(user).execute();
    List<Label> labels = listResponse.getLabels();
    for (Label label : labels) {
      labelToId.put(label.getName(), label.getId());
    }
    return labelToId;
  }

  public void syncFilters(List<String> labelIds, List<Filter> filters) throws IOException {}
}
