package org.commonjs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * RequireJsRunner laods and runs a commonjs module. Every JS module is loaded as a function as 
 * follows:
 * <pre>
 *  function(require, module, exports) {
 *    // actual module's code here
 *  }
 * </pre>
 * This makes 'require' function, 'module' and 'exports' objects implicitly available to all
 * the modules
 * @author aniket
 */
public class RequireJsRunner {
  // private static final Logger SCRIPT_LOG = LoggerFactory.getLogger("SCRIPT_LOGGER");
  private final ModuleResourceLoader loader;
  
  private ScriptEngineManager engineManager;
  private ScriptEngine engine;
  private Bindings engineVars;
  private Map<String, Object> modules = new HashMap<>();
  
  public RequireJsRunner(URL basePath) {
    this(basePath, new HashMap<>());
  }
  
  public RequireJsRunner(URL basePath, Map<String, Object> modules) {
    this.loader = new ModuleResourceLoader(basePath);
    
    // this.modules.put("logger", SCRIPT_LOG);
    this.modules.putAll(modules);
    this.initialize();
  }
  
  public void registerModule(String moduleId, Object module) {
    this.modules.put(moduleId, module);
  }
  
  /**
   * Runs the function specified by <tt>functionName</tt> of module specified by <tt>moduleId</tt>.
   * The function is passed one argument: an object specified by <tt>vars</tt>
   * @param moduleId The id of the module to execute e.g. 'mymodule/main' or 'mymodule'
   * @param functionName The name of the function to execute. This function has to be exported by
   * the module specified by <tt>moduleId</tt>
   * @param vars Single argument passed to the module function as object
   * @return The result of the function execution
   * @throws ScriptException If the underlying scripting system throws an error
   */
  public Object execute(String moduleId, String functionName, Map<String, Object> vars) 
      throws ScriptException {    
    ScriptContext executionCtx = new SimpleScriptContext();
    Bindings scriptVars = new SimpleBindings();
    scriptVars.putAll(engineVars);
    scriptVars.put("runContext", vars);
    executionCtx.setBindings(scriptVars, ScriptContext.ENGINE_SCOPE);
    
    String statement = new StringBuilder()
        .append("require('").append(moduleId).append("').")
        .append(functionName).append("(runContext);")
        .toString();
    
    return engine.eval(statement, executionCtx);
  }
  
  /**
   * Runs the 'main' function of module specified by <tt>moduleId</tt>.
   * <pre>
   *  // inside a module...
   *  exports.main = function(options) {
   *    // this function is called by execute
   *  }
   * </pre>
   * The 'main' function is passed one argument: an object specified by <tt>vars</tt>
   * @param moduleId The id of the module to execute e.g. 'mymodule/main' or 'mymodule'
   * @param vars Single argument passed to the module function as object
   * @return The result of the function execution
   * @throws ScriptException If the underlying scripting system throws an error
   */
  public Object execute(String moduleId, Map<String, Object> vars) throws ScriptException {
    return execute(moduleId, "main", vars);
  }
  
  /**
   * Initializes the requirejs system and the scripting sandbox
   */
  private void initialize() {
    // Initialize the scripting engine
    engineManager = new ScriptEngineManager();
    engine = engineManager.getEngineByName("ECMAScript");
    if(engine == null) {
      throw new RuntimeException("ECMAScript scripting engine not found");
    }
    
    // Setup context for this engine
    ScriptContext ctx = engine.getContext();
    engineVars = ctx.getBindings(ScriptContext.ENGINE_SCOPE);

    // Restrict scripts to only use javascript and require (sandbox)
    engineVars.remove("Java");
    engineVars.remove("load");
    
    // Add global modules
    engineVars.put("modules", modules);
    
    // Add a script module info finder
    engineVars.put("Lookup", (ModuleResourceLookup) (modId) -> {
      return loader.findModule(modId);
    });
    
    loadRequireJs();
  }
  
  private void loadRequireJs() {
    InputStream in = RequireJsRunner.class.getResourceAsStream("require.js");
    if(in == null) {
      throw new RuntimeException("require.js not found in Engine's classpath");
    }
    try {
      Reader jsReader = new InputStreamReader(in);
      if(engine instanceof Compilable) {
        CompiledScript script = ((Compilable) engine).compile(jsReader);
        script.eval();
      }else {
        engine.eval(jsReader);
      }
    }catch(ScriptException se) {
      throw new RuntimeException("Error loading require.js", se);
    }
  }

}
