package org.commonjs;

/**
 * ModuleResourceLookup defines a single method to find module resource. 
 * This interface is used by require.js script to lookup module resources.
 * 
 * @author aniket
 */
public interface ModuleResourceLookup {
  /**
   * Find a module resource.
   * @param id The resolved id of the module
   * @return The module resource containing the contents, uri from which the resource was loaded
   * and its actual id.
   */
  public ModuleResource findModuleResource(String id);
}
