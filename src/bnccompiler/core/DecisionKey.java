package bnccompiler.core;

import java.util.*;

class DecisionKey {
  double[] decision;
  public DecisionKey() {
    this.decision = null;
  }
  public DecisionKey(double[] decision) {
    updateDecision(decision);
  }
  public void updateDecision(double[] decision) {
    this.decision = decision.clone();
  }
  @Override
  public int hashCode() {
    int p = 15487469;
    int sum = 0;
    for (int i = 0; i < this.decision.length; i++) {
      sum += this.decision[i];
      sum *= p;
    }
    return sum;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof DecisionKey)) {
      return false;
    }
    DecisionKey other = ( DecisionKey ) obj;
    if (this.decision.length != other.decision.length) {
      return false;
    }
    for (int i = 0; i < this.decision.length; i++) {
      if (this.decision[i] != other.decision[i]) {
        return false;
      }
    }
    return true;
  }
}
