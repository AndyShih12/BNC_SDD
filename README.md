# BNC_ODD

Converts a Bayesian Network Classifier (BNC) into a Ordered Decision Diagram (ODD). 
Then, post-processes the ODD into a Sentential Decision Diagram (SDD).
The algorithm is described in the following paper:

```
Compiling Bayesian Networks into Decision Graphs
In Proceedings of the Thirty-Third AAAI Conference on Artificial Intelligence (AAAI), 2019
Andy Shih, Arthur Choi, Adnan Darwiche
```

### Input Format

The input BNC is specified using "config.json". It should have a JSON object with the following fields:

- "id": some number to help you distinguish the compilation task
- "name": name of the .net or the .uai Bayesian Network file
- "filetype": filetype of the Bayesian Network file (either ".net" or ".uai")
- "vars": number of feature variables
- "root": class node of the Bayesian Network Classifier
- "leaves": a list of the feature variables. If empty, defaults to all leaves of the Bayesian Network. If not empty, length should match "vars"
- "threshold": threshold of the Bayesian Network Classifier
- "input_filepath": filepath of the Bayesian Network file
- "output_filepath": filepath to write the output ODD and SDD


The underlying Bayesian Network of the BNC is specified as a .net file or a .uai file.
The file that will be looked up is ```config["input_filepath"] + config["name"] + "." + config["filetype"]```.
For example, if the filepath of the network file is at ```"networks/binarynetworks/admission.net"```, then
we set ```config["input_filepath"]: "networks/binarynetworks/"```, ```config["name"]: "admission"```, and ```config["filetype"]: "net"```.

#### Assumptions

The underlying Bayesian Network should be completely binary (all interal nodes and leaves are binary). Furthermore, there should be no hard CPTs (no 0/1 values in CPTS). 

### Output Format

The output of the program will be 4 files:

- ODD file: The ODD representation of the decision function of the BNC
- SDD file: The SDD representation of the decision function of the BNC
- vtree file: The vtree accompanying the SDD
- variable description file: A description of the variables of the ODD/SDD (includes variable order, comments)

The ODD file will be written at ```config["output_filepath"] + config["name"] + "_" + config["id"] + ".odd"```

The SDD file will be written at ```config["output_filepath"] + config["name"] + "_" + config["id"] + ".sdd"```

The vtree file will be written at ```config["output_filepath"] + config["name"] + "_" + config["id"] + ".vtree"```

The variable description file will be written at ```config["output_filepath"] + config["name"] + "_" + config["id"] + ".txt"```

### Running BNC_ODD

```
./run
```

### Further Questions

Contact us at 
```
Andy Shih: andyshih@cs.ucla.edu
Arthur Choi: aychoi@cs.ucla.edu
Adnan Darwiche: darwiche@cs.ucla.edu
```
