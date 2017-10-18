package org.immutables.mongo.repository.internal;

import com.google.gson.internal.bind.DateTypeAdapter;
import org.junit.Test;

import java.util.Date;

import static org.immutables.check.Checkers.check;

public class SupportTest {

  @Test
  public void unwrapPrimitives() throws Exception {
    check(Support.unwrapBsonable(101)).is(101);
    check(Support.unwrapBsonable(null)).isNull();
    check(Support.unwrapBsonable(22D)).is(22D);
    check(Support.unwrapBsonable(33L)).is(33L);
  }

  @Test
  public void unwrapObject() throws Exception {
    Date date = new Date();
    Support.Adapted<Date> adapted = new Support.Adapted<>(new DateTypeAdapter(), date);
    Object result = Support.unwrapBsonable(adapted);
    check(result.toString()).isNonEmpty();
  }
}