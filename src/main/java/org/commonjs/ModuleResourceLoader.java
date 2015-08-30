package org.commonjs;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A ModuleResource loading utility that loads module resoruces from specified URLs.
 * 
 * @author aniket
 */
class ModuleResourceLoader {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleResourceLoader.class.getSimpleName());
  private final URL basePath;
  
  ModuleResourceLoader(URL path) {
    this.basePath = path;
  }
  
  ModuleResource findModule(String id) {
    return findModule(id, false);
  }
  
  ModuleResource findModule(String id, boolean reload) {
    // load from path
    return loadModule(id);
  }
  
  ModuleResource loadModule(String id) {
    if("file".equals(basePath.getProtocol().toLowerCase())) {
      return loadFromFileSystem(id);
    }else {
      return loadRemote(id);
    }
  }
  
  
  // Private API -----------------------------------------------------------------------------------
  
  /**
   * Attempts to load a given module from filesystem. This should only be called if the base path
   * for this loader is a file protocol
   * @param id The id of the module to load
   * @return The loaded moduleinfo or null;
   */
  private ModuleResource loadFromFileSystem(String id) {
    File baseDir = new File(basePath.getPath());
    FileChannel channel = null;
    
    String modId = id;
    
    try {
      // try to load as js file
      File moduleFile = new File(baseDir, modId + ".js");
      LOG.info("Trying to load as js file: {}", moduleFile);
      if(moduleFile.exists()) {
        channel = new FileInputStream(moduleFile).getChannel();
      }else {
        // try to load as directory containing main.js
        // LOG.info("Trying to load as dir with main.js");
        moduleFile = new File(baseDir, modId);
        if(moduleFile.isDirectory() && (moduleFile = new File(moduleFile, "main.js")).exists()) {
          modId = id + "/main";
          channel = new FileInputStream(moduleFile).getChannel();
        }
      }

      if(channel != null) {
        byte[] content = getContent(channel);
        
        String js = new String(content, Charset.forName("UTF-8"));
        return new ModuleResource(modId, moduleFile.getPath(), js);
      }
    }catch(IOException ioe) {
      throw new RuntimeException("Error loading module from filesystem", ioe);
    }finally {
      closeSilently(channel);
    }
    return null;
  }
  
  /**
   * Called if the base path for this loader is not a file:// URL
   * @param id The id of the module to load
   * @return The loaded module or null if module not found
   */
  private ModuleResource loadRemote(String id) {
    ReadableByteChannel channel = null;
    URL moduleUrl = null;
    String moduleUri = id + ".js";
    try {
      moduleUrl = new URL(this.basePath, moduleUri);
      LOG.info("Loading module from remote URL: {}", moduleUrl);
      channel = Channels.newChannel(moduleUrl.openStream());
      byte[] content = getContent(channel);
      
      String js = new String(content, Charset.forName("UTF-8"));
      return new ModuleResource(id, moduleUrl.toExternalForm(), js);
    }catch(MalformedURLException ex) {
      throw new RuntimeException("Invalid module id " + id, ex);     
    }catch(IOException ioe) {
      if(ioe instanceof FileNotFoundException) {
        LOG.warn("Module not found: {}", moduleUrl);
        return null;
      }
      throw new RuntimeException("Error loading remote module " + id, ioe);
    }finally {
      closeSilently(channel);
    }
  }
  
  /**
   * Reads contents of a channel into a byte array
   * @param channel The channel to read from
   * @return The bytes read 
   * @throws IOException 
   */
  private static byte[] getContent(ReadableByteChannel channel) throws IOException {
    if(channel instanceof FileChannel) {
      return getFileContent((FileChannel) channel);
    }
    
    ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
    ByteBuffer buff = ByteBuffer.allocate(4096);
    int bytesRead;
    while((bytesRead = channel.read(buff)) != -1) {
      bout.write(buff.array(), 0, buff.position());
      buff.clear();
    }
    return bout.toByteArray();
  }
  
  /**
   * Gets contents of a file from a file channel.
   * @param channel The channel to read from
   * @return The contents of the channel
   * @throws IOException 
   */
  private static byte[] getFileContent(FileChannel channel) throws IOException {
    ByteBuffer buff = ByteBuffer.allocate((int) channel.size());
    channel.read(buff);
    return buff.array();
  }
  
  private static void closeSilently(Closeable c) {
    if(c != null) {
      try {
        c.close();
      }catch(IOException e) {}
    }
  }

}
