/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package org.jboss.virtual.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.jboss.virtual.plugins.context.file.FileSystemContextFactory;
import org.jboss.virtual.plugins.context.jar.JarContextFactory;
import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;
import org.jboss.virtual.plugins.context.VfsArchiveBrowserFactory;
import org.jboss.util.file.ArchiveBrowser;

/**
 * A singleton factory for locating VFSContextFactory instances given VFS root URIs.
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 45764 $
 */
public class VFSContextFactoryLocator
{
   /** The log */
   private static final Logger log = Logger.getLogger(VFSContextFactoryLocator.class); 
   
   /** The VSFactory mapped keyed by the VFS protocol string */
   private static final Map<String, VFSContextFactory> factoryByProtocol = new ConcurrentHashMap<String, VFSContextFactory>();
   
   /** The system property that defines the default file factory */
   public static final String DEFAULT_FACTORY_PROPERTY = VFSContextFactory.class.getName();
   
   /** The path used to load services from the classpath */
   public static final String SERVICES_PATH = "META-INF/services/" + VFSContextFactory.class.getName();
   
   /** Has the default initialzation been performed */
   private static boolean initialized;

   static
   {
      String pkgs = System.getProperty("java.protocol.handler.pkgs");
      if (pkgs == null || pkgs.trim().length() == 0)
      {
         pkgs = "org.jboss.virtual.protocol";
         System.setProperty("java.protocol.handler.pkgs", pkgs);
      }
      else if (!pkgs.contains("org.jboss.virtual.protocol"))
      {
         pkgs += "|org.jboss.virtual.protocol";
         System.setProperty("java.protocol.handler.pkgs", pkgs);
      }
      // keep this until AOP and HEM uses VFS internally instead of the stupid ArchiveBrowser crap.
      ArchiveBrowser.factoryFinder.put("vfsfile", new VfsArchiveBrowserFactory());
   }


   /**
    * Register a new VFSContextFactory
    * 
    * @param factory the factory
    * @throws IllegalArgumentException if the factory is null or the factory
    *         returns a null or no protocols
    * @throws IllegalStateException if one of the protocols is already registered
    */
   public synchronized static void registerFactory(VFSContextFactory factory)
   {
      if (factory == null)
         throw new IllegalArgumentException("Null VFSContextFactory");

      String[] protocols = factory.getProtocols();
      if (protocols == null || protocols.length == 0)
         throw new IllegalArgumentException("VFSContextFactory trying to register null or no protocols: " + factory);
      
      for (String protocol : protocols)
      {
         if (protocol == null)
            throw new IllegalArgumentException("VFSContextFactory try to register a null protocol: " + factory + " protocols=" + Arrays.asList(protocols));
         VFSContextFactory other = factoryByProtocol.get(protocol);
         if (other != null)
            throw new IllegalStateException("VFSContextFactory: " + other + " already registered for protocol: " + protocol);
      }

      boolean trace = log.isTraceEnabled();
      for (String protocol : protocols)
      {
         factoryByProtocol.put(protocol, factory);
         if (trace)
            log.trace("Registered " + factory + " for protocol: " + protocol);
      }
   }

   /**
    * Unregister a VFSContextFactory
    * 
    * @param factory the factory
    * @return false when not registered
    * @throws IllegalArgumentException if the factory is null
    */
   public synchronized static boolean unregisterFactory(VFSContextFactory factory)
   {
      if (factory == null)
         throw new IllegalArgumentException("Null VFSContextFactory");

      ArrayList<String> protocols = new ArrayList<String>();
      for (Map.Entry<String, VFSContextFactory> entry : factoryByProtocol.entrySet())
      {
         if (factory == entry.getValue())
            protocols.add(entry.getKey());
      }

      boolean trace = log.isTraceEnabled();
      for (String protocol : protocols)
      {
         factoryByProtocol.remove(protocol);
         if (trace)
            log.trace("Unregistered " + factory + " for protocol: " + protocol);
      }
      
      return protocols.isEmpty() == false;
   }

   /**
    * Return the VFSContextFactory for the VFS mount point specified by the rootURL.
    *  
    * @param rootURL - the URL to a VFS root
    * @return the VFSContextFactory capable of handling the rootURL. This will be null
    * if there is no factory registered for the rootURL protocol.
    * @throws IllegalArgumentException if the rootURL is null
    */
   public static VFSContextFactory getFactory(URL rootURL)
   {
      if (rootURL == null)
         throw new IllegalArgumentException("Null rootURL");
      
      init();
      String protocol = rootURL.getProtocol();
      return factoryByProtocol.get(protocol);
   }
   /**
    * Return the VFSContextFactory for the VFS mount point specified by the rootURI.
    *  
    * @param rootURI - the URI to a VFS root
    * @return the VFSContextFactory capable of handling the rootURI. This will be null
    * if there is no factory registered for the rootURI scheme.
    * @throws IllegalArgumentException if the rootURI is null
    */
   public static VFSContextFactory getFactory(URI rootURI)
   {
      if (rootURI == null)
         throw new IllegalArgumentException("Null rootURI");
      
      init();
      String scheme = rootURI.getScheme();
      return factoryByProtocol.get(scheme);
   }

   /**
    * Initialises the default VFSContextFactorys<p>
    * 
    * <ol>
    * <li>Look for META-INF/services/org.jboss.virtual.spi.VFSContextFactory
    * <li>Look at the system property org.jboss.virtual.spi.VFSContextFactory for a comma
    *     seperated list of factories
    * <li>Register default loaders when not done by the above mechanisms.
    * </ol>
    */
   private static synchronized void init()
   {
      // Somebody beat us to it?
      if (initialized)
         return;
      
      // Try to locate from services files
      ClassLoader loader = AccessController.doPrivileged(new GetContextClassLoader());
      Enumeration<URL> urls = AccessController.doPrivileged(new EnumerateServices());
      if (urls != null)
      {
         while (urls.hasMoreElements())
         {
            URL url = urls.nextElement();
            
            VFSContextFactory[] factories = loadFactories(url, loader);
            for (VFSContextFactory factory : factories)
            {
               try
               {
                  registerFactory(factory);
               }
               catch (Exception e)
               {
                  log.warn("Error registering factory from " + url, e);
               }
            }
         }
      }

      String defaultFactoryNames = AccessController.doPrivileged(new GetDefaultFactories());
      if (defaultFactoryNames != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(defaultFactoryNames, ",");
         while (tokenizer.hasMoreTokens())
         {
            String factoryName = tokenizer.nextToken();
            VFSContextFactory factory = createVFSContextFactory(loader, factoryName, " from system property.");
            if (factory != null)
               registerFactory(factory);
         }
      }

      // No file protocol, use the default 
      if (factoryByProtocol.containsKey("file") == false)
         registerFactory(new FileSystemContextFactory());

      // No jar protocol, use the default 
      if (factoryByProtocol.containsKey("jar") == false)
         registerFactory(new JarContextFactory());
      
      if (factoryByProtocol.containsKey("vfsmemory") == false)
         registerFactory(MemoryContextFactory.getInstance());

      initialized = true;
   }
   
   /**
    * Load the VFSFactory classes found in the service file
    * 
    * @param serviceURL the service url
    * @param loader the class loader
    * @return A possibly zero length array of the VFSFactory instances
    *    loaded from the serviceURL
    */
   private static VFSContextFactory[] loadFactories(URL serviceURL, ClassLoader loader)
   {
      ArrayList<VFSContextFactory> temp = new ArrayList<VFSContextFactory>();
      try
      {
         InputStream is = serviceURL.openStream();
         try
         {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ( (line = br.readLine()) != null )
            {
               if (line.startsWith("#") == true)
                  continue;
               String[] classes = line.split("\\s+|#.*");
               for (int n = 0; n < classes.length; n ++)
               {
                  String name = classes[n];
                  if (name.length() == 0)
                     continue;
                  VFSContextFactory factory = createVFSContextFactory(loader, name, " defined in " + serviceURL);
                  if (factory != null)
                     temp.add(factory);
               }
            }
         }
         finally
         {
            is.close();
         }
      }
      catch(Exception e)
      {
         log.warn("Error parsing " + serviceURL, e);
      }

      VFSContextFactory[] factories = new VFSContextFactory[temp.size()];
      return temp.toArray(factories);
   }
   
   /**
    * Create a vfs context factory
    * 
    * @param cl the classloader
    * @param className the class name
    * @return the vfs context factory
    */
   private static VFSContextFactory createVFSContextFactory(ClassLoader cl, String className, String context)
   {
      try
      {
         Class factoryClass = cl.loadClass(className);
         return (VFSContextFactory) factoryClass.newInstance();
      }
      catch (Exception e)
      {
         log.warn("Error creating VFSContextFactory " + className + " " + context, e);
         return null;
      }
   }
   
   /**
    * Get the context classloader
    */
   private static class GetContextClassLoader implements PrivilegedAction<ClassLoader>
   {
      public ClassLoader run()
      {
         return Thread.currentThread().getContextClassLoader();
      }
   }
   
   /**
    * Get the default file factory class name
    */
   private static class GetDefaultFactories implements PrivilegedAction<String>
   {
      public String run()
      {
         return System.getProperty(DEFAULT_FACTORY_PROPERTY);
      }
   }
   
   /**
    * Enumerates the services
    */
   private static class EnumerateServices implements PrivilegedAction<Enumeration<URL>>
   {
      public Enumeration<URL> run()
      {
         ClassLoader cl = Thread.currentThread().getContextClassLoader();
         try
         {
            return cl.getResources(SERVICES_PATH);
         }
         catch (IOException e)
         {
            log.warn("Error retrieving " + SERVICES_PATH, e);
            return null;
         }
      }
   }
}
