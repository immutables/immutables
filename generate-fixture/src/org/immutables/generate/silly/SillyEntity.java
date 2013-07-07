/**
 * Propritary and confidential. All rights reserved by Hotwire Inc.
 */
package org.immutables.generate.silly;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.primitives.UnsignedInteger;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.annotation.GenerateDerived;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;

@GenerateImmutable
@GenerateMarshaler(importRoutines = { SillyEntity.class })
@GenerateRepository
public abstract class SillyEntity {

  @GenerateMarshaledAs("_id")
  public abstract int id();

  @GenerateMarshaledAs("v")
  public abstract String val();

  @GenerateMarshaledAs("p")
  public abstract Map<String, Integer> payload();

  @GenerateMarshaledAs("i")
  public abstract List<Integer> ints();

  @GenerateDerived
  public UnsignedInteger der() {
    return UnsignedInteger.valueOf(1);
  }

  public static void marshal(JsonGenerator generator, UnsignedInteger integer) throws IOException {
    generator.writeNumber(integer.doubleValue());
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param integerNull the integer null
   * @param expectedClass the expected class
   * @return the unsigned integer
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static UnsignedInteger unmarshal(
      JsonParser parser,
      @Nullable UnsignedInteger integerNull,
      Class<UnsignedInteger> expectedClass)
      throws IOException {
    return UnsignedInteger.valueOf((long) parser.getDoubleValue());
  }
}
