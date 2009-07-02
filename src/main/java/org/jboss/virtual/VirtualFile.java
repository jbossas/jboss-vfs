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
package org.jboss.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;
import org.jboss.virtual.plugins.vfs.helpers.MatchAllVirtualFileFilter;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;

/**
 * A virtual file.  This is a symbolic reference to a location in the virtual file system hierarchy.  Holding a
 * {@code VirtualFile} instance gives no guarantees as to the presence or immutability of the referenced file or
 * any of its parent path elements.
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @author Ales.Justin@jboss.org
 * @version $Revision: 44334 $
 */
public class VirtualFile implements Serializable
{
   private static final long serialVersionUID = 1L;
   private final String path;
   private final String name;
   private final List<String> tokens;
   private final VFS vfs;

   VirtualFile(VFS vfs, List<String> realPath, String realPathString)
   {
      path = realPathString;
      final int size = realPath.size();
      name = size == 0 ? "" : realPath.get(size - 1);
      tokens = realPath;
      this.vfs = vfs;
   }

   /**
    * Get the simple VF name (X.java)
    *
    * @return the simple file name
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the VFS relative path name (org/jboss/X.java)
    *
    * @return the VFS relative path name
    */
   public String getPathName()
   {
      return path;
   }

   /**
    * When the file was last modified
    *
    * @return the last modified time
    * @throws IOException for any problem accessing the virtual file system
    */
   public long getLastModified() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().getLastModified(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Get the size
    *
    * @return the size
    * @throws IOException for any problem accessing the virtual file system
    */
   public long getSize() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().getSize(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Tests whether the underlying implementation file still exists.
    * @return true if the file exists, false otherwise.
    * @throws IOException - thrown on failure to detect existence.
    */
   public boolean exists() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().exists(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Whether it is a simple leaf of the VFS,
    * i.e. whether it can contain other files
    *
    * @return true if a simple file.
    * @throws IOException for any problem accessing the virtual file system
    * @deprecated use {@link #isDirectory()} instead
    */
   @Deprecated
   public boolean isLeaf() throws IOException
   {
      return ! isDirectory();
   }

   /**
    * Determine whether the named virtual file is a directory.
    *
    * @return {@code true} if it is a directory, {@code false} otherwise
    * @throws IOException if an I/O error occurs
    */
   public boolean isDirectory() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().isDirectory(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Access the file contents.
    *
    * @return an InputStream for the file contents.
    * @throws IOException for any error accessing the file system
    */
   public InputStream openStream() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().openInputStream(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Delete this virtual file
    *
    * @return true if file was deleted
    * @throws IOException if an error occurs
    */
   public boolean delete() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().delete(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Get a physical file for this virtual file.  Depending on the underlying file system type, this may simply return
    * an already-existing file; it may create a copy of a file; or it may reuse a preexisting copy of the file.  Furthermore,
    * the retured file may or may not have any relationship to other files from the same or any other virtual
    * directory.
    *
    * @return the physical file
    * @throws IOException if an I/O error occurs while producing the physical file
    */
   public File getPhysicalFile() throws IOException
   {
      final List<String> tokens = this.tokens;
      final VFS.Mount mount = vfs.getMount(tokens);
      return mount.getFileSystem().getFile(tokens.subList(mount.getRealMountPoint().size(), tokens.size()));
   }

   /**
    * Get the VFS instance for this virtual file
    *
    * @return the VFS
    */
   public VFS getVFS()
   {
      return vfs;
   }

   /**
    * Get a {@code VirtualFile} which represents the parent of this instance.
    *
    * @return the parent or {@code null} if there is no parent
    * @throws IOException for any problem accessing the virtual file system
    */
   public VirtualFile getParent() throws IOException
   {
      final List<String> tokens = this.tokens;
      final String path = this.path;
      final int size = tokens.size();
      if (size == 0)
         return null;
      else if (size == 1)
         return vfs.getRootVirtualFile();
      else
         return new VirtualFile(vfs, tokens.subList(0, size - 1), path.substring(0, path.lastIndexOf('/')));
   }

   /**
    * Get the children
    *
    * @return the children
    * @throws IOException for any problem accessing the virtual file system
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
      if (! isDirectory())
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
      if (! isDirectory())
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
      if (! isDirectory() == false)
         getVFS().visit(this, visitor);
   }

   /**
    * Get a child virtual file.
    *
    * @param path the path
    * @return the child or {@code null} if not found
    * @throws IOException for any problem accessing the VFS
    * @throws IllegalArgumentException if the path is null
    */
   public VirtualFile getChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      final List<String> newPathTokens = new ArrayList<String>();
      newPathTokens.addAll(tokens);
      PathTokenizer.getTokens(newPathTokens, path);
      final List<String> childPath = PathTokenizer.applySpecialPaths(newPathTokens);
      return new VirtualFile(vfs, newPathTokens, PathTokenizer.getRemainingPath(childPath, 0));
   }

   @Override
   public String toString()
   {
      return "Virtual file \"" + path + "\" for " + vfs;
   }

   @Override
   public boolean equals(Object o)
   {
      if (this == o)
         return true;
      if (! (o instanceof VirtualFile))
         return false;
      VirtualFile that = (VirtualFile) o;
      if (! path.equals(that.path))
         return false;
      if (vfs != that.vfs)
         return false;
      return true;
   }

   @Override
   public int hashCode()
   {
      int result = path.hashCode();
      result = 31 * result + vfs.hashCode();
      return result;
   }
}
