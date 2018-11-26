package bnccompiler.core;

/** Import statements necessary for il2 classes. */
import il2.model.*;
import il2.model.Table;
import il2.util.*;

import java.util.*;

public class BayesianNetworkClassifierCompilationOrder
{
  String[] feature_order;
  String[][] h_order;
  String[][] block_order;
  int[] block_order_sz;
  String[][] node_order;

  public BayesianNetworkClassifierCompilationOrder(BayesianNetworkClassifier bnc) {
    Table[] cpts = bnc.getBayesianNetwork().cpts();
    String root = bnc.getRoot();
    String[] features = bnc.getFeatures();

    if (features.length == 0) {
      features = null;
    }

    OrderHeuristic oh = new OrderHeuristic();
    OrderHeuristic.GraphOrder result = oh.run(cpts, root, features); //if features is null then defaults to leaves in the BN
    String[][] h_order = new String[result.class_order.length][];
    for (int i = 0; i < result.class_order.length; i++) {
      h_order[i] = new String[]{ result.class_order[i] };
    }
    setH_order(h_order);
    setFeature_order(result.feature_order);
    setBlock_order(result.block_order);
    setNode_order(result.node_order);
    

    /*
    // set custom order
    setH_order(new String[][] {
      {"S1"},{"S1"},{"S1"},
      {"S3","S4","S5"},
      {"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},{"S3","S4","S5"},
      {"S6","S7","S8"},
      {"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},{"S6","S7","S8"},
      {"S2","S6","S7","S8"},{"S2","S6","S7","S8"},{"S2","S6","S7","S8"},{"S2","S6","S7","S8"},{"S2","S6","S7","S8"}
    });
    setFeature_order(new String[] {
      "u1","u2","u17","u6","u8_3","u4","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"
    });
    setBlock_order(new String[][] {
      {}, {"u1"}, {"u2"},
      {"u17", "u6", "u8_3"},
      {"u4"},{"u5"},{"u10"},{"u11"},{"u16_1"},{"u16_2"},{"u16_3"},{"u16_4"},{"u18"},
      {"u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3"},
      {"u9_1"},{"u9_2"},{"u13"},{"u14"},{"u21"},{"u22"},{"u23"},{"u24"},
      {"u19"},{"u25_1"},{"u25_2"},{"u25_3"},{"u25_4"}
    });
    setNode_order(new String[][] {
      {"S1","S2","S3","S4","S5","S6","S7","S8","u1","u2","u17","u6","u8_3","u4","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S1","S2","S3","S4","S5","S6","S7","S8","u2","u17","u6","u8_3","u4","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S1","S2","S3","S4","S5","S6","S7","S8","u17","u6","u8_3","u4","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u4","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u5","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u10","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u11","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u16_1","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u16_2","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u16_3","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u16_4","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u18","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S3","S4","S5","S6","S7","S8","u3","u7","u8_1","u8_2","u12","u15","u20","u26_1","u26_2","u26_3","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u9_1","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u9_2","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u13","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u14","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u21","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u22","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u23","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u24","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u19","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u25_1","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u25_2","u25_3","u25_4"},
      {"S2","S6","S7","S8","u25_3","u25_4"},
      {"S2","S6","S7","S8","u25_4"},
      {"S2","S6","S7","S8"}
    });
    */
    printCompilationOrder();
  }

  public String[][] getH_order() {
    return this.h_order;
  }
  public String[] getFeature_order() {
    return this.feature_order;
  }
  public String[][] getBlock_order() {
    return this.block_order;
  }
  public int[] getBlock_order_sz() {
    return this.block_order_sz;
  }
  public String[][] getNode_order() {
    return this.node_order;
  }

  public void setH_order(String[][] h_order) {
    this.h_order = new String[h_order.length][];
    for (int i = 0; i < h_order.length; i++) {
      this.h_order[i] = h_order[i].clone();
    }
  }
  public void setFeature_order(String[] feature_order) {
    this.feature_order = feature_order.clone();
  }
  public void setBlock_order(String[][] block_order) {
    this.block_order = new String[block_order.length][];
    for (int i = 0; i < block_order.length; i++) {
      this.block_order[i] = block_order[i].clone();
    }

    this.block_order_sz = new int[this.block_order.length];
    if (this.block_order_sz.length > 0) {
      this.block_order_sz[0] = this.block_order[0].length;
    }
    for (int i = 1; i < this.block_order_sz.length; i++) {
      this.block_order_sz[i] = this.block_order_sz[i-1] + this.block_order[i].length;
    }
  }
  public void setNode_order(String[][] node_order) {
    this.node_order = new String[node_order.length][];
    for (int i = 0; i < node_order.length; i++) {
      this.node_order[i] = node_order[i].clone();
    }
  }

  private void printStringArray(String[] arr) {
    System.out.print("{");
    for (int i = 0; i < arr.length; i++) {
      System.out.print("\"" + arr[i] + "\"");
      if (i != arr.length-1) System.out.print(",");
    }
    System.out.println("}");
  }

  private void printCompilationOrder() {
    //print the GraphOrder
    System.out.println("-----Print Compilation Order-----\nh_order:");
    for (int i = 0; i < this.h_order.length; i++) {
      printStringArray(this.h_order[i]);
    }
    System.out.println("\nfeature_order:");
    printStringArray(this.feature_order);

    System.out.println("\nblock_order:");
    for (int i = 0; i < this.block_order.length; i++) {
      printStringArray(this.block_order[i]);
    }
    System.out.println("\nnode_order:");
    for (int i = 0; i < this.node_order.length; i++) {
      printStringArray(this.node_order[i]);
    }
    System.out.println("-----End Compilation Order-----");
  }
}
