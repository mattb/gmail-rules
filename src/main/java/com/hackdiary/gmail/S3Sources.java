package com.hackdiary.gmail;

import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class S3Sources implements Sources {
  String bucketName;
  AmazonS3 client;
  Set<String> importantLabels;

  public S3Sources(String bucketName, Set<String> importantLabels) throws IOException {
    this.importantLabels = importantLabels;
    this.bucketName = bucketName;
    this.client =
        AmazonS3ClientBuilder.standard()
            .withRegion(com.amazonaws.regions.Regions.US_EAST_1)
            .build();
    if (!this.client.doesBucketExistV2(bucketName)) {
      throw new IOException(bucketName + " does not exist");
    }
  }

  public List<String> getLabelnames() throws IOException {
    var req = new ListObjectsV2Request().withBucketName(this.bucketName).withDelimiter("/");
    var result = this.client.listObjectsV2(req);
    return result
        .getCommonPrefixes()
        .stream()
        .map(s -> s.substring(0, s.length() - 1))
        .collect(Collectors.toList());
  }

  public List<Address> getAddresses(Map<String, String> labelIds) throws IOException {
    var addresses = new LinkedList<Address>();
    for (var labelName : this.getLabelnames()) {
      var req =
          new ListObjectsV2Request()
              .withBucketName(this.bucketName)
              .withDelimiter("/")
              .withPrefix(labelName + "/");
      var result = this.client.listObjectsV2(req);
      for (var objectSummary : result.getObjectSummaries()) {
        var object = this.client.getObject(objectSummary.getBucketName(), objectSummary.getKey());
        new BufferedReader(new InputStreamReader(object.getObjectContent()))
            .lines()
            .map(
                line -> {
                  var a = new Address();
                  a.labelId = labelIds.get(labelName);
                  a.email = line;
                  if (labelName.startsWith("Lists")) {
                    a.skipInbox = true;
                  }
                  if (this.importantLabels.contains(labelName)) {
                    a.important = true;
                  }
                  return a;
                })
            .forEach(addresses::add);
      }
    }
    return addresses;
  }
}
