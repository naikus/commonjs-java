package org.commonjs;

import java.util.HashMap;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Simple test for require js runner
 * @author naikus
 */
public class RequireJsRunnerTest {
  
  @Test
  public void testJavaTypeModules() throws Exception {
    RequireJsRunner runner = new RequireJsRunner
        (getClass().getClassLoader().getResource("modules"));
    
    runner.registerModuleType(RequireJsRunnerTest.class.getName());
    Object ret = runner.execute("test-javatypes", new HashMap<>());
    Assert.assertTrue(ret instanceof RequireJsRunnerTest);
  }
  
  @Test
  public void testObjectModules() throws Exception {
    RequireJsRunner runner = new RequireJsRunner
        (getClass().getClassLoader().getResource("modules"));
    
    runner.registerModule("runnertestclass", RequireJsRunnerTest.class);
    Object ret = runner.execute("test-objects", new HashMap<>());
    Assert.assertEquals(RequireJsRunnerTest.class.getName(), ret);
  }
}
