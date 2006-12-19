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
package org.jboss.virtual.classloading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.jboss.classloading.spi.ClassLoadingDomain;
import org.jboss.classloading.spi.DomainClassLoader;
import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/** A class loader that obtains classes and resources from a VFS.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 56428 $
 */
public class VFSClassLoader extends SecureClassLoader
   implements DomainClassLoader
{
   private static Logger log = Logger.getLogger(VFSClassLoader.class);

   protected static class ClassPathVFS
   {
      private ArrayList<String> searchCtxs = new ArrayList<String>();
      private VFS vfs;
      protected ClassPathVFS(String[] searchCtxs, VFS vfs)
      {
         this.searchCtxs.addAll(Arrays.asList(searchCtxs));
         this.vfs = vfs;
      }
   }
   protected ArrayList<ClassPathVFS> classpath = new ArrayList<ClassPathVFS>();

   /**
    * Create a class loader given a search path VFS, and default parent class
    * loader.
    * @param searchCtxs - the paths from the VFS that make up the class loader path
    * @param vfs - the VFS used to resolve and load classes and resources
    */
   public VFSClassLoader(String[] searchCtxs, VFS vfs)
   {
      String[] resolvedCtxs = searchCtxs;
      try
      {
         resolvedCtxs = resolveSearchCtxs(searchCtxs, vfs);
      }
      catch(IOException e)
      {
         log.warn("Failed to resolve searchCtxs", e);
      }
      ClassPathVFS cp  = new ClassPathVFS(resolvedCtxs, vfs);         
      classpath.add(cp);
   }
   /**
    * Create a class loader given a search path VFS, and given parent class
    * loader.
    * @param searchCtxs - the paths from the VFS that make up the class loader path
    * @param vfs - the VFS used to resolve and load classes and resources
    * @param parent - the parent class loader to use
    */
   public VFSClassLoader(String[] searchCtxs, VFS vfs, ClassLoader parent)
   {
      super(parent);
      String[] resolvedCtxs = searchCtxs;
      try
      {
         resolvedCtxs = resolveSearchCtxs(searchCtxs, vfs);
      }
      catch(IOException e)
      {
         log.warn("Failed to resolve searchCtxs", e);
      }
      ClassPathVFS cp  = new ClassPathVFS(resolvedCtxs, vfs);         
      classpath.add(cp);
   }

   
   /* (non-Javadoc)
    * @see org.jboss.classloading.spi.DomainClassLoader#getClasspath()
    */
   public URL[] getClasspath()
   {
      ArrayList<URL> cp = new ArrayList<URL>(classpath.size());
      for(ClassPathVFS entry : classpath)
      {
         try
         {
            URL baseURL = entry.vfs.getRoot().toURL();
            for(String path : entry.searchCtxs)
            {
               try
               {
                  URL entryURL = new URL(baseURL, path);
                  cp.add(entryURL);
               }
               catch(MalformedURLException e)
               {               
                  log.debug("Failed to parse path: "+path, e);
               }
            }
         }
         catch(Exception e)
         {
            log.debug("Failed to parse entry: "+entry, e);
         }
      }
      URL[] theClasspath = new URL[cp.size()];
      cp.toArray(theClasspath);
      return theClasspath;
   }

   /**
    * TODO getPackageNames
    * 
    * @see org.jboss.classloading.spi.DomainClassLoader#getPackageNames()
    */
   public String[] getPackageNames()
   {
      return null;
   }

   /**
    * Find and define the given java class
    * 
    * @param name - the binary class name
    * @return the defined Class object
    * @throws ClassNotFoundException thrown if the class could not be found
    *    or defined
    */
   protected Class<?> findClass(String name) throws ClassNotFoundException
   {
      String resName = name.replace('.', '/');
      VirtualFile classFile = findResourceFile(resName+".class");
      if( classFile == null )
         throw new ClassNotFoundException(name);
      try
      {
         byte[] tmp = new byte[128];
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         InputStream is = classFile.openStream();
         int length;
         while ((length = is.read(tmp)) > 0)
         {
            baos.write(tmp, 0, length);
         }
         is.close();
         tmp = baos.toByteArray();
         ProtectionDomain pd = getProtectionDomain(classFile);
         return super.defineClass(name, tmp, 0, tmp.length, pd);
      }
      catch (Exception e)
      {
         throw new ClassNotFoundException(name, e);
      }
   }

   /**
    * Search for the resource in the VFS contraining the search to the
    * class loader paths.
    * @param name - the resource name
    * @return the resource URL if found, null otherwise
    */
   public URL findResource(String name)
   {
      URL res = null;
      VirtualFile vf = findResourceFile(name);
      if( vf != null )
      {
         try
         {
            res = vf.toURL();
         }
         catch(Exception e)
         {
            if( log.isTraceEnabled() )
               log.trace("Failed to obtain vf URL: "+vf, e);
         }
      }
      return res;
   }

   /**
    * Search for the resource in the VFS contraining the search to the
    * class loader paths.
    * @param name - the resource name
    * @return A possibly empty enumeration of the matching resources
    */
   public Enumeration<URL> findResources(String name) throws IOException
   {
      Vector<URL> resources = new Vector<URL>();
      /*for(ClassPathVFS cp : classpath)
      {
         List<VirtualFile> matches = null;//cp.vfs.resolveFiles(name, cp.searchCtxs);
         for(VirtualFile vf : matches)
         {
            URL resURL = vf.toURL();
            resources.add(resURL);
         }
      }*/
      return resources.elements();
   }
   public Enumeration<URL> findResourcesLocally(String name) throws IOException
   {
      return findResources(name);
   }

   public ClassLoadingDomain getDomain()
   {
      return null;
   }
   public void setDomain(ClassLoadingDomain domain)
   {
   }

   public Class loadClassLocally(String name, boolean resolve)
      throws ClassNotFoundException
   {
      return findClass(name);
   }

   public URL loadResourceLocally(String name)
   {
      return this.findResource(name);
   }

   /**
    * Get the packages defined by the classloader
    * 
    * @return the packages
    */
   public Package[] getPackages()
   {
      return super.getPackages();
   }

   /**
    * Get a package defined by the classloader
    * 
    * @param name the name of the package
    * @return the package
    */
   public Package getPackage(String name)
   {
      return super.getPackage(name);
   }

   protected VirtualFile findResourceFile(String name)
   {
      VirtualFile vf = null;
      try
      {
         outer:
         for(ClassPathVFS cp : classpath)
         {
            for(String ctx : cp.searchCtxs)
            {
               String path = ctx + '/' + name;
               vf = cp.vfs.findChild(path);
               if( vf != null )
               {
                  break outer;
               }
            }
         }
      }
      catch (IOException e)
      {
         if( log.isTraceEnabled() )
            log.trace("Failed to find resource: "+name, e);
      }
      return vf;
   }

   /**
    * Determine the protection domain. If we are a copy of the original
    * deployment, use the original url as the codebase.
    * 
    * @param classFile the virtual file for this class
    * @return the protection domain
    * @throws Exception for any error
    * TODO certificates and principles?
    */
   protected ProtectionDomain getProtectionDomain(VirtualFile classFile) throws Exception
   {
      Certificate certs[] = null;
      URL codesourceUrl = classFile.toURL();
      CodeSource cs = new CodeSource(codesourceUrl, certs);
      PermissionCollection permissions = SecurityActions.getPolicy().getPermissions(cs);
      if (log.isTraceEnabled())
         log.trace("getProtectionDomain, url=" + codesourceUrl +
                   " codeSource=" + cs + " permissions=" + permissions);
      return new ProtectionDomain(cs, permissions);
   }

   /**
    * Iterate through the searchCtxs and look for wildcard expressions. Currently only '*.jar' indicating
    * every jar in a directory is supported.
    * 
    * @param searchCtxs - input array of vfs paths relative to vfs
    * @param vfs - the vfs to resolve the searchCtxs against
    * @return the contexts
    * @throws IOException for any error
    */
   protected String[] resolveSearchCtxs(String[] searchCtxs, VFS vfs) throws IOException
   {
      ArrayList<String> tmp = new ArrayList<String>(searchCtxs.length);
      for(String ctx : searchCtxs)
      {
         if( ctx.endsWith("*.jar") )
         {
            // Obtain the parent directory name
            int slash = ctx.lastIndexOf('/');
            String dir = "";
            if( slash > 0 )
               dir = ctx.substring(0, slash);
            VirtualFile dirFile = vfs.findChild(dir);
            List<VirtualFile> children = dirFile.getChildren();
            StringBuilder sb = new StringBuilder(dir);
            sb.append('/');
            int dirLength = sb.length();
            for(VirtualFile child : children)
            {
               String name = child.getName();
               if( name.endsWith(".jar") )
               {
                  sb.append(name);
                  String path = sb.toString();
                  tmp.add(path);
                  sb.setLength(dirLength);
               }
            }
         }
         else
         {
            tmp.add(ctx);
         }
      }
      log.debug("Resolved searchCtxs to: "+tmp);
      String[] newCtxs = new String[tmp.size()];
      tmp.toArray(newCtxs);
      return newCtxs;
   }
}
