package bnccompiler;

import bnccompiler.core.*;

/** Import statements necessary for il2 classes. */
import il2.bridge.*;
import il2.io.Uai;
import il2.model.*;
import il2.model.Table;
import il2.util.*;

/** Import standard Java classes. */
import java.util.*;
import java.lang.*;
import java.io.*;

public class CompileClassifier 
{
  CompileClassifierConfig config;
  BayesianNetworkClassifier bnc;
  BayesianNetworkClassifierCompilationOrder bnc_compilation_order;
  String output_filename;

  int num_vars;
  long model_count;
  long model_count_progress;
  HashMap<OddNode, String> node_name;

  static int ODD_SIZE = 0;

  private static String getDefaultOutputFilename(CompileClassifierConfig config) {
    return config.getOutput_filepath() + config.getName() + ".odd";
  }

  public CompileClassifier(CompileClassifierConfig config) {
    this(config, getDefaultOutputFilename(config));
  }
  public CompileClassifier(CompileClassifierConfig config, String custom_output_filename) {
    this.config = config;
    this.output_filename = custom_output_filename;

    BayesianNetwork bn = null;
    String input_filename = this.config.getInput_filepath() + this.config.getName() + "." + this.config.getFiletype();
    try {
      switch (this.config.getFiletype()) {
        case "net":
          bn = IO.readNetwork(input_filename);
          break;
        case "uai":
          bn = Uai.uaiToBayesianNetwork(input_filename);
          break;
        default:
          bn = null;
      }
      if (bn == null) {
        throw new Exception("File type is unsupported: " + this.config.getFiletype());
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    this.bnc = new BayesianNetworkClassifier(
      bn,
      config.getRoot(),
      config.getLeaves(),
      Double.parseDouble(config.getThreshold())
    );
    this.bnc_compilation_order = new BayesianNetworkClassifierCompilationOrder(bnc);
  }

  public void run(boolean logging) {
    long start = System.nanoTime();
    compileAndWriteOdd(this.bnc, this.bnc_compilation_order, this.output_filename);
    long stop = System.nanoTime();

    if (logging) {
      int largest_feature_group = 0;
      int compile_width = 0;
      for (int j = 0; j < this.bnc_compilation_order.getBlock_order().length; j++) {
        largest_feature_group = Math.max(largest_feature_group, this.bnc_compilation_order.getBlock_order()[j].length);
        
        int width_before = 0;
        int width_after = 0;
        for (int k = 0; k < this.bnc_compilation_order.getBlock_order().length; k++) {
          if (k < j) { width_before += this.bnc_compilation_order.getBlock_order()[k].length; }
          else { width_after += this.bnc_compilation_order.getBlock_order()[k].length; }
        }
        compile_width = Math.max(compile_width, this.bnc_compilation_order.getBlock_order()[j].length + Math.min(width_before, width_after));
      }
      System.out.println(
        config.getName() + // network name
        " & " + this.bnc_compilation_order.getH_order()[0][0] + // class name
        " & " + this.bnc_compilation_order.getFeature_order().length + // number of features
        " & " + compile_width + // block order width
        " & " + largest_feature_group + // largest feature block
        " & " + (this.bnc_compilation_order.getH_order().length - 1) + // number of blocks
        " & " + ODD_SIZE + // ODD size
        " & " + (stop-start)/1000000000L + "\\\\" // compile time
      );
    }
  }

  private void compileAndWriteOdd(BayesianNetworkClassifier bnc, BayesianNetworkClassifierCompilationOrder compilation_order, String output_filename) {
    BayesianNetworkClassifierToOdd compiler = new BayesianNetworkClassifierToOdd(bnc, compilation_order);
    OddNode result = compiler.compile();
    writeOddToFile(result, compilation_order, output_filename);
  }

  public void writeOddToFile(OddNode root, BayesianNetworkClassifierCompilationOrder compilation_order, String filename) {
    ODD_SIZE = 0;
    File file = new File(filename);
    try {
      PrintWriter writer = new PrintWriter(file);

      writer.println(Arrays.toString(compilation_order.getFeature_order()));

      this.node_name = new HashMap<OddNode,String>();
      writeOddToFileHelper(root, writer);
      writer.close();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  private void writeOddToFileHelper(OddNode root, PrintWriter writer) {
    if (this.node_name.containsKey(root)) {
      return;
    }

    if (root.getType() == OddNode.NodeType.SINK) {
      this.node_name.put(root, "S" + String.valueOf(root.getSinkNum()));
    }
    else {
      String cur = String.valueOf(this.node_name.size());
      this.node_name.put(root, cur);

      for (int i = 0; i < root.getCardinality(); i++) {
        writeOddToFileHelper(root.getChild(i), writer);
      }
      writer.print(cur);
      for (int i = 0; i < root.getCardinality(); i++) {
        writer.print(" " + this.node_name.get(root.getChild(i)));
      }
      writer.println(" " + root.getLabel());
      ODD_SIZE += 1;
    }
  }
}

