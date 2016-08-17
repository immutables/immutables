package nonimmutables;

import org.immutables.value.Value;

@Value.Immutable
public interface HasUmlauts {
  int getZahlungsempfänger();
}
