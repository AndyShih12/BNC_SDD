import bnccompiler.*;

/** Import standard Java classes. */
import java.util.*;
import java.lang.*;
import java.io.*;

import java.nio.charset.Charset;
import java.nio.file.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RunCompiler 
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

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Must have 1 argument: config_file");
      return;
    }
    RunCompiler T = new RunCompiler();
   
    String config_file = args[0];

    CompileClassifierConfig config = loadConfig(config_file);
    CompileClassifier compile_job = new CompileClassifier(config);
    compile_job.run(true);
  }
}

