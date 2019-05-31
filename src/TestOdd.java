import bnccompiler.*;
import bnccompiler.core.*;

import il2.model.*;
import il2.model.Table;
import il2.util.*;
import il2.bridge.*;
import il2.io.Uai;

/** Import standard Java classes. */
import java.util.*;
import java.lang.*;
import java.io.*;

import java.nio.charset.Charset;
import java.nio.file.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TestOdd
{
  private static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  private static CompileClassifierConfig loadConfig(String config_file) {
    CompileClassifierConfig config = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      String jsonString = readFile(config_file, Charset.forName("UTF-8"));
      config = mapper.readValue(jsonString, CompileClassifierConfig.class);
    } catch (IOException e) {
      System.out.println(e);
    } catch (Exception e) {
      System.out.println(e);
    }
    return config;
  }



  //tests the generated ODD against the classifier decision function
  //the function walks through all instances of the ODD and evaluates them on the classifier,
  //checking that the decision is the same

  private void test(BayesianNetworkClassifier bnc, OddNode root, String[] var_order) {
    BayesianNetwork bn = bnc.getBayesianNetwork();
    Domain d = bn.domain();

    int root_node = d.index( bnc.getRoot() );
    int num_features = var_order.length;
    int[] cardinality = new int[num_features];

    System.out.println("Instance" + "\t" + "ODD verdict" + "\t" + "Classification" + "\t" + "Classifier Posterior Odds");
    boolean pass = true;
    long num_true = 0L;

    long state_space = 1L;
    for (int i = 0; i < num_features; i++) {
      cardinality[i] = d.size( d.index( var_order[i] ) );
      state_space *= cardinality[i];
    }


    for (long i = 0 ; i < state_space; i++) {
      OddNode cur = root;
      int[] instance = new int[num_features];
      long i_prime = i;
      for (int j = 0; j < num_features; j++) {
        instance[j] = (int)(i_prime % cardinality[j]);
        i_prime /= cardinality[j];
      }
      //walks the instance through the ODD until a sink node is reached
      for (int j = 0; j < num_features; j++) {
        cur = cur.getChild(instance[j]);
        if (cur.getType() == OddNode.NodeType.SINK) {
          break;
        }
      }

      int odd_verdict = cur.getSinkNum();

      //System.out.println(Arrays.toString(vars) + Arrays.toString(instance));
      //System.out.println(this.class_order[0]); 
      int[] vars = new int[num_features];
      int[] evid_instance = new int[num_features];
      for (int j = 0; j < num_features; j++) {
        vars[j] = d.index( var_order[j] );
        evid_instance[j] = instance[j]; // double check this is correctly defined
      }
      IntMap evidence = new IntMap(vars, evid_instance);
      Table res = doProbabilityQuery( getInferenceEngine( bn ), evidence, root_node);

      // argmax classification
      //int network_verdict = argmax(res.values());
      // threshold classification
      int threshold_class = 1;
      int network_verdict = (res.values()[threshold_class] >= bnc.getThreshold()) ? 1 : 0;
      num_true += network_verdict;

      if (odd_verdict != network_verdict) {
        pass = false;
      }

      if (odd_verdict != network_verdict) {
        for (int j = 0; j < num_features; j++) {
          System.out.print(instance[j]);
        }
        System.out.println("\t\t" + odd_verdict + "\t\t" + network_verdict + "\t\t" + Arrays.toString(res.values()));
      }
    }

    if (pass) {
      System.out.println("All instances pass :)");
    }
    else {
      System.out.println("At least one instance failed :(");
    }
    System.out.println("Count brute force: " + num_true);
  }

  private il2.inf.JointEngine getInferenceEngine( BayesianNetwork bn) {
    java.util.Random r = new java.util.Random(2018);
    // this is the old, hard-coded way of constructing an ie
    java.util.Collection subdomains=java.util.Arrays.asList(bn.cpts());
    IntList order = il2.inf.structure.EliminationOrders.minFill(subdomains, 6, r).order;
    // this JT construction has quadratic space complexity
    il2.inf.structure.EliminationOrders.JT jt = il2.inf.structure.EliminationOrders.traditionalJoinTree( subdomains, order, null, null );
    // this JT construction has linear space complexity
    //il2.inf.structure.EliminationOrders.JT jt = il2.inf.structure.EliminationOrders.bucketerJoinTree( subdomains, order, null, null );
    // I recommend using a normalizing algorithm, to prevent underflow

    //il2.inf.JointEngine ie = il2.inf.jointree.NormalizedZCAlgorithm.create(bn.cpts(),jt);
    il2.inf.JointEngine ie = il2.inf.jointree.UnindexedSSAlgorithm.create(bn.cpts(),jt);
    return ie;
  }

  /**
    Demonstrates a probability query.
  */
  @SuppressWarnings("unchecked")
  private Table doProbabilityQuery( il2.inf.JointEngine ie, IntMap evidence, int query_var )
  {
    /* Set evidence. */
    ie.setEvidence(evidence);

    Table answer = ie.varConditional( query_var );
    return answer;
  }
  private int argmax(double[] d) {
    int res = 0;
    double best = d[0];
    for (int i = 1; i < d.length; i++) {
      if (d[i] > best) {
        res = i;
        best = d[i];
      }
    }
    return res;
  }


  BayesianNetworkClassifier loadBNC(CompileClassifierConfig config) {
    BayesianNetwork bn = null;
    String input_filename = config.getInput_filepath() + config.getName() + "." + config.getFiletype();
    try {
      switch (config.getFiletype()) {
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
        throw new Exception("File type is unsupported: " + config.getFiletype());
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    return new BayesianNetworkClassifier(
      bn,
      config.getRoot(),
      config.getLeaves(),
      Double.parseDouble(config.getThreshold())
    );
  }

  String[] loadVarOrder(String odd_file) {
    String[] variable_order = null;
    try {
      Scanner sc = new Scanner(new File(odd_file));

      String str = sc.nextLine();
      str = str.replace('[',' ').replace(']',' ');
      str = str.replace(" ","");
      variable_order = str.split(",",-1);
    } catch (FileNotFoundException e) {
      System.out.println(e);
    }

    return variable_order;
  }

  OddNode loadODD(String odd_file) {
    OddNode root = null;
    try {
      Scanner sc = new Scanner(new File(odd_file));
      
      String variable_order = sc.nextLine();
      int offset = 2;

      HashMap<String, OddNode> node = new HashMap<String, OddNode>();
      HashMap<String, ArrayList<String> > map = new HashMap<String, ArrayList<String> >();
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        Scanner scl = new Scanner(line);

        ArrayList<String> tokens = new ArrayList<String>();
        while (scl.hasNext()) {
          tokens.add(scl.next());
        }
        node.put(tokens.get(0), new OddNode(Integer.parseInt(tokens.get(1)), new double[]{}, tokens.size() - 2, OddNode.NodeType.NORMAL));
        map.put(tokens.get(0), tokens);

        for (int i = offset; i < tokens.size(); i++) {
          if (tokens.get(i).charAt(0) == 'S') {
            node.put(tokens.get(i), new OddNode(-1, new double[]{}, 0, OddNode.NodeType.SINK));
            node.get(tokens.get(i)).setSinkNum( Integer.parseInt(tokens.get(i).substring(1)) );
          }
        }
      }

      for (Map.Entry<String, ArrayList<String> > entry : map.entrySet()) {
        ArrayList<String> tokens = entry.getValue();
        for (int i = offset; i < tokens.size(); i++) {
          node.get(tokens.get(0)).setChild(i-offset, node.get(tokens.get(i)));
        }
      }

      root = node.get("0");      

    } catch (FileNotFoundException e) {
      System.out.println(e);
    }

    return root;
  }

  void run(String config_file) {
    CompileClassifierConfig config = loadConfig(config_file);
    String odd_file = config.getOutput_filepath() + config.getName() + "_" + config.getId() + ".odd";

    BayesianNetworkClassifier bnc = loadBNC(config);
    OddNode odd = loadODD(odd_file);
    String[] var_order = loadVarOrder(odd_file);
    test(bnc, odd, var_order);
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Must have 1 argument: config_file");
      return;
    }
    TestOdd T = new TestOdd();

    String config_file = args[0];

    T.run(config_file);
  }
}
