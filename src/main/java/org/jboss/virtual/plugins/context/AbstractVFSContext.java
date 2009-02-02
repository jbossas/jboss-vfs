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
package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.TempInfo;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandlerVisitor;

/**
 * AbstractVFSContext.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVFSContext implements VFSContext
{
   /** The log */
   protected final Logger log = Logger.getLogger(getClass());
   
   /** The VFS wrapper */
   private VFS vfs;
   
   /** The root url */
   private final URI rootURI;

   /** Options associated with the root URL */
   private Map<String, String> rootOptions;

   /** Root's peer within another context */
   private VirtualFileHandler rootPeer;

   /** The temp handlers */
   private Map<String, TempInfo> tempInfos = Collections.synchronizedSortedMap(new TreeMap<String, TempInfo>());

   /** The exception handler */
   private ExceptionHandler exceptionHandler;

   /**
    * Create a new AbstractVFSContext.
    * 
    * @param rootURI the root url
    * @throws IllegalArgumentException if rootURI is null
    */
   protected AbstractVFSContext(URI rootURI)
   {
      if (rootURI == null)
         throw new IllegalArgumentException("Null rootURI");
      this.rootURI = rootURI;
      String query = rootURI.getQuery();
      rootOptions = VFSUtils.parseURLQuery(query);
   }

   /**
    * Create a new AbstractVFSContext.
    * 
    * @param rootURL the root url
    * @throws URISyntaxException for illegal URL
    * @throws IllegalArgumentException if rootURI is null
    */
   protected AbstractVFSContext(URL rootURL) throws URISyntaxException
   {
      this(rootURL.toURI());
   }

   public VFS getVFS()
   {
      if (vfs == null)
         vfs = new VFS(this);
      return vfs;
   }

   public URI getRootURI()
   {
      return rootURI;
   }

   public void setRootPeer(VirtualFileHandler handler)
   {
      this.rootPeer = handler;
   }

   public VirtualFileHandler getRootPeer()
   {
      return rootPeer;
   }

   protected void addOption(String key, String value)
   {
      rootOptions.put(key, value);
   }

   public Map<String, String> getOptions()
   {
      return rootOptions;
   }

   /**
    * Get peer vfs context.
    *
    * @return the peer context
    */
   protected VFSContext getPeerContext()
   {
      VirtualFileHandler peer = getRootPeer();
      return peer != null ? peer.getVFSContext() : null;
   }

   /**
    * Helper method to set options on an URL
    *
    * @param url  url to set options on
    * @return url with query parameters
    * @throws java.net.MalformedURLException if url manipulation fails
    */
   protected URL setOptionsToURL(URL url) throws MalformedURLException
   {
      if (rootOptions.size() == 0)
         return url;

      StringBuilder sb = new StringBuilder(url.toString());
      sb.append("?");
      int i = 0;
      for (Map.Entry<String, String> ent : rootOptions.entrySet())
      {
         if (i > 0)
            sb.append("&");
         sb.append(ent.getKey()).append("=").append(ent.getValue());
         i++;
      }
      
      return new URL(sb.toString());
   }

   public List<VirtualFileHandler> getChildren(VirtualFileHandler parent, boolean ignoreErrors) throws IOException
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");
      return parent.getChildren(ignoreErrors);
   }

   public VirtualFileHandler getChild(VirtualFileHandler parent, String path) throws IOException
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");
      if (path == null)
         throw new IllegalArgumentException("Null path");
      return parent.getChild(path);
   }

   /**
    * Construct a URL from a given parent and a name
    *
    * @param parent a parent
    * @param name a name of the child
    * @return URL corresponding to a child
    * @throws IOException for any error
    */
   public URL getChildURL(VirtualFileHandler parent, String name) throws IOException
   {      
      if(parent != null)
      {
         VFSContext parentCtx = parent.getVFSContext();
         if (parentCtx != this)
         {
            if (parentCtx instanceof AbstractVFSContext)
            {
               return ((AbstractVFSContext) parentCtx).getChildURL(parent, name);
            }
            else
            {
               StringBuilder urlStr = new StringBuilder(512);
               try
               {
                  urlStr.append(parent.toURI());
                  if (urlStr.charAt( urlStr.length()-1) != '/')
                     urlStr.append("/");

                  urlStr.append(name);
                  return new URL(urlStr.toString());
               }
               catch (URISyntaxException e)
               {
                  throw new RuntimeException("Failed to create child URL: " + parent + " + " + name, e);
               }
            }
         }
      }

      StringBuilder urlStr = new StringBuilder(512);
      URI rootUri = getRootURI();
      urlStr.append(rootUri.getScheme())
              .append(":").append(rootUri.getPath());

      if(parent != null)
      {
         if (urlStr.charAt( urlStr.length()-1) != '/')
            urlStr.append("/");

         String pPathName = parent.getPathName();         
         if(pPathName.length() != 0)
            urlStr.append(pPathName);

         if (urlStr.charAt( urlStr.length()-1) != '/')
            urlStr.append("/");

         urlStr.append(name);
      }

      return new URL(urlStr.toString());
   }

   public void visit(VirtualFileHandler handler, VirtualFileHandlerVisitor visitor) throws IOException
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      if (visitor == null)
         throw new IllegalArgumentException("Null visitor");
      
      VisitorAttributes attributes = visitor.getAttributes();
      boolean includeRoot = attributes.isIncludeRoot();
      boolean leavesOnly = attributes.isLeavesOnly();
      boolean ignoreErrors = attributes.isIgnoreErrors();
      boolean includeHidden = attributes.isIncludeHidden();
      VirtualFileFilter recurseFilter = attributes.getRecurseFilter();
      visit(handler, visitor, includeRoot, leavesOnly, ignoreErrors, includeHidden, recurseFilter);
   }

   /**
    * Visit. the file system, recursive death checking is left to the visitor
    * or otherwise a stack overflow.
    * 
    * @param handler the reference handler
    * @param visitor the visitor
    * @param includeRoot whether to visit the root
    * @param leavesOnly whether to visit leaves only
    * @param ignoreErrors whether to ignore errors
    * @param includeHidden whether to include hidden files
    * @param recurseFilter the recurse filter
    * @throws IOException for any problem accessing the virtual file system
    */
   protected void visit(VirtualFileHandler handler, VirtualFileHandlerVisitor visitor,
         boolean includeRoot, boolean leavesOnly, boolean ignoreErrors,
         boolean includeHidden, VirtualFileFilter recurseFilter)
      throws IOException
   {
      // Visit the root when asked
      if (includeRoot)
         visitor.visit(handler);
      
      // Visit the children
      boolean trace = log.isTraceEnabled();
      List<VirtualFileHandler> children;
      try
      {
          children = getChildren(handler, ignoreErrors);
      }
      catch (IOException e)
      {
         if (ignoreErrors == false)
            throw e;
         if( trace )
            log.trace("Ignored: " + e);
         return;
      }
      
      // Look through each child
      for (VirtualFileHandler child : children)
      {
         // Ignore hidden if asked
         if (includeHidden == false && child.isHidden())
         {
            if( trace )
               log.trace("Ignoring hidden file: "+child);
            continue;
         }
         
         // Visit the leaf or non-leaves when asked
         boolean isLeaf = child.isLeaf();
         if (leavesOnly == false || isLeaf)
            visitor.visit(child);
         else if( trace )
         {
            log.trace("Skipping non-leaf file: "+child);
         }

         // Recurse when asked
         VirtualFile file = child.getVirtualFile();
         if ( isLeaf == false && recurseFilter != null && recurseFilter.accepts(file))
         {
            try
            {
               if (handler instanceof DelegatingHandler)
                  child.getVFSContext().visit(child, visitor);
               else
                  visit(child, visitor, false, leavesOnly, ignoreErrors, includeHidden, recurseFilter);
            }
            catch (StackOverflowError e)
            {
               log.debug("Original: " + child, e);
               throw new IOException("Stack overflow, the file system is too complicated? " + child);
            }
         }
      }
   }

   public void addTempInfo(TempInfo tempInfo)
   {
      tempInfos.put(tempInfo.getPath(), tempInfo);
   }

   public TempInfo getTempInfo(String path)
   {
      return tempInfos.get(path);
   }

   public Iterable<TempInfo> getTempInfos()
   {
      return tempInfos.values();
   }

   public void cleanupTempInfo(String path)
   {
      Iterator<Map.Entry<String, TempInfo>> iter = tempInfos.entrySet().iterator();
      while (iter.hasNext())
      {
         Map.Entry<String, TempInfo> entry = iter.next();
         if (entry.getKey().startsWith(path))
         {
            try
            {
               entry.getValue().cleanup();
            }
            catch (Throwable ignored)
            {
            }
            iter.remove();
         }
      }
   }

   public ExceptionHandler getExceptionHandler()
   {
      return exceptionHandler;
   }

   public void setExceptionHandler(ExceptionHandler exceptionHandler)
   {
      this.exceptionHandler = exceptionHandler;
   }

   @Override
   public String toString()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append(getClass().getSimpleName());
      buffer.append('@');
      buffer.append(System.identityHashCode(this));
      buffer.append('[');
      buffer.append(rootURI);
      buffer.append(']');
      return buffer.toString();
   }
   
   @Override
   public int hashCode()
   {
      return rootURI.hashCode();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null || obj instanceof VFSContext == false)
         return false;
      VFSContext other = (VFSContext) obj;
      return rootURI.equals(other.getRootURI());
   }
}
