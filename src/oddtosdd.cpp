extern "C" {
	#include <stdio.h>
	#include <stdlib.h>
	#include "sddapi.h"
}

#include <iostream>
#include <fstream>
#include <sstream>
#include <cstring>
#include <algorithm>
#include <vector>
#include <map>
#include <set>
#include <random>
#include <unordered_map>

using namespace std;

struct Node {
  Node() {
    memset(ch, 0, sizeof(ch));
    cnt++;
  }
  Node* ch[2];
  static int cnt;
} one, zero;

int Node::cnt;
int cntbad;

bool isOne(Node* n)  { return n == &one;  }
bool isZero(Node* n) { return n == &zero; }

int NUM_ATTRS;
string input_odd_file;
string output_sdd_file;
string output_vtree_file;

Node* read_odd_from_file(string filename) {
  ifstream fin(filename);
  string line;

  map<int, Node*> ms;
  map<int, pair<string, string>> mp;

  if (fin.is_open()) {
    while (getline(fin, line)) {
      stringstream sin(line);
      int cur;
      sin >> cur;
      ms[cur] = new Node();
      
      pair<string,string> p;
      sin >> p.first >> p.second;
      mp[cur] = p;
    }
  }
  for (auto elem: mp) {
    int cur = elem.first;
    string child[2] = {elem.second.first, elem.second.second};
    for (int i = 0; i < 2; i++) {
      if (child[i] == "S1") {
        ms[cur]->ch[i] = &one;
      } else if (child[i] == "S0") {
        ms[cur]->ch[i] = &zero;
      } else {
        ms[cur]->ch[i] = ms[stoi(child[i])];
      }
    }
    //cout << cur << " " << child[0] << " " << child[1] << endl;
  }

  int root_num = 0;
  return ms[root_num];
}

long long model_count_helper(Node* root, map<Node*, long long> &ms, int level) {
  if (ms.find(root) != ms.end()) {
    return ms[root];
  }
  if (isOne(root)) { return (1L << (NUM_ATTRS - level)); }  
  if (isZero(root)) { return 0; }
  ms[root] = model_count_helper(root->ch[0], ms, level+1) + model_count_helper(root->ch[1], ms, level+1);
  return ms[root];
}

long long model_count(Node* root) {
  map<Node*, long long> ms;
  return model_count_helper(root, ms, 0);
}

SddManager* init_sdd(Node* r, Vtree* &vtree) {
  // set up vtree and manager  
  SddLiteral var_count = NUM_ATTRS;

  SddLiteral* var_order = new SddLiteral[NUM_ATTRS];
  for (int i = 0; i < NUM_ATTRS; i++) {
  	var_order[i] = i+1;
  }
  
  vtree = sdd_vtree_new_with_var_order(var_count,var_order,"right");
  return sdd_manager_new(vtree);
}

SddNode* convert_obdd_to_sdd_helper(SddManager* manager, Node* r, map<Node*, SddNode*> &m, int lvl = 1) {
  if (m.find(r) != m.end()) {
    return m[r];
  }

  if (isOne(r) || isZero(r)) {
    bool flip_one_and_zero = false;
    SddNode* alpha = (isOne(r) ^ flip_one_and_zero) ? sdd_manager_true(manager) : sdd_manager_false(manager);
    SddNode* beta;

    for (int i = lvl; i <= NUM_ATTRS; i++) {
      beta = sdd_conjoin(sdd_manager_literal(i, manager), alpha, manager);
      alpha = sdd_disjoin(alpha, beta, manager);
      beta = sdd_conjoin(sdd_negate(sdd_manager_literal(i, manager), manager), alpha, manager);
      alpha = sdd_disjoin(alpha, beta, manager);
    }
    return alpha;
  }

  SddNode* ch0 = convert_obdd_to_sdd_helper(manager, r->ch[0], m, lvl+1);
  SddNode* ch1 = convert_obdd_to_sdd_helper(manager, r->ch[1], m, lvl+1);

  SddNode* alpha = sdd_manager_false(manager);
  SddNode* beta;

  beta = sdd_conjoin(sdd_manager_literal(lvl, manager), ch1, manager);
  alpha = sdd_disjoin(alpha, beta, manager);
  beta = sdd_conjoin(sdd_negate(sdd_manager_literal(lvl, manager),manager), ch0, manager);
  alpha = sdd_disjoin(alpha, beta, manager);
  
  m[r] = alpha;
  //sdd_ref(m[r], manager);
  return m[r];
}

SddNode* convert_obdd_to_sdd(SddManager* manager, Node* r) {
  map<Node*, SddNode*> m;
  return convert_obdd_to_sdd_helper(manager, r, m);
}

void write_sdd_to_file(SddNode* final_sdd, Vtree* vtree, string sdd_filename, string vtree_filename) {
  sdd_save(sdd_filename.c_str(), final_sdd);
  sdd_vtree_save(vtree_filename.c_str(), vtree);
  cout << "wrote to files: " << sdd_filename << ", " << vtree_filename << endl;
  
  //sdd_save_as_dot((sdd_filename + ".dot").c_str(), final_sdd);
  //sdd_vtree_save_as_dot((vtree_filename + ".dot").c_str(), vtree);
}

int main() {
  cin >> input_odd_file >> NUM_ATTRS;
  cin >> output_sdd_file >> output_vtree_file;

  Node* root = read_odd_from_file(input_odd_file);
  cout << model_count(root) << endl;

  Vtree* vtree = nullptr;
  SddManager* manager = init_sdd(root, vtree);
  SddNode* root_sdd = convert_obdd_to_sdd(manager, root);
  write_sdd_to_file(root_sdd,vtree,output_sdd_file,output_vtree_file);

  cout << "done" << endl;

  return 0;
}
