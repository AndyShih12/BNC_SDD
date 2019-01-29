package bnccompiler.core;

/** Import statements necessary for il2 classes. */
import il2.model.*;
import il2.model.Table;
import il2.util.*;

/** Import standard Java classes. */
import java.util.*;
import java.lang.Double;

public class BayesianNetworkClassifierToOdd
{
  int num_features;
  int[] cardinality;
  double threshold;
  double class_prior;
  HashMap<DecisionKey, OddNode> cache;

  boolean all_c_and_h_nodes_are_binary;
  double[][][] instance_points;
  ArrayList<TreeSet<Double>> instance_points_binary;
  int[] h_cardinality;
  int root_cardinality;

  BayesianNetworkClassifier bnc;
  BayesianNetwork[] version_network;
  String[] feature_order;
  String[][] h_order;
  String[][] block_order;
  int[] block_order_sz;
  String[][] node_order;

  OddNode[] sink_nodes;

  static final int root_index = 0;
  static final String root_string = "z__root__";

  static int cache_hits = 0;
  static int calc_counts = 0;
  static long fn_time = 0;
  static int buildodd_count = 0;

  public BayesianNetworkClassifierToOdd(BayesianNetworkClassifier bnc, BayesianNetworkClassifierCompilationOrder compilation_order) {
    this.bnc = bnc;

    this.feature_order = compilation_order.getFeature_order();
    this.h_order = compilation_order.getH_order();
    this.block_order = compilation_order.getBlock_order();
    this.block_order_sz = compilation_order.getBlock_order_sz();
    this.node_order = compilation_order.getNode_order();
  }

  public OddNode compile() {
    //store classifier data locally
    this.num_features = this.feature_order.length;

    Domain d = this.bnc.getBayesianNetwork().domain();
    this.cardinality = new int[this.num_features];
    this.instance_points_binary = new ArrayList<TreeSet<Double>>();
    while(instance_points_binary.size() < this.block_order.length) instance_points_binary.add(null);
    this.instance_points = new double[this.block_order.length][][];
    this.h_cardinality = new int[this.block_order.length];
    this.root_cardinality = d.size( d.index( this.h_order[0][0] ) );

    for (int i = 0; i < this.num_features; i++) {
      this.cardinality[i] = d.size( d.index( this.feature_order[i] ) );
    }

    this.all_c_and_h_nodes_are_binary = true;
    for (int i = 0; i < this.block_order.length; i++) {
      this.h_cardinality[i] = 1;
      for (int j = 0; j < this.h_order[i].length; j++) {
        this.h_cardinality[i] *= d.size( d.index(this.h_order[i][j]) );
        if (d.size( d.index(this.h_order[i][j]) ) > 2) {
          this.all_c_and_h_nodes_are_binary = false;
        }
      }
    }
    //System.out.println("all_binary?: " + all_c_and_h_nodes_are_binary);

    this.threshold = this.bnc.getThreshold();
    this.cache = new HashMap<DecisionKey, OddNode>();
    //System.out.println("Root prior odds: " + this.class_prior_odds);

    this.version_network = new BayesianNetwork[this.block_order.length];

    //outputInitialSummary();
    OddNode root = buildOdd();  
    keepUniqueNodes(root);
    //outputFinalSummary();

    //OddNode root = null;
 
    return root;
  }

  private OddNode buildOdd() {
    //setup ODD sinks on the last layer
    this.sink_nodes = new OddNode[this.root_cardinality];
    for (int i = 0; i < this.root_cardinality; i++) {
      this.sink_nodes[i] = new OddNode(this.num_features, new double[]{}, 0, OddNode.NodeType.SINK);
      this.sink_nodes[i].setSinkNum(i);
    }

    int[] instance = new int[this.num_features];
    //build the ODD, starting with an empty instance
    return buildSubOdd(0, 0, null, null, instance);
  }

  //builds an ODD depth-first
  //level determines the variable currently being processed
  //order_level determines the subclassifier version currently being processed
  private OddNode buildSubOdd(int level, int order_level, double[] prior, double[][] vector, int[] instance) {
    String indent = new String(new char[level]).replace('\0', ' ');
    //System.out.println(indent + "level: " + level + " order_level: " + order_level + " prior_odds: " + prior_odds);
    if (level == this.block_order_sz[order_level+1]) {
      double[] new_prior = getPrior(order_level, prior, vector, instance);
      double[][] new_vector = getVector(order_level, prior, vector, instance);
    
      //System.out.println(order_level + " " + Arrays.toString(prior));
      //System.out.println("new: " + Arrays.toString(new_prior));

      int level_sz = this.num_features - this.block_order_sz[order_level+1];
      if (level_sz > this.num_features / 2) {
        return buildSubOdd(level, order_level+1, new_prior, new_vector, instance);
      }

      if (level_sz == 0) {
        return getSink(order_level+1, new_prior, new_vector);
      }
      //find equivalent node
      DecisionKey key = getKey(order_level+1, new_prior, new_vector);
      //System.out.println("level: " + level + " order_level: " + order_level + " " + Arrays.toString(instance));
      OddNode node = this.cache.get(key);
      if (node == null) {
        node = buildSubOdd(level, order_level+1, new_prior, new_vector, instance);
        this.cache.put(key, node);
      }
      //System.out.println(node.getSinkNum());
      return node;
    }

    OddNode node = new OddNode(level, new double[]{}, this.cardinality[level], OddNode.NodeType.NORMAL);

    for (int i = 0; i < this.cardinality[level]; i++) {
      instance[level] = i;
      
      //since this level does not correspond to a subclassifier, just build in a
      //decision-tree-like fashion
      OddNode child = buildSubOdd(level+1, order_level, prior, vector, instance);
     
      node.setChild(i, child);
    }

    //node = getUniqueNode(level, vector);

    boolean debug = true;
    if (debug && level < 5) {
      outputDebug(level, instance);
    }

    return node; 
  }

  private OddNode getSink(int order_level, double[] prior, double[][] vector) {
    BayesianNetwork network = create(order_level, prior, vector);
    int root_node = network.domain().index( this.root_string );
    il2.inf.JointEngine ie = getInferenceEngine(network);

    Table table = doProbabilityQuery(ie, new IntMap(new int[]{}, new int[]{}), root_node);

    // argmax classification
    //int best = argmax(table.values());
    //int best = argmax(prior);
    //System.out.println(" " + Arrays.toString(prior));
    //System.out.println(" " + Arrays.toString(table.values()) + " " + best);
    //return this.sink_nodes[best];

    // threshold classification
    int threshold_class = 1;
    return (table.values()[threshold_class] >= this.threshold) ? this.sink_nodes[1] : this.sink_nodes[0];
  }

  //finds a node that has an equivalent vector.
  private DecisionKey getKey(int order_level, double[] prior, double[][] vector) {
    int state_space = 1;
    for (int i = this.block_order_sz[ order_level ]; i < this.num_features; i++) {
      state_space *= this.cardinality[i];
    }
    //System.out.println(state_space);

    BayesianNetwork network = create(order_level, prior, vector);
    int root_node = network.domain().index( this.root_string );
    il2.inf.JointEngine ie = getInferenceEngine(network);

    IntMap evidence = null;
    int h_node = network.domain().index( this.h_order[order_level][0] );
    Table tableH = doProbabilityQuery(ie, new IntMap(new int[]{},new int[]{}), h_node); // Pr(H)
    evidence = new IntMap(new int[]{h_node}, new int[]{network.domain().instanceIndex( h_node, "0" )});
    Table tableCH0 = doProbabilityQuery(ie, evidence, root_node); // Pr(C | H = 0)
    evidence = new IntMap(new int[]{h_node}, new int[]{network.domain().instanceIndex( h_node, "1" )});
    Table tableCH1 = doProbabilityQuery(ie, evidence, root_node); // Pr(C | H = 1)

    if (this.instance_points[order_level] == null && this.instance_points_binary.get(order_level) == null) {
      // instance_points[v][h] stores Pr(v|h)/Pr(v)

      int[] instance = new int[this.num_features];
      this.instance_points[order_level] = new double[state_space][];
      this.instance_points_binary.set(order_level, new TreeSet<Double>());
      this.instance_points_binary.get(order_level).add(Double.POSITIVE_INFINITY);
      this.instance_points_binary.get(order_level).add(Double.NEGATIVE_INFINITY);
      for (int i = 0; i < state_space; i++) {
        int i_prime = i;
        for (int j = this.block_order_sz[order_level]; j < this.num_features; j++) {
          instance[j] = i_prime % this.cardinality[j];
          i_prime /= this.cardinality[j];
        }

        evidence = setUpEvidence(network, this.block_order_sz[order_level], this.num_features, instance);
        Table table = doProbabilityQuery(ie, evidence, h_node);

        if (this.all_c_and_h_nodes_are_binary) {
          ie.setEvidence(evidence);
          double prV = ie.prEvidence();

          double prVH0 = (table.values()[0] * prV) / tableH.values()[0]; // Pr(v|H=0)
          double prVH1 = (table.values()[1] * prV) / tableH.values()[1]; // Pr(v|H=1)

          double gamma = prVH0 / prVH1;
          this.instance_points_binary.get(order_level).add(gamma);
        }
        else {
          this.instance_points[order_level][i] = table.values().clone();
          for (int h = 0; h < this.h_cardinality[order_level]; h++) {
            this.instance_points[order_level][i][h] *= (1.0/tableH.values()[h]);
          }
        }
      }
    }

    if (this.all_c_and_h_nodes_are_binary) {
      double prC1H0 = tableCH0.values()[1] * tableH.values()[0]; // Pr(C,H=0)
      double prC1H1 = tableCH1.values()[1] * tableH.values()[1]; // Pr(C,H=1)
      double gamma = -1 * (prC1H1 - this.threshold * tableH.values()[1]) / (prC1H0 - this.threshold * tableH.values()[0]);

      Double eq_interval = null;
      double flip = tableCH0.values()[1] < this.threshold ? 1.0 : -1.0;
      if (tableCH0.values()[1] < this.threshold) {
        eq_interval = this.instance_points_binary.get(order_level).floor(gamma);
      } else {
        eq_interval = this.instance_points_binary.get(order_level).ceiling(gamma);
      }

      // System.out.println(sign + " " + (prC1H1 - this.threshold * tableH.values()[1]) + " " + (prC1H0 - this.threshold * tableH.values()[0]));
      // //System.out.println(gamma + " " + lower + " " + order_level);
      return new DecisionKey(new double[]{ eq_interval, flip, order_level });
    }

    double[] result = new double[state_space];
    for (int i = 0; i < state_space; i++) {
      // argmax classification
      /*
      double[] posterior = new double[this.root_cardinality];
      for (int c = 0; c < this.root_cardinality; c++) {
        for (int h = 0; h < this.h_cardinality[order_level]; h++) {
          if (!Double.isNaN(this.instance_points[order_level][i][h])) {
            posterior[c] += vector[c][h] * prior[c] * this.instance_points[order_level][i][h]; // Pr(h|c) * Pr(c) * (Pr(v|h)/Pr(v))
          }
        }
      }
      int best_class = argmax(posterior);
      */
      // threshold classification
      
      int threshold_class = 1;
      double threshold_class_posterior = 0.0;
      double all_class_posterior = 0.0;
      for (int c = 0; c < 2; c++) {
        double posterior = 0.0;
        for (int h = 0; h < this.h_cardinality[order_level]; h++) {
          posterior += vector[c][h] * prior[c] * this.instance_points[order_level][i][h]; // Pr(h|c) * Pr(c) * Pr(v|h)
        }
        if (c == threshold_class) {
          threshold_class_posterior += posterior;
        }
        all_class_posterior += posterior;
      }
      int best_class = (threshold_class_posterior / all_class_posterior) >= this.threshold ? 1 : 0;
      // end threshold classification
      
      result[i] = (double)(best_class);
    }
    return new DecisionKey(result);
  }

  private double[] getPrior(int order_level, double[] prior, double[][] vector, int[] instance) {
    BayesianNetwork network = create(order_level, prior, vector);
    int root_node = network.domain().index( this.root_string );
    int h_node = network.domain().index( this.h_order[order_level][0] );


    //sets up the evidence u, where u a set of feature variables in the old classifier
    //but not in the new subclassifier.
    IntMap evidence = setUpEvidence(network, this.block_order_sz[order_level], this.block_order_sz[order_level+1], instance);
    il2.inf.JointEngine ie = getInferenceEngine(network);

    //computes pr(c|u) where c is the class node
    Table answerCU = doProbabilityQuery(ie, evidence, root_node);
    double[] new_prior = answerCU.values().clone();

    //Table[] table = network.cpts();
    //for (int i = 0; i < table.length; i++) {
    //  System.out.println(table[i].toString());
    //}
    //System.out.println("instance: " + Arrays.toString(instance) + " start: " + this.block_order_sz[order_level] + " end: " + this.block_order_sz[order_level+1]);
    //System.out.println(Arrays.toString(network.cpts()));
    //System.out.println("old prior: " + Arrays.toString(prior) + " root_node: " + root_node);
    //System.out.println("new prior: " + Arrays.toString(new_prior) + " h_node: " + h_node);
    //System.out.println(Arrays.toString(instance) + " " + Arrays.toString(new_prior));

    return new_prior; 
  }

  private double[][] getVector(int order_level, double[] prior, double[][] vector, int[] instance) {
    BayesianNetwork network = create(order_level, prior, vector);
    int root_node = network.domain().index( this.root_string );
    int h_node = network.domain().index( this.h_order[order_level+1][0] );
    double[][] new_vector = new double[this.root_cardinality][];

    //sets up the evidence u, where u a set of feature variables in the old classifier
    //but not in the new subclassifier.
    IntMap evidence = setUpEvidence(network, this.block_order_sz[order_level], this.block_order_sz[order_level+1], instance);
    il2.inf.JointEngine ie = getInferenceEngine(network);
    
    //computes pr(h|cu) where c is the class node and h is the h-node
    for (int c = 0; c < this.root_cardinality; c++) {
      String value_str = String.valueOf(c);
      evidence.put( root_node, network.domain().instanceIndex(root_node, value_str) );
      
      Table answerHCU = doProbabilityQuery(ie, evidence, h_node);
      new_vector[c] = answerHCU.values().clone(); 
      
      evidence.remove( root_node );
    }

    //System.out.println(Arrays.toString(instance));
    //if (vector != null) {
    //  System.out.println("old vector: ");
    //  for (int i = 0; i < vector.length; i++) {
    //    System.out.println(Arrays.toString(vector[i]));
    //  }
    //}
    //System.out.println("new vector: ");
    //for (int i = 0; i < new_vector.length; i++) {
    //  System.out.println(Arrays.toString(new_vector[i]));
    //}

    return new_vector; 
  }

  //helper function that takes an instance array and sets up the evidence IntMap
  private IntMap setUpEvidence(BayesianNetwork network, int level_start, int level_end, int[] instance) {
    int level_sz = level_end - level_start;
    int[] vars = new int[level_sz];
    int[] subinstance = new int[level_sz];

    for (int i = level_start; i < level_end; i++) {
      vars[i - level_start] = network.domain().index(this.feature_order[i]);
      String value_str = String.valueOf(instance[i]);
      subinstance[i - level_start] = network.domain().instanceIndex(vars[i - level_start], value_str);
    }
    return new IntMap(vars, subinstance);
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
   
  private void outputDebug(int level, int[] instance) {
    String indent = new String(new char[level]).replace('\0', ' ');

    System.out.println(indent + "instance: " + Arrays.toString( Arrays.copyOfRange(instance, 0, level) ));
    //System.out.println(indent + "fn_time: " + fn_time/1000000000 + " level: " + level);
  }

  /*
  private void outputInitialSummary() {
    System.out.println("Threshold " + invodds(this.threshold_odds) + "\t" + "Prior " + invodds(this.class_prior_odds));
    System.out.println("Variables: " + this.num_features);
    System.out.println("Treewidth: " + getInferenceEngine(create(0,0.5,0,0)).getClusterStats().getNormalizedMax());
  }
  private void outputFinalSummary() {
    //System.gc(); 
    //output results info
    //System.out.println("ODD size: " + OddNode.counter);
    //System.out.println("calc_counts: " + calc_counts + " cache_hits: " + cache_hits + " fn_time: " + fn_time + " buildodd_count: " + buildodd_count );

    
    //for (int i = 0; i < this.num_features+1; i++) {
      //System.out.println("cache level: " + i + " size: " + this.cache.get(0).get(i).size());
    //}
    
  }
  */

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

    long start = System.nanoTime();
    Table answer = ie.varConditional( query_var );
    long stop = System.nanoTime();
    fn_time += (stop-start);

    return answer;
  }

  private BayesianNetwork create(int version, double[] prior, double[][] vector) {
    if (this.version_network[version] == null) {
      this.version_network[version] = createVersionNetwork(version, prior, vector);
      return this.version_network[version];
    }

    if (version == 0) {
      return this.version_network[0];
    }

    //System.out.println("modifying version: " + version);
    Table[] tables = this.version_network[version].cpts();
    Domain domain = this.version_network[version].domain();

    for (int i = 0; i < tables.length; i++) {
      IntSet parent_set = tables[i].vars();
      
      if (i == this.root_index) {
        tables[i] = new Table(domain, parent_set, prior);
      }
      else if (i == domain.index( this.h_order[version][0] )) {
        double[] cpt = new double[vector.length * vector[0].length];
        for (int j = 0; j < cpt.length; j++) {
          cpt[j] = vector[ j % vector.length ][ j / vector.length ];
        }
        tables[i] = new Table(domain, parent_set, cpt);
      }
      else {
        double[] cpt = tables[i].values();
        tables[i] = new Table(domain, parent_set, cpt);
      }
    }

    BayesianNetwork bn = new BayesianNetwork( tables );
    //System.out.println("done modifying: " + version);
    //System.out.println(Arrays.toString(bn.cpts()));
    return bn;
  }

  private BayesianNetwork createVersionNetwork(int version, double[] prior, double[][] vector) {
    //System.out.println("creating version: " + version);

    Table[] base_tables = this.bnc.getBayesianNetwork().cpts();
    Domain base_domain = this.bnc.getBayesianNetwork().domain();
    int offset = 1; //one for dummy root node
    int max_index = this.bnc.getBayesianNetwork().size() + offset;
    int[] cards = new int[max_index];
    int[][] parents = new int[max_index][];
    double[][] cpts = new double[max_index][];

    Arrays.sort(this.node_order[version], new Comparator<String>() {
      public int compare(String s1, String s2) {
        return Integer.compare(base_domain.index( s1 ), base_domain.index( s2 ));
      }
    });

    String[] nodes = new String[this.node_order[version].length + offset]; //all_nodes + dummy_root
    System.arraycopy(this.node_order[version], 0, nodes, offset, this.node_order[version].length);

    int h_index = -1;
    String h_string = this.h_order[version][0];
    for (int i = offset; i < nodes.length; i++) {
      if (nodes[i].equals(h_string)) {
        h_index = i;
      }
    }

    if (version == 0) {
      //setup root node
      nodes[this.root_index] = this.root_string;
      cards[this.root_index] = base_domain.size( base_domain.index(this.h_order[0][0]) );
      cpts[this.root_index] = base_tables[ base_domain.index(this.h_order[0][0]) ].values().clone();
      //System.out.println("cpt: " + Arrays.toString(cpts[this.root_index]));

      //setup h node
      nodes[h_index] = h_string;
      cards[h_index] = cards[this.root_index];
      cpts[h_index] = new double[cards[this.root_index] * cards[this.root_index]];
      for (int i = 0; i < cards[this.root_index]*cards[this.root_index]; i++) {
        cpts[h_index][i] = ((i%cards[this.root_index]) == (i/cards[this.root_index])) ? 1.0 : 0.0; //if i is on the diagonal
      }
    }
    else {
      //setup root node
      nodes[this.root_index] = this.root_string;
      cards[this.root_index] = prior.length;
      cpts[this.root_index] = prior.clone();

      //setup h node
      nodes[h_index] = h_string;
      cards[h_index] = this.h_cardinality[version];
      cpts[h_index] = new double[vector.length * vector[0].length];
      for (int i = 0; i < cpts[h_index].length; i++) {
        cpts[h_index][i] = vector[ i % vector.length ][ i / vector.length ];
      }      
    }

    //setup domain
    Domain domain = new Domain(nodes.length);
    for (int i = 0; i < nodes.length; i++) {
      if (i != this.root_index && i != h_index) {
        cards[i] = base_domain.size( base_domain.index(nodes[i]) );
      }
      String[] values = new String[cards[i]];
      for (int j = 0; j < cards[i]; j++) {
        values[j] = String.valueOf(j);
      }
      domain.addDim(nodes[i], values);
    }

    //setup parents and modify cpts if h is a parent
    for (int i = 0; i < nodes.length; i++) {
      if (i != this.root_index && i != h_index) {
        int[] parents_copy = base_tables[ base_domain.index(nodes[i]) ].vars().toArray().clone();
        parents[i] = new int[parents_copy.length];
        for (int j = 0; j < parents[i].length; j++) {
          parents[i][j] = domain.index( base_domain.name(parents_copy[j]) );
        }

        cpts[i] = base_tables[ base_domain.index(nodes[i]) ].values().clone();
      }
    }


    parents[this.root_index] = new int[]{ domain.index(this.root_string) };
    parents[h_index] = new int[]{ domain.index(this.root_string), domain.index(h_string) };


    // Create an array of all the tables.
    Table[] tables = new Table[nodes.length];
    for (int i = 0; i < nodes.length; i++) {
      tables[i] = new Table( domain, new IntSet(parents[i]), cpts[i] );
    }
    
    // System.out.println(domain.namesToString());
    // for (int i = 0; i < nodes.length; i++) {
    //   System.out.println("NODE: " + nodes[i]);
    //   for (int j = 0; j < parents[i].length; j++) {
    //     System.out.print(domain.name( parents[i][j] ));
    //   }
    //   System.out.println("Node index: " + domain.index(nodes[i]));
    //   for (int j = 0; j < parents[i].length; j++) {
    //     System.out.print(parents[i][j] + " ");
    //   }
    //   System.out.println();
    //   System.out.println(Arrays.toString(cpts[i]));
    // }

    BayesianNetwork bn = new BayesianNetwork( tables );
    //System.out.println("done version: " + version);
    return bn;
  }

  private void keepUniqueNodes(OddNode root) {
    HashMap<OddNode, OddNode> unique_node = new HashMap<OddNode,OddNode>();
    HashSet<Long> id_set = new HashSet<Long>();
    keepUniqueNodesHelper(root, unique_node, id_set);
  }

  private void keepUniqueNodesHelper(OddNode root, HashMap<OddNode, OddNode> unique_node, HashSet<Long> id_set) {
    if (root.getType() == OddNode.NodeType.SINK) {
      unique_node.put(root, root);
      return;
    }
    if (id_set.contains(root.getId())) {
      return;
    }
    id_set.add(root.getId());

    for (int i = 0; i < root.getCardinality(); i++) {
      keepUniqueNodesHelper(root.getChild(i), unique_node, id_set);
      root.setChild(i, unique_node.get(root.getChild(i)));
    }

    if (!unique_node.containsKey(root)) {
      unique_node.put(root, root);
    }
  }
}

