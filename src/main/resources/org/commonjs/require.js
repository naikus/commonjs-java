/**
 * A simplistic (although not exact) implementation of a commonjs module specification
 */

(function(global) {
  // Utility Functions -----------------------------------------------------------------------------
  function startsWith(str, search) {
    return str.indexOf(search) === 0;
  }
  
  function endsWith(str, search) {
    return str.lastIndexOf(search) + search.length === str.length;
  }
  
  
  
  // Require related functionality -----------------------------------------------------------------
  var registry = {}, // Stores all the loaded modules keyed with their id
      Lookup = global.Lookup, // ModuleResource finder
      globalModules = global.modules || {};
      
      
  // Disallow other scripts to access the ModuleResource finder and global modules directly
  delete global.Lookup;
  delete global.modules;

  var Require = {
    /**
     * Creates a commonjs module object into a registry and return it. This is done to avoid any
     * circular dependencies.
     * @param {String} id The id of the module
     * @param {String} uri The URI of the module
     * @returns {Object} The module object
     */
    createModule: function(id, uri) {
      // create a module and add to registry, for circular deps
      var module = {
        id: id,
        uri: uri,
        exports: {}
      };
      registry[id] = module;
      return module;
    },
    
    /**
     * Register a module with require. This is useful for internal objects that need to be exposed
     * as modules in require system. e.g. Logger
     * @param {type} module The module object
     */
    registerModule: function(module) {
      registry[module.id] = module;
      return module;
    },
    
    /**
     * Gets a module from the registry. 
     * @param {String} id The id of the module
     * @returns {Object} The module object or null if not present in registry
     */
    getModule: function(id) {
      var mod = registry[id];
      if(!mod && (mod = globalModules[id])) {
        // check in global modules
        mod = Require.registerModule({
          id: id,
          uri: id,
          exports: mod
        });
      }
      return mod;
    },
    
    /**
     * Removes the module from the registry whose id matches the specified id.
     * @param {String} id
     * @returns {undefined}
     */
    removeModule: function(id) {
      delete registry[id];
    },
    
    /**
     * Resolves a module's id specified by id with respect to a base id specified by baseId.
     * Some examples are:
     * 
     * baseId                           | id                   | returned resolved id
     * ---------------------------------|----------------------|------------------------
     * /usr/share/                      | ./share.js           | /usr/share/share.js
     * /usr/share/bin/                  | ./program.js         | /usr/share/bin/program.js
     * /usr/share/themes/               | ../themes.js         | /usr/share/themes.js
     * /usr/share/themes                | ../themes.js         | /usr/themes.js
     * /usr/share/themes/ambience.theme | ./ambience.js        | /usr/share/themes/ambience.js
     * /usr/share/themes/ambience.theme | ../ambience.js       | /usr/share/ambience.js
     * /usr/themes/ambience/metacity/   | ../../he/llo/../t.js | /usr/themes/t.js
     * 
     * @param {String} id The id to resolve
     * @param {String} baseId The id to resolve against (base)
     * @returns {String} The resolved id
     */
    resolve: function(id, baseId) {
      // log("Resolving {} -> {}", id, baseId);
      if(startsWith(id, "./") || startsWith(id, "../")) {
        var baseFrags = baseId.split("/"), 
            pathFrags = id.split("/"), 
            baseIndex = baseFrags.length - 1,
            pathItem;

        for(var i = 0, len = pathFrags.length; i < len; i++) {
          pathItem = pathFrags[i];
          if(pathItem === ".") {
            baseFrags.splice(baseIndex, 1);
            baseIndex = --baseIndex < 0 ? 0 : baseIndex;
            
          }else if(pathItem === "..") {
            baseIndex -= 1;
            baseFrags.splice(baseIndex, 2);
          }else {
            if(! baseFrags[baseIndex]) { // when its an empty string
              baseFrags[baseIndex] = pathItem;
            }else {
              baseIndex = baseFrags.push(pathItem) - 1;
            }
          }
        }
        // log("{}", baseFrags.join("/"));
        return baseFrags.join("/");        
      }else {
        // top level module
        return id;
      }
    },
    
    /**
     * Creates a require funtion for the baseUri. All the modules inside this require function
     * will be resolved against this base URI
     * @param {String} baseUri The base URI for resolving modules
     * @returns {Function} The require function for the specified base URI
     */
    make: function(baseUri) {
      return function(id) {
        // check if already loaded
        var module = Require.getModule(id);
        if(module) {
          return module.exports;
        }
        
        // Create path
        var resolvedId = Require.resolve(id, baseUri);
        // log("CanonicalId {}", canonicalId);

        // lookup/find module resource
        var moduleResource = Lookup.findModuleResource(resolvedId);
        if(!moduleResource) {
          throw new Error("Module not found " + id);
        }
        
        var module = Require.createModule(id, moduleResource.uri);
        var moduleFunc = new Function("require", "module", "exports", moduleResource.content);
        try {
          // User moduleResorce.id here instead of id since moduleResource.id contains
          // the real id of the module. e.g given app/main.js, require('app') will look for
          // app/main.js so moduleResource.id will be 'app/main'
          var moduleRequire = Require.make(moduleResource.id);
          moduleRequire.main = module;
          // moduleFunc(moduleRequire, module, module.exports);
          moduleFunc.call(global, moduleRequire, module, module.exports);
          return module.exports;
        }catch(err) {
          Require.removeModule(id);
          throw err;
        }
      };
    }
  };
  

  global.require = Require.make("");
  
})(this);