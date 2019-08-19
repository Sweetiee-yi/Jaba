package com.soud.jaba;

import java.util.Objects;

public class Pair<K, V> {
  private K key;
  private V value;

  public K getKey() {
    return key;
  }

  public void setKey(K key) {
    this.key = key;
  }

  public V getValue() {
    return value;
  }

  public void setValue(V value) {
    this.value = value;
  }

  public Pair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public static <K, V> Pair<K, V> of(K key, V value) {
    return new Pair<>(key, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(key, pair.key) &&
        Objects.equals(value, pair.value);
  }

  @Override
  public String toString() {
    return "Pair{" +
        "key=" + key +
        ", value=" + value +
        '}';
  }

  @Override
  public int hashCode() {
    return (this.getKey() == null ? 0 : this.getKey().hashCode()) ^ (this.getValue() == null ? 0 : this.getValue().hashCode());
  }
}

