package com.hackdiary.gmail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Sources {
  public List<String> getLabelnames() throws IOException;

  public List<Address> getAddresses(Map<String, String> labelIds) throws IOException;
}
