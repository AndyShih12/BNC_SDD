#!/usr/bin/env python

import sys
import sdd
import json

def convert_helper(node, mgr, obdd, dp, depth):
  if node == 'S1':
    return sdd.sdd_manager_true(mgr)
  if node == 'S0':
    return sdd.sdd_manager_false(mgr)
  if node in dp:
    return dp[node]

  var, ch0, ch1 = obdd[node][0] + 1, obdd[node][1], obdd[node][2]

  #print var
  alpha = sdd.sdd_conjoin(convert_helper(ch0, mgr, obdd, dp, depth+1), sdd.sdd_manager_literal(-1*var,mgr), mgr)
  beta = sdd.sdd_conjoin(convert_helper(ch1, mgr, obdd, dp, depth+1), sdd.sdd_manager_literal(var,mgr), mgr)

  dp[node] = sdd.sdd_disjoin(alpha, beta, mgr)
  return dp[node]

def convert_obdd_to_sdd(output_filename, documentation_filename):
  with open(output_filename,'r') as f:
    nodes = f.readlines()[1:]
  with open(documentation_filename,'r') as f:
    num_variables = int(f.readline().split(' ')[1])

  nodes = [x.strip().split(' ') for x in nodes]
  nodes = [[int(x) if x.isdigit() else x for x in node] for node in nodes]  

  node_dict = {}
  for l in nodes:
    node_dict[l[0]] = l[1:]

  #print node_dict

  vtree = sdd.sdd_vtree_new(num_variables,"right")
  mgr = sdd.sdd_manager_new(vtree)
  vtree = sdd.sdd_manager_vtree(mgr)

  root = 0
  return convert_helper(root,mgr,node_dict,{},0), vtree, mgr

def main():
  config_file = sys.argv[1]

  with open(config_file, 'r') as f:
    config = json.load(f)
    basename = str("../../" + config[u"output_filepath"] + config[u"name"] + "_" + config[u"id"])
    output_filename = basename + ".odd"
    documentation_filename = basename + ".txt"

  alpha, vtree, mgr = convert_obdd_to_sdd(output_filename, documentation_filename)
  sdd.sdd_save(basename + ".sdd", alpha)
  sdd.sdd_vtree_save(basename + ".vtree", vtree)


if __name__== "__main__":
  main()
