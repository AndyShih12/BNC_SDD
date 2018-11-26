package bnccompiler.core;

import java.lang.Double;

public class OddNode {
  public enum NodeType { NORMAL, SINK }

  OddNode[] children;
  int label;
  NodeType type;
  double[] vector;
  long id;
  int sink_num;

  public static long counter = 1;

  public OddNode(int label, double[] vector, int card, NodeType type) {
    this.children = new OddNode[card];
    this.label = label;
    this.vector = vector.clone();
    this.type = type;

    this.id = counter;
    counter += 1;
  }

  @Override
  public int hashCode() {
    int code = 0;
    for (OddNode child : this.children) {
      code ^= (child == null) ? 0 : (int)child.id;
    }
    return code;
  }
  @Override
  public boolean equals(Object obj) {
    if (obj==null || !(obj instanceof OddNode)) {
      return false;
    }
    OddNode other = ( OddNode ) obj;

    if (this.type != other.type) {
      return false;
    }
    if (this.label != other.label) {
      return false;
    }
    if (this.sink_num != other.sink_num) {
      return false;
    }
    if (this.children.length != other.children.length) {
      return false;
    }
    for (int i = 0; i < this.children.length; i++) {
      long idt = this.children[i] == null ? 0L : this.children[i].id;
      long ido = other.children[i] == null ? 0L : other.children[i].id;
      if (idt != ido) {
        return false;
      }
    }

    return true;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getId() {
    return this.id;
  }

  public void setSinkNum(int sink_num) {
    this.sink_num = sink_num;
  }

  public int getSinkNum() {
    return this.sink_num;
  }


  public void setChild(int val, OddNode child) {
    this.children[val] = child;
  }

  public OddNode getChild(int index) {
    return this.children[index];
  }

  public int getCardinality() {
    return this.children.length;
  }

  public NodeType getType() {
    return this.type;
  }

  public void setVector(double[] vector) {
    this.vector = vector.clone();
  }

  public double[] getVector() {
    return this.vector;
  }

  public int getLabel() {
    return this.label;
  }
}


