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
package org.jboss.virtual.plugins.context.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.util.file.Files;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * FileHandler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.1 $
 */
public class FileHandler extends AbstractURLHandler implements StructuredVirtualFileHandler
{
   private static final long serialVersionUID = 1;
   /** The file */
   private transient File file;
   /** The child cache */
   private transient Map<String, VirtualFileHandler> childCache = Collections.synchronizedMap(new HashMap<String, VirtualFileHandler>());

   /**
    * Create a new FileHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param file the file
    * @param url the url
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url
    */
   public FileHandler(FileSystemContext context, VirtualFileHandler parent, File file, URL url) throws IOException
   {
      super(context, parent, url, file.getName());

      this.file = file;
      if (file.exists() == false)
         throw new FileNotFoundException("File does not exist: " + file.getCanonicalPath());
      setVfsUrl(new URL("vfs" + url));
      // do cache here - on the file
      cachedLastModified = getLastModified();
   }
   /**
    * Create a new FileHandler
    *  
    * @param context the context
    * @param parent the parent
    * @param file the file
    * @param uri the uri
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, uri
    */
   public FileHandler(FileSystemContext context, VirtualFileHandler parent, File file, URI uri) throws IOException
   {
      this(context, parent, file, uri.toURL());
   }

   protected void initCacheLastModified()
   {
      // ignore that url.openConnection.lastModified
   }

   protected URL toInternalVfsUrl() throws MalformedURLException, URISyntaxException
   {
      if (getVfsUrl() == null)
      {
         setVfsUrl(new URL("vfs" + getURL()));
      }
      return getVfsUrl();
   }

   public URL getRealURL() throws IOException, URISyntaxException
   {
      return getURL();
   }

   @Override
   public FileSystemContext getVFSContext()
   {
      return (FileSystemContext) super.getVFSContext();
   }
   
   /**
    * Get the file for this file handler
    * 
    * @return the file
    */
   protected File getFile()
   {
      checkClosed();
      return file;
   }
   
   @Override
   public long getLastModified()
   {
      return getFile().lastModified();
   }

   public boolean exists() throws IOException
   {
      return getFile().exists();
   }

   @Override
   public long getSize()
   {
      return getFile().length();
   }

   public boolean isLeaf()
   {
      return getFile().isFile();
   }

   public boolean isHidden()
   {
      return getFile().isHidden();
   }

   public boolean isNested() throws IOException
   {
      return false;
   }

   @Override
   public void cleanup()
   {
      if (isTemporary())
      {
         try
         {
            delete(2000);
         }
         catch (Exception ignored)
         {
         }
      }
   }

   public boolean delete(int gracePeriod) throws IOException
   {
      // we can bypass the checkClosed
      File f = file;

      boolean exists = f.exists();
      if (exists == false)
         return false;

      if (Files.delete(f) == false)
      {
         long endOfGrace = System.currentTimeMillis() + gracePeriod;
         while(System.currentTimeMillis() < endOfGrace)
         {
            boolean done = Files.delete(f);
            if (done)
            {
               childCache.remove(f.getName());
               return true;
            }
            
            try
            {
               Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
               IOException ioe = new IOException("Interrupted: " + e);
               ioe.initCause(e);
               throw ioe;
            }
         }
         return false;
      }
      else
      {
         childCache.remove(f.getName());
         return true;
      }
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      File parent = getFile();
      File[] files = parent.listFiles();
      if (files == null)
         throw new IOException("Error listing files: " + parent.getCanonicalPath());
      // We need to validate the files list due to jdk bug 6192331
      List<File> tmpFiles = new ArrayList<File>();
      for (File file : files)
      {
         if( file.canRead() == true )
            tmpFiles.add(file);
      }
      if (tmpFiles.isEmpty())
         return Collections.emptyList();

      files = tmpFiles.toArray(new File[tmpFiles.size()]);
      FileSystemContext context = getVFSContext();
      
      List<VirtualFileHandler> result = new ArrayList<VirtualFileHandler>();
      Map<String, VirtualFileHandler> newCache = Collections.synchronizedMap(new HashMap<String, VirtualFileHandler>());
      Map<String, VirtualFileHandler> oldCache = childCache;
      // fill up a new cache with old entries
      // old entries no longer existing in directory are purged by not being added to new cache
      // we cache handlers so that things like JARs are recreated (optimization)
      for (File file : files)
      {
         try
         {
            VirtualFileHandler handler;
            handler = oldCache.get(file.getName());
            // if underlying file has been modified then create a new handler instead of using the cached one
            if (handler != null && handler.hasBeenModified())
            {
               handler = null;
            }
            if (handler == null)
            {
               handler = context.createVirtualFileHandler(this, file);
            }
            if (handler != null)
            {
               result.add(handler);
               newCache.put(file.getName(), handler);
            }
         }
         catch (IOException e)
         {
            if (ignoreErrors)
               log.trace("Ignored: " + e);
            else
               throw e;
         }
      }
      // cleanup old entries
      childCache = newCache;
      return result;
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      File parentFile = getFile();
      File child = new File(parentFile, name);
      VirtualFileHandler handler = childCache.get(name);
      // if a child has already been created use that
      // if the child has been modified on disk then create a new handler
      if (handler != null && (handler.hasBeenModified() || handler.exists() == false))
      {
         childCache.remove(name);
         handler = null;
      }
      if (handler == null)
      {
         FileSystemContext context = getVFSContext();
         handler = context.createVirtualFileHandler(this, child);
         if (handler != null)
            childCache.put(name, handler);
      }
      return handler;
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }

   /**
    * Find an existing LinkHandler or create a new one.
    *
    * @param name a handler name
    * @return cached or newly created LinkHandler
    * @throws IOException for any error
    */
   LinkHandler getChildLink(String name) throws IOException
   {
      VirtualFileHandler handler = childCache.get(name);
      if (handler == null)
      {
         // check if .vfslink.properties file exists for this name
         File file = new File(getFile(), name + VFSUtils.VFS_LINK_PROPERTIES_SUFFIX);
         if (file.isFile())
         {
            FileSystemContext ctx = getVFSContext();
            return ctx.createLinkHandler(this, file, name);
         }
      }
      else if (handler instanceof LinkHandler)
      {
         LinkHandler link = (LinkHandler) handler;
         if (link.exists())
         {
            // detect any changes in configuration
            FileSystemContext ctx = getVFSContext();
            try
            {
               return ctx.createLinkHandler(this, new File(link.toURI()), name);
            }
            catch (URISyntaxException e)
            {
               IOException ex = new IOException("Failed to convert link to URI: " + link);
               ex.initCause(e);
               throw ex;
            }
         }
         else
         {
            // remove from cache
            childCache.remove(name);
         }
      }
      return null;
   }

   public boolean removeChild(String name) throws IOException
   {
      return (childCache.remove(name) != null);
   }

   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      childCache.put(original.getName(), replacement);
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      in.defaultReadObject();
      // Initialize the transient values
      this.file = new File(getURL().getPath());
      this.childCache = Collections.synchronizedMap(new HashMap<String, VirtualFileHandler>());
   }
}
