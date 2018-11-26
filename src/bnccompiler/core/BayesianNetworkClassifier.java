package bnccompiler.core;

/** Import statements necessary for il2 classes. */
import il2.model.*;

import java.util.*;

public class BayesianNetworkClassifier
{
  BayesianNetwork bn;
  String root;
  String[] features;
  double threshold;

  public BayesianNetworkClassifier(BayesianNetwork bn, String root, String[] features, double threshold) {
    this.bn = bn;
    this.root = root;
    this.features = features;
    this.threshold = threshold;    
  }

  public BayesianNetwork getBayesianNetwork() {
    return this.bn;
  }
  public String getRoot() {
    return this.root;
  }
  public String[] getFeatures() {
    return this.features;
  }
  public double getThreshold() {
    return this.threshold;
  }

  public void setBayesianNetwork(BayesianNetwork bn) {
    this.bn = bn;
  }
  public void setRoot(String root) {
    this.root = root;
  }
  public void setFeatures(String[] features) {
    this.features = features.clone();
  }
  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }
}
