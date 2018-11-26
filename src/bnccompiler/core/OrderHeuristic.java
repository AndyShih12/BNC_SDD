package bnccompiler.core;

/** Import statements necessary for il2 classes. */
import il2.bridge.*;
import il2.model.*;
import il2.model.Table;
import il2.util.*;

import java.util.*;

class OrderHeuristic {
  //graph stores edges from parents to children
  //r_graph stores edges from children to parents
  ArrayList<HashSet<Integer>> graph, r_graph;
  HashSet<Integer> features;
  HashSet<Integer> subroots;
  int root;
  int max_index;
  Domain domain;

  class RootFeatureGroup {
    int new_root;
    int[] delete_feature_group;
    RootFeatureGroup(int new_root, int[] delete_feature_group) {
      this.new_root = new_root;
      this.delete_feature_group = delete_feature_group;
    }
  }
  class GraphOrder {
    String[] class_order;
    String[] feature_order;
    String[][] block_order;
    String[][] node_order;
    GraphOrder(String[] class_order,String[] feature_order, String[][] block_order, String[][] node_order) {
      this.class_order = class_order;
      this.feature_order = feature_order;
      this.block_order = block_order;
      this.node_order = node_order;
    }
  }

  int[] hashSetToArray(HashSet<Integer> set) {
    int[] arr = new int[set.size()];
    int m = 0;
    for (int i : set) {
      arr[m++] = i;
    }
    return arr;
  }

  HashSet<Integer> recursivelyGetPrunedLeaves(HashSet<Integer> good_set) {
    HashSet<Integer> pruned_leaves = new HashSet<Integer>();

    //start working set as features, subroots, and root
    HashSet<Integer> working_set = new HashSet<Integer>(features);
    working_set.addAll(subroots);
    working_set.add(root);

    int[] degree_cnt = new int[this.max_index];
    Queue<Integer> leaf_queue = new LinkedList<Integer>();
    for (int node: working_set) {
      degree_cnt[node] = graph.get(node).size();
      if (degree_cnt[node] == 0) { // node is leaf
        leaf_queue.add(node);
      }
    }
    while (!leaf_queue.isEmpty()) {
      int leaf = leaf_queue.poll();

      //don't erase leaves in good_set
      if (good_set.contains(leaf)) {
        continue;
      }

      pruned_leaves.add(leaf);
      for (int parent: r_graph.get(leaf)) {
        degree_cnt[parent]--;
        if (degree_cnt[parent] == 0) {
          leaf_queue.add(parent);
        }
      }
    }
    return pruned_leaves;
  }

  void dfs(int cur_node, HashSet<Integer> z_set, HashSet<Integer> seen) {
    if (seen.contains(cur_node)) {
      return;
    }
    seen.add(cur_node);

    //dfs on parents if parent is not in z
    for (int parent: r_graph.get(cur_node)) {
      //System.out.println(this.domain.name(cur_node) + " par: " + this.domain.name(parent));
      if (!z_set.contains(parent)) {
        dfs(parent, z_set, seen);
      }
    }

    //dfs on children if cur_node is not in z
    if (!z_set.contains(cur_node)) {
      for (int child: graph.get(cur_node)) {
        dfs(child, z_set, seen);
      }
    }
  }

  boolean dsep(int[] x, int[] z, int[] y) {
    HashSet<Integer> xyz_set = new HashSet<Integer>();
    for (int cur_x: x) { xyz_set.add(cur_x); }
    for (int cur_y: y) { xyz_set.add(cur_y); }
    for (int cur_z: z) { xyz_set.add(cur_z); }

    // prune leaves not in x u y u z
    HashSet<Integer> seen = recursivelyGetPrunedLeaves(xyz_set);
    
    //remove outgoing edges from z (implemented in dfs function)
    //dsep is true iff x and y are not connected
    HashSet<Integer> z_set = new HashSet<Integer>();
    for (int cur_z: z) { z_set.add(cur_z); }

    for (int cur_x: x) {
      dfs(cur_x, z_set, seen);
    }
    for (int cur_y: y) {
      if (seen.contains(cur_y) && !z_set.contains(cur_y)) {
        return false;
      }
    }
    return true;
  }

  void extractGraph(Table[] cpts) {
    this.max_index = cpts.length;
    this.domain = cpts[0].domain();

    this.graph = new ArrayList<HashSet<Integer>>();
    this.r_graph = new ArrayList<HashSet<Integer>>();

    for (int i = 0; i < this.max_index; i++) {
      this.graph.add(new HashSet<Integer>());
      this.r_graph.add(new HashSet<Integer>());
    }
    for (int i = 0; i < this.max_index; i++) {
      IntSet vars = cpts[i].vars();

      int child = vars.get(vars.size() - 1);
      for (int j = 0; j < vars.size() - 1; j++) {
        this.r_graph.get(child).add(vars.get(j));
        this.graph.get(vars.get(j)).add(child);
      }
    }

    //assuming leaf nodes are features
    this.subroots = new HashSet<Integer>();
    this.features = new HashSet<Integer>();
    for (int i = 0; i < this.max_index; i++) {
      if (this.graph.get(i).size() == 0) {
        this.features.add(i);
      }
      else if (i != this.root) {
        this.subroots.add(i);
      }
    }
  }

  int[] tryCurRoot() {
    int[] feature_arr = hashSetToArray(features);
    int m = feature_arr.length;

    //try current root
    boolean[][] dependent = new boolean[m][];
    for (int i = 0; i < m; i++) {
      dependent[i] = new boolean[m];
      for (int j = 0; j < m; j++) {
        if ( dsep( new int[]{ feature_arr[i] }, new int[]{ root }, new int[]{ feature_arr[j] }) ) {
          dependent[i][j] = false;
        }
        else {
          dependent[i][j] = true;
        }
      }
    }
    //floyd-warshall for easy implementation
    for (int k = 0; k < m; k++) {
      for (int i = 0; i < m; i++) {
        for (int j = 0; j < m; j++) {
          dependent[i][j] |= (dependent[i][k] && dependent[k][j]);
        }
      }
    }
    //find smallest feature cluster
    HashSet<Integer> best_set = null;
    for (int i = 0; i < m; i++) {
      HashSet<Integer> set = new HashSet<Integer>();
      for (int j = 0; j < m; j++) {
        if (dependent[i][j]) {
          set.add(feature_arr[j]);
        }
      }
      if (best_set == null || set.size() < best_set.size() ) {
        best_set = set;
      }
    }
    return hashSetToArray(best_set);
  }

  int[] trySubroot(int subroot) {
    //try subroots
    HashSet<Integer> x_side = new HashSet<Integer>();
    HashSet<Integer> y_side = new HashSet<Integer>();
    for (int feature: features) {
      int[] subroot_parents = hashSetToArray(r_graph.get(subroot));

      boolean dsep1 = dsep( new int[]{ root }, new int[]{ subroot }, new int[]{ feature } );
      boolean dsep2 = dsep( subroot_parents, new int[]{ subroot }, new int[]{ feature } );

      if ( dsep1 && dsep2 ) {
        y_side.add(feature);
      }
      else {
        x_side.add(feature);
      }
    }

    
    for (Iterator<Integer> it = y_side.iterator(); it.hasNext();) {
      int cur_y = it.next();
      boolean add_to_x = false;
      for (int cur_x: x_side) {
        if ( !dsep(new int[]{ cur_x }, new int[]{ subroot }, new int[]{ cur_y }) ) {
          add_to_x = true;
        }
      }
      if (add_to_x) {
        x_side.add(cur_y);
        it.remove();
      }
    }
    
 
    int[] x_noroot = hashSetToArray(x_side);
    int[] y = hashSetToArray(y_side);
    int[] z = new int[]{ subroot };
    
    x_side.add(root);
    int[] x = hashSetToArray(x_side);

    
    //System.out.println("-------------------");
    //System.out.println("subroot: " + this.domain.name(subroot));
    //for (int cur_x: x) {
    //  System.out.println(this.domain.name(cur_x));
    //}
    //System.out.println("::");
    //for (int cur_y: y) {
    //  System.out.println(this.domain.name(cur_y));
    //}
    //System.out.println("dsep: " + (dsep(x,z,y)? "y":"n")); 

    if ( dsep( x, z, y ) ) {
      return x_noroot;
    }
    return null;
  }

  RootFeatureGroup getRootFeatureGroup() {
    int best_root = this.root;
    int[] best_x = tryCurRoot();

    //try subroots
    for (int subroot : subroots) {
      int[] good_x = trySubroot(subroot);
      if (good_x != null && good_x.length < best_x.length) {
        best_x = good_x;
        best_root = subroot;
      }
      /*
      System.out.println(this.domain.name(subroot));
      if (good_x != null) {
        System.out.println(this.domain.name(subroot) + ": ");
        for (int x : good_x) {
          System.out.print(this.domain.name(x) + ", ");
        }
        System.out.println("");
      }
      */
    }

    /*
    System.out.println("new root: " + this.domain.name(best_root));
    for (int cur_x: best_x) {
      System.out.println(this.domain.name(cur_x));
    }
    */
    RootFeatureGroup rf_group = new RootFeatureGroup(best_root, best_x);
    rf_group.new_root = best_root;
    rf_group.delete_feature_group = best_x;
    return rf_group;
  }

  void pruneNetwork(int new_root, int[] features_delete) {
    //mark the nodes that we don't want to prune
    HashSet<Integer> keep_set = new HashSet<Integer>(features);
    keep_set.add(new_root);
    for (int to_delete: features_delete) {
      keep_set.remove(to_delete);
    }

    //start working set as features, subroots, and root
    HashSet<Integer> working_set = new HashSet<Integer>(features);
    working_set.addAll(subroots);
    working_set.add(root);

    //remove incoming edges to new_root
    for (int node: working_set) {
      for (Iterator<Integer> it = this.graph.get(node).iterator(); it.hasNext();) {
        Integer j = it.next();
        //remove edge if it is to new_root
        if (j == new_root) {
          it.remove();
        }
      }
      //remove backward edge if it is to new_root
      if (node == new_root) {
        this.r_graph.get(node).clear();
      }
    }

    //get recursively pruned leaves
    HashSet<Integer> pruned_leaves = recursivelyGetPrunedLeaves(keep_set);

    //remove pruned leaves
    for (int node: working_set) {
      for (Iterator<Integer> it = this.graph.get(node).iterator(); it.hasNext();) {
        Integer j = it.next();
        //remove edge if it is to a pruned leaf
        if (pruned_leaves.contains(j)) {
          it.remove();
        }
      }
      //remove backward edge if it is to a pruned leaf
      if (pruned_leaves.contains(node)) {
        this.r_graph.get(node).clear();
      }
    }

    //remove pruned leaves from feature and subroot set
    for (int node: working_set) {
      if (pruned_leaves.contains(node)) {
        features.remove(node);
        subroots.remove(node);
      }
    }
    this.root = new_root;
    subroots.remove(new_root);
  }

  GraphOrder run(Table[] cpts, String root_str,  String[] my_features) {
    Domain domain = cpts[0].domain();
    this.root = domain.index(root_str);
    //extract graph info from cpt
    extractGraph(cpts);

    //override features and subroots set
    if (my_features != null) {
      this.subroots.addAll(this.features);
      this.features.clear();
      for (String f: my_features) {
        this.subroots.remove(this.domain.index(f));
        this.features.add(this.domain.index(f));
      }
    }

    ArrayList<String> class_order = new ArrayList<String>();
    ArrayList<String> feature_order = new ArrayList<String>();
    ArrayList<ArrayList<String>> block_order = new ArrayList<ArrayList<String>>();
    ArrayList<ArrayList<String>> nodes = new ArrayList<ArrayList<String>>();

    //System.out.println(this.domain.name(this.root));
    //System.out.println(this.root);

    while (features.size() > 0) {
      RootFeatureGroup rf_group = null;
      if (class_order.size() == 0) { //encode base network
        rf_group = new RootFeatureGroup(this.root, new int[0]); //dummy object
      }
      else {
        rf_group = getRootFeatureGroup();
        pruneNetwork(rf_group.new_root, rf_group.delete_feature_group);
      }

      ArrayList<String> cur_nodes = new ArrayList<String>();
      //make sure topological order is kept
      for (int i = 0; i < this.max_index; i++) {
        if (features.contains(i) || subroots.contains(i) || i == this.root) {
          cur_nodes.add(this.domain.name(i));
        }
      }
      nodes.add(cur_nodes);

      ArrayList<String> cur_order = new ArrayList<String>();
      for (int f : rf_group.delete_feature_group) {
        cur_order.add(this.domain.name(f));
        feature_order.add(this.domain.name(f));
      }
      class_order.add(this.domain.name(this.root));
      block_order.add(cur_order);
    }


    String[] class_order_arr = class_order.toArray(new String[class_order.size()]);
    String[] feature_order_arr = feature_order.toArray(new String[feature_order.size()]);
    String[][] block_order_arr = new String[block_order.size()][];
    String[][] nodes_arr = new String[nodes.size()][];
    for (int i = 0; i < block_order.size(); i++) {
      block_order_arr[i] = block_order.get(i).toArray(new String[block_order.get(i).size()]);
    }
    for (int i = 0; i < nodes.size(); i++) {
      nodes_arr[i] = nodes.get(i).toArray(new String[nodes.get(i).size()]);
    }
    
    return new GraphOrder(class_order_arr, feature_order_arr, block_order_arr, nodes_arr);
  }

}
