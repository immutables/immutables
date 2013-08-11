package org.immutables.common.collect;

class Ord implements OrdinalValue<Ord> {
  private final Domain domain;
  private final int ordinal;

  public Ord(Domain domain, int ordinal) {
    this.domain = domain;
    this.ordinal = ordinal;
  }

  @Override
  public int ordinal() {
    return ordinal;
  }

  @Override
  public OrdinalDomain<Ord> ordinalDomain() {
    return domain;
  }
}
