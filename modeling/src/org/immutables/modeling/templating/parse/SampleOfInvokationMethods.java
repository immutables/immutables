package org.immutables.modeling.templating.parse;

class SampleOfInvokationMethods {
  interface Call<R> {

  }

  interface Binary<R> {

  }

  Binary<Boolean> eq;

  Call<Integer> sum;

  void dd() {
    apply(1, eq, 2);
    apply(sum, 2, 4);
  }

  <R> R apply(Call<R> kkk, Object o, Object j) {
    return null;
  }

  <R> R apply(Object o, Binary<R> b, Object g) {
    return null;
  }
}
