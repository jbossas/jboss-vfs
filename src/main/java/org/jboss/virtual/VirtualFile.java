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
package org.jboss.virtual ;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;
import org.jboss.virtual.plugins.vfs.helpers.MatchAllVirtualFileFilter;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.util.collection.WeakSet;

/**
 * A virtual file as seen by the user
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 44334 $
 */
public class VirtualFile implements Serializable
{
   private static final long serialVersionUID = 1L;

   /** The virtual file handler */
   private final VirtualFileHandler handler;

   /** Whether we are closed */
   private final AtomicBoolean closed = new AtomicBoolean(false);

   /** The open streams */
   private transient Set<InputStream> streams;

   /**
    * Create a new VirtualFile.
    *
    * @param handler the handler
    * @throws IllegalArgumentException if the handler is null
    */
   public VirtualFile(VirtualFileHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");
      this.handler = handler;
   }

   /**
    * Get the virtual file handler
    *
    * @return the handler
    * @throws IllegalStateException if the file is closed
    */
   VirtualFileHandler getHandler()
   {
      if (closed.get())
         throw new IllegalStateException("The virtual file is closed");
      return handler;
   }

   /**
    * Get the simple VF name (X.java)
    *
    * @return the simple file name
    * @throws IllegalStateException if the file is closed
    */
   public String getName()
   {
      return getHandler().getName();
   }

   /**
    * Get the VFS relative path name (org/jboss/X.java)
    *
    * @return the VFS relative path name
    * @throws IllegalStateException if the file is closed
    */
   public String getPathName()
   {
      return getHandler().getPathName();
   }

   /**
    * Get the VF URL (file://root/org/jboss/X.java)
    *
    * @return the full URL to the VF in the VFS.
    * @throws MalformedURLException if a url cannot be parsed
    * @throws URISyntaxException if a uri cannot be parsed
    * @throws IllegalStateException if the file is closed
    */
   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return getHandler().toVfsUrl();
   }

   /**
    * Get the VF URI (file://root/org/jboss/X.java)
    *
    * @return the full URI to the VF in the VFS.
    * @throws URISyntaxException if a uri cannot be parsed
    * @throws IllegalStateException if the file is closed
    * @throws MalformedURLException for a bad url
    */
   public URI toURI() throws MalformedURLException, URISyntaxException
   {
      return VFSUtils.toURI(toURL());
   }

   /**
    * When the file was last modified
    *
    * @return the last modified time
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public long getLastModified() throws IOException
   {
      return getHandler().getLastModified();
   }

   /**
    * Returns true if the file has been modified since this method was last called
    * Last modified time is initialized at handler instantiation.
    *
    * @return true if modifed, false otherwise
    * @throws IOException for any error
    */
   public boolean hasBeenModified() throws IOException
   {
      return getHandler().hasBeenModified();
   }

   /**
    * Get the size
    *
    * @return the size
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public long getSize() throws IOException
   {
      return getHandler().getSize();
   }

   /**
    * Tests whether the underlying implementation file still exists.
    * @return true if the file exists, false otherwise.
    * @throws IOException - thrown on failure to detect existence.
    */
   public boolean exists() throws IOException
   {
      return getHandler().exists();      
   }

   /**
    * Whether it is a simple leaf of the VFS,
    * i.e. whether it can contain other files
    *
    * @return true if a simple file.
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public boolean isLeaf() throws IOException
   {
      return getHandler().isLeaf();
   }

   /**
    * Is the file archive.
    * 
    * @return true if archive, false otherwise
    * @throws IOException for any error
    */
   public boolean isArchive() throws IOException
   {
      return getHandler().isArchive();
   }

   /**
    * Whether it is hidden
    *
    * @return true when hidden
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public boolean isHidden() throws IOException
   {
      return getHandler().isHidden();
   }

   /**
    * Access the file contents.
    *
    * @return an InputStream for the file contents.
    * @throws IOException for any error accessing the file system
    * @throws IllegalStateException if the file is closed
    */
   public InputStream openStream() throws IOException
   {
      InputStream result = getHandler().openStream();
      checkStreams();
      streams.add(result);
      return result;
   }

   /**
    * Check if streams set exist.
    */
   @SuppressWarnings("unchecked")
   protected void checkStreams()
   {
      if (streams == null)
      {
         synchronized (closed)
         {
            // double null check, so that possible
            // waiting threads don't override streams
            if (streams == null)
               streams = Collections.synchronizedSet(new WeakSet());
         }
      }
   }

   /**
    * Close the streams
    */
   public void closeStreams()
   {
      if (streams == null)
         return;

      // Close the streams
      for (InputStream stream : streams)
      {
         if (stream != null)
         {
            try
            {
               stream.close();
            }
            catch (IOException ignored)
            {
            }
         }
      }
      streams.clear();
   }

   /**
    * Do file cleanup.
    * e.g. delete temp files
    */
   public void cleanup()
   {
      try
      {
         getHandler().cleanup();
      }
      finally
      {
         VFS.cleanup(this);
      }
   }

   /**
    * Close the file resources (stream, etc.)
    */
   public void close()
   {
      if (closed.getAndSet(true) == false)
      {
         closeStreams();
         handler.close();
      }
   }

   /**
    * Delete this virtual file
    *
    * @return true if file was deleted
    * @throws IOException if an error occurs
    */
   public boolean delete() throws IOException
   {
      // gracePeriod of 2 seconds
      return getHandler().delete(2000);
   }

   /**
    * Delete this virtual file
    *
    * @param gracePeriod max time to wait for any locks (in milliseconds)
    * @return true if file was deleted
    * @throws IOException if an error occurs
    */
   public boolean delete(int gracePeriod) throws IOException
   {
      return getHandler().delete(gracePeriod);
   }

   /**
    * Get the VFS instance for this virtual file
    *
    * @return the VFS
    * @throws IllegalStateException if the file is closed
    */
   public VFS getVFS()
   {
      VFSContext context = getHandler().getVFSContext();
      return context.getVFS();
   }

   /**
    * Get the parent
    *
    * @return the parent or null if there is no parent
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public VirtualFile getParent() throws IOException
   {
      VirtualFileHandler parent = getHandler().getParent();
      if (parent != null)
         return parent.getVirtualFile();
      return null;
   }

   /**
    * Get the children
    *
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public List<VirtualFile> getChildren() throws IOException
   {
      return getChildren(null);
   }

   /**
    * Get the children
    *
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed or it is a leaf node
    */
   public List<VirtualFile> getChildren(VirtualFileFilter filter) throws IOException
   {
      if (isLeaf())
         return Collections.emptyList();

      if (filter == null)
         filter = MatchAllVirtualFileFilter.INSTANCE;
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, null);
      visit(visitor);
      return visitor.getMatched();
   }

   /**
    * Get all the children recursively<p>
    *
    * This always uses {@link VisitorAttributes#RECURSE}
    *
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed
    */
   public List<VirtualFile> getChildrenRecursively() throws IOException
   {
      return getChildrenRecursively(null);
   }

   /**
    * Get all the children recursively<p>
    *
    * This always uses {@link VisitorAttributes#RECURSE}
    *
    * @param filter to filter the children
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalStateException if the file is closed or it is a leaf node
    */
   public List<VirtualFile> getChildrenRecursively(VirtualFileFilter filter) throws IOException
   {
      if (isLeaf())
         return Collections.emptyList();

      if (filter == null)
         filter = MatchAllVirtualFileFilter.INSTANCE;
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, VisitorAttributes.RECURSE);
      visit(visitor);
      return visitor.getMatched();
   }

   /**
    * Visit the virtual file system
    *
    * @param visitor the visitor
    * @throws IOException for any problem accessing the virtual file system
    * @throws IllegalArgumentException if the visitor is null
    * @throws IllegalStateException if the file is closed
    */
   public void visit(VirtualFileVisitor visitor) throws IOException
   {
      if (isLeaf() == false)
         getVFS().visit(this, visitor);
   }

   /**
    * Find a child
    *
    * @param path the path
    * @return the child
    * @throws IOException for any problem accessing the VFS (including the child does not exist)
    * @throws IllegalArgumentException if the path is null
    * @throws IllegalStateException if the file is closed or it is a leaf node
    * @deprecated use getChild, and handle null if not found
    */
   @Deprecated
   public VirtualFile findChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      VirtualFileHandler handler = getHandler();      
      VirtualFileHandler child = handler.getChild(VFSUtils.fixName(path));
      if (child == null)
      {
         List<VirtualFileHandler> children = handler.getChildren(true);
         throw new IOException("Child not found " + path + " for " + handler + ", available children: " + children);
      }
      return child.getVirtualFile();
   }

   /**
    * Get a child
    *
    * @param path the path
    * @return the child or <code>null</code> if not found
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the path is null
    * @throws IllegalStateException if the file is closed or it is a leaf node
    */
   public VirtualFile getChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      VirtualFileHandler handler = getHandler();
      VirtualFileHandler child = handler.getChild(VFSUtils.fixName(path));
      return child != null ? child.getVirtualFile() : null;
   }

   @Override
   public String toString()
   {
      return handler.toString();
   }

   @Override
   public int hashCode()
   {
      return handler.hashCode();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof VirtualFile == false)
         return false;
      VirtualFile other = (VirtualFile) obj;
      return handler.equals(other.handler);
   }
}
