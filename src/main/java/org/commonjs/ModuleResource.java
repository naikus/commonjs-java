package org.commonjs;

/**
 * An object that holds module's resource information such as its id, URI and content.
 * 
 * @author aniket
 */
public class ModuleResource {
  public final String id;
  public final String uri;
  public final String content;

  public ModuleResource(String id, String uri, String content) {
    this.id = id;
    this.uri = uri;
    this.content = content;
  }
  
  @Override
  public String toString() {
    return id + String.format(":[%s]", uri);
  }
}
