package bnccompiler;

public class CompileClassifierConfig {
  private String id;
  private String root;
  private String filetype;
  private String name;
  private String vars;
  private String[] leaves;
  private String threshold;
  private String input_filepath;
  private String output_filepath;
  public String getId () {
   return id;
  }
  public void setId (String id) {
    this.id = id;
  }
  public String getRoot () {
    return root;
  }
  public void setRoot (String root) {
    this.root = root;
  }
  public String getFiletype () {
    return filetype;
  }
  public void setFiletype (String filetype) {
    this.filetype = filetype;
  }
  public String getVars () {
    return vars;
  }
  public void setVars (String vars) {
    this.vars = vars;
  }
  public String getName () {
    return name;
  }
  public void setName (String name) {
    this.name = name;
  }
  public String[] getLeaves () {
    return leaves;
  }
  public void setLeaves (String[] leaves) {
    this.leaves = leaves;
  }
  public String getThreshold () {
    return threshold;
  }
  public void setThreshold (String threshold) {
    this.threshold = threshold;
  }
  public String getInput_filepath () {
    return input_filepath;
  }
  public void setInput_filepath (String input_filepath) {
    this.input_filepath = input_filepath;
  }
  public String getOutput_filepath () {
    return output_filepath;
  }
  public void setOutput_filepath (String output_filepath) {
    this.output_filepath = output_filepath;
  }
 
  @Override
  public String toString() {
    return "ClassPojo [id = "+id+", root = "+root+", name = "+name+", vars = "+vars+", leaves = "+leaves+", threshold = "+threshold+", input_filepath = "+input_filepath+", output_filepath = "+output_filepath+"]";
  }
} 
