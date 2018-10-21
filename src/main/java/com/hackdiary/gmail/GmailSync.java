package com.hackdiary.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Filter;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.LabelColor;
import com.google.api.services.gmail.model.ListLabelsResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GmailSync {
  String[] colors =
      new String[] {
        "#000000", "#434343", "#666666", "#999999", "#cccccc", "#efefef", "#f3f3f3", "#ffffff",
        "#fb4c2f", "#ffad47", "#fad165", "#16a766", "#43d692", "#4a86e8", "#a479e2", "#f691b3",
        "#f6c5be", "#ffe6c7", "#fef1d1", "#b9e4d0", "#c6f3de", "#c9daf8", "#e4d7f5", "#fcdee8",
        "#efa093", "#ffd6a2", "#fce8b3", "#89d3b2", "#a0eac9", "#a4c2f4", "#d0bcf1", "#fbc8d9",
        "#e66550", "#ffbc6b", "#fcda83", "#44b984", "#68dfa9", "#6d9eeb", "#b694e8", "#f7a7c0",
        "#cc3a21", "#eaa041", "#f2c960", "#149e60", "#3dc789", "#3c78d8", "#8e63ce", "#e07798",
        "#ac2b16", "#cf8933", "#d5ae49", "#0b804b", "#2a9c68", "#285bac", "#653e9b", "#b65775",
        "#822111", "#a46a21", "#aa8831", "#076239", "#1a764d", "#1c4587", "#41236d", "#83334c"
      };
  private Gmail service;

  public GmailSync(Gmail service) {
    this.service = service;
  }

  private String contrastFor(String color) {
    var c = java.awt.Color.decode(color);
    if (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114 > 186) {
      return "#000000";
    }
    return "#ffffff";
  }

  public Map<String, String> ensureLabels(List<String> labelNames) throws IOException {
    var labels = getLabels().keySet();
    for (var name : labelNames) {
      if (!labels.contains(name)) {
        var color = new LabelColor();
        color.setBackgroundColor(colors[Math.abs(name.hashCode()) % colors.length]);
        color.setTextColor(contrastFor(colors[Math.abs(name.hashCode()) % colors.length]));

        var l = new Label();
        l.setName(name);
        l.setType("user");
        l.setColor(color);
        l.setLabelListVisibility("labelShow");
        l.setMessageListVisibility("show");

        this.service.users().labels().create("me", l).execute();
        System.out.println("Creating label " + name);
      }
    }
    return this.getLabels();
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

  public void ensureFilters(List<Filter> filters) throws IOException {
    List<Filter> toDelete = new LinkedList<Filter>();
    Set<Filter> toAdd = new HashSet<Filter>(filters);
    for (var existingFilter :
        this.service.users().settings().filters().list("me").execute().getFilter()) {
      var found = false;
      for (var filter : filters) {
        if (filter.getCriteria().getQuery().equals(existingFilter.getCriteria().getQuery())
            && filter
                .getAction()
                .getAddLabelIds()
                .equals(existingFilter.getAction().getAddLabelIds())) {
          found = true;
          toAdd.remove(filter);
        }
      }
      if (!found) {
        toDelete.add(existingFilter);
      }
    }
    for (var f : toDelete) {
      this.service.users().settings().filters().delete("me", f.getId()).execute();
      System.out.println("Deleted " + f.getId());
    }
    for (var f : toAdd) {
      var result = this.service.users().settings().filters().create("me", f).execute();
      System.out.println("Created " + result.getId());
    }
  }
}
