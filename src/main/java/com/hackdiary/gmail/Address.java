package com.hackdiary.gmail;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Address {
  public String email;
  public String labelId;
  public boolean skipInbox;
  public boolean important;

  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
