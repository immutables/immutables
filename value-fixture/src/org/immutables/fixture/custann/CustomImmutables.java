package org.immutables.fixture.custann;

public class CustomImmutables {

  @CustomWithStyle
  interface Od {}
  
  @CustomWithStyle2
  interface Dv {}

  void use() {
    CustoOd.builder().build();
    JcIvDv.builder().build();
  }
}
