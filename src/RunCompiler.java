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

  private static CompileClassifierConfig loadConfig(String config_file, String config_id) {
    CompileClassifierConfig config = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      String jsonString = readFile(config_file, Charset.forName("UTF-8"));
      CompileClassifierConfig[] configArr = mapper.readValue(jsonString, CompileClassifierConfig[].class);
      for (CompileClassifierConfig c : configArr) {
        if (c.getId().equals(config_id)) {
          config = c;
          break;
        }
      }
      if (config == null) {
        throw new Exception(String.format("config_id is not found: %s", config_id));
      }
    } catch (IOException e) {
      System.out.println(e);
    } catch (Exception e) {
      System.out.println(e);
    }
    return config;
  }

  public static void main(String[] args) {
    if (args.length != 3) {
      System.out.println("Must have 3 arguments: config_file, config_id, output_filename");
      return;
    }
    RunCompiler T = new RunCompiler();
   
    String config_file = args[0];
    String config_id = args[1];
    String output_filename = args[2];

    CompileClassifierConfig config = loadConfig(config_file, config_id);
    CompileClassifier compile_job = new CompileClassifier(config, output_filename);
    compile_job.run(true);
  }
}

