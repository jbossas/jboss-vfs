/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.virtual;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.plugins.vfs.helpers.WrappingVirtualFileHandlerVisitor;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.registry.VFSRegistry;

/**
 * Virtual File System
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="ales.justin@jboss.com">Ales Justin</a> 
 * @version $Revision: 1.1 $
 */
public class VFS
{
   /** The VFS Context */
   private final VFSContext context;

   static
   {
      init();
   }

   /**
    * Create a new VFS.
    *
    * @param context the context
    * @throws IllegalArgumentException for a null context
    */
   public VFS(VFSContext context)
   {
      if (context == null)
         throw new IllegalArgumentException("Null name");
      this.context = context;
   }

   /**
    * Initialize VFS protocol handlers package property. 
    */
   @SuppressWarnings({"deprecation", "unchecked"})
   public static void init()
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
      org.jboss.virtual.plugins.context.VfsArchiveBrowserFactory factory = org.jboss.virtual.plugins.context.VfsArchiveBrowserFactory.INSTANCE;
      // keep this until AOP and HEM uses VFS internally instead of the stupid ArchiveBrowser crap.
      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfsfile", factory);
      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfszip", factory);
      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfsjar", factory);
      org.jboss.util.file.ArchiveBrowser.factoryFinder.put("vfs", factory);
   }

   /**
    * Get the vfs context.
    *
    * This is package protected method.
    * Same as VirtualFile::getHandler. 
    *
    * @return the vfs context
    */
   VFSContext getContext()
   {
      return context;
   }

   /**
    * Set exception handler.
    *
    * @param exceptionHandler the exception handler.
    */
   public void setExceptionHandler(ExceptionHandler exceptionHandler)
   {
      context.setExceptionHandler(exceptionHandler);
   }

   /**
    * Cleanup any resources tied to this file.
    * e.g. vfs cache
    *
    * @param file the file
    */
   static void cleanup(VirtualFile file)
   {
      try
      {
         VirtualFileHandler fileHandler = file.getHandler();
         VFSContext context = fileHandler.getVFSContext();
         context.cleanupTempHandlers(fileHandler.getPathName());
         
         VirtualFileHandler contextHandler = context.getRoot();
         // the file is the context root, hence possible cache candidate
         if (fileHandler.equals(contextHandler))
         {
            VFSRegistry registry = VFSRegistry.getInstance();
            registry.removeContext(context);
         }
      }
      catch (Exception ignored)
      {
      }
   }

   /**
    * Get the virtual file system for a root uri
    * 
    * @param rootURI the root URI
    * @return the virtual file system
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL is null
    */
   public static VFS getVFS(URI rootURI) throws IOException
   {
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(rootURI);
      if (factory == null)
         throw new IOException("No context factory for " + rootURI);
      VFSContext context = factory.getVFS(rootURI);
      VFSRegistry.getInstance().addContext(context);
      return context.getVFS();
   }

   /**
    * Create new root
    *
    * @param rootURI the root url
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL
    */
   public static VirtualFile createNewRoot(URI rootURI) throws IOException
   {
      VFS vfs = getVFS(rootURI);
      return vfs.getRoot();
   }

   /**
    * Get the root virtual file
    * 
    * @param rootURI the root uri
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL is null
    */
   public static VirtualFile getRoot(URI rootURI) throws IOException
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile file = registry.getFile(rootURI);
      return (file != null) ? file : createNewRoot(rootURI);
   }

   /**
    * Get cached file.
    *
    * If VFSContext matching the rootURI parameter is cached
    * this method will return cached virtual file
    * else it will use VFS::getRoot(rootURI).
    *
    * @param rootURI the root uri
    * @return the cached virtual file
    * @throws IOException for any error
    * @throws IllegalArgumentException if the rootURL is null
    * @deprecated use getRoot
    */
   @Deprecated
   public static VirtualFile getCachedFile(URI rootURI) throws IOException
   {
      return getRoot(rootURI);
   }

   /**
    * Get a virtual file
    * 
    * @param rootURI the root uri
    * @param name the path name
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL or name is null
    */
   @SuppressWarnings("deprecation")
   public static VirtualFile getVirtualFile(URI rootURI, String name) throws IOException
   {
      VirtualFile root = getRoot(rootURI);
      return root.findChild(name);
   }

   /**
    * Get the virtual file system for a root url
    * 
    * @param rootURL the root url
    * @return the virtual file system
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL is null
    */
   public static VFS getVFS(URL rootURL) throws IOException
   {
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(rootURL);
      if (factory == null)
         throw new IOException("No context factory for " + rootURL);
      VFSContext context = factory.getVFS(rootURL);
      VFSRegistry.getInstance().addContext(context);
      return context.getVFS();
   }

   /**
    * Create new root
    * 
    * @param rootURL the root url
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL
    */
   public static VirtualFile createNewRoot(URL rootURL) throws IOException
   {
      VFS vfs = getVFS(rootURL);
      return vfs.getRoot();
   }

   /**
    * Get the root virtual file
    *
    * @param rootURL the root url
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL
    */
   public static VirtualFile getRoot(URL rootURL) throws IOException
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile file = registry.getFile(rootURL);
      return (file != null) ? file : createNewRoot(rootURL);
   }

   /**
    * Get cached file.
    *
    * If VFSContext matching the rootURL parameter is cached
    * this method will return cached virtual file
    * else it will use VFS::getRoot(rootURL).
    *
    * @param rootURL the root url
    * @return the cached virtual file
    * @throws IOException for any error
    * @throws IllegalArgumentException if the rootURL is null
    * @deprecated use getRoot
    */
   @Deprecated
   public static VirtualFile getCachedFile(URL rootURL) throws IOException
   {
      return getRoot(rootURL);
   }

   /**
    * Get a virtual file
    * 
    * @param rootURL the root url
    * @param name the path name
    * @return the virtual file
    * @throws IOException if there is a problem accessing the VFS
    * @throws IllegalArgumentException if the rootURL or name is null
    */
   @SuppressWarnings("deprecation")
   public static VirtualFile getVirtualFile(URL rootURL, String name) throws IOException
   {
      VirtualFile root = getRoot(rootURL);
      return root.findChild(name);
   }

   /**
    * Get the root file of this VFS
    * 
    * @return the root
    * @throws IOException for any problem accessing the VFS
    */
   public VirtualFile getRoot() throws IOException
   {
      VirtualFileHandler handler = context.getRoot();
      return handler.getVirtualFile();
   }
   
   /**
    * Find a child from the root
    *
    * @param path the child path
    * @return the child
    * @throws IOException for any problem accessing the VFS (including the child does not exist)
    * @throws IllegalArgumentException if the path is null
    * @deprecated use getChild, and handle null if not found
    */
   @Deprecated
   public VirtualFile findChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");
      
      VirtualFileHandler handler = context.getRoot();
      VirtualFileHandler result = context.getChild(handler, VFSUtils.fixName(path));
      if (result == null)
      {
         List<VirtualFileHandler> children = handler.getChildren(true);
         throw new IOException("Child not found " + path + " for " + handler + ", available children: " + children);
      }
      return result.getVirtualFile();
   }
   
   /**
   * Get a child
   *
   * @param path the child path
   * @return the child or <code>null</code> if not found
   * @throws IOException if a real problem occurs
   */
   public VirtualFile getChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      VirtualFileHandler handler = context.getRoot();
      VirtualFileHandler result = context.getChild(handler, VFSUtils.fixName(path));
      return result != null ? result.getVirtualFile() : null;
   }

   /**
    * Find a child from the root
    * 
    * @deprecated use {@link #findChild(String)}
    * @param path the child path
    * @return the child
    * @throws IOException for any problem accessing the VFS (including the child does not exist)
    * @throws IllegalArgumentException if the path is null
    */
   @Deprecated
   @SuppressWarnings("deprecation")
   public VirtualFile findChildFromRoot(String path) throws IOException
   {
      return findChild(path);
   }
   
   /**
    * Get the children
    * 
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildren() throws IOException
   {
      return getRoot().getChildren(null);
   }

   /**
    * Get the children
    * 
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildren(VirtualFileFilter filter) throws IOException
   {
      return getRoot().getChildren(filter);
   }
   
   /**
    * Get all the children recursively<p>
    * 
    * This always uses {@link VisitorAttributes#RECURSE}
    * 
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      return getRoot().getChildrenRecursively(null);
   }
   
   /**
    * Get all the children recursively<p>
    * 
    * This always uses {@link VisitorAttributes#RECURSE}
    * 
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    */
   public List<VirtualFile> getChildrenRecursively(VirtualFileFilter filter) throws IOException
   {
      return getRoot().getChildrenRecursively(filter);
   }
   
   /**
    * Visit the virtual file system from the root
    * 
    * @param visitor the visitor
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the visitor is null
    */
   public void visit(VirtualFileVisitor visitor) throws IOException
   {
      VirtualFileHandler handler = context.getRoot();
      if (handler.isLeaf() == false)
      {
         WrappingVirtualFileHandlerVisitor wrapper = new WrappingVirtualFileHandlerVisitor(visitor);
         context.visit(handler, wrapper);
      }
   }

   /**
    * Visit the virtual file system
    * 
    * @param file the file
    * @param visitor the visitor
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the file or visitor is null
    */
   protected void visit(VirtualFile file, VirtualFileVisitor visitor) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      VirtualFileHandler handler = file.getHandler();
      WrappingVirtualFileHandlerVisitor wrapper = new WrappingVirtualFileHandlerVisitor(visitor);
      VFSContext handlerContext = handler.getVFSContext();
      handlerContext.visit(handler, wrapper);
   }

   @Override
   public String toString()
   {
      return context.toString();
   }

   @Override
   public int hashCode()
   {
      return context.hashCode();
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof VFS == false)
         return false;
      VFS other = (VFS) obj;
      return context.equals(other.context);
   }
}
