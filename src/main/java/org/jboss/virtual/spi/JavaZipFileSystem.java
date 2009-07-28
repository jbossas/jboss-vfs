/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Closeable;
import java.io.BufferedOutputStream;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collection;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * {@inheritDoc}
 * <p/>
 * This implementation is backed by a zip file.  The provided file must be owned by this instance; otherwise, if the file
 * disappears unexpectedly, the filesystem will malfunction.
 */
public final class JavaZipFileSystem implements FileSystem
{
   private final ZipFile zipFile;
   private final long zipTime;
   private final ZipNode rootNode;
   private final TempDir tempDir;
   private final File contentsDir;

   /**
    * Create a new instance.
    *
    * @param name the name of the source archive
    * @param inputStream an input stream from the source archive
    * @param tempDir the temp dir into which zip information is stored
    * @throws java.io.IOException if an I/O error occurs
    */
   public JavaZipFileSystem(String name, InputStream inputStream, TempDir tempDir) throws IOException {
      this(tempDir.createFile(name, inputStream), tempDir);
   }

   /**
    * Create a new instance.
    *
    * @param archiveFile the original archive file
    * @param tempDir the temp dir into which zip information is stored
    * @throws java.io.IOException if an I/O error occurs
    */
   public JavaZipFileSystem(File archiveFile, TempDir tempDir) throws IOException
   {
      zipTime = archiveFile.lastModified();
      final ZipFile zipFile;
      this.zipFile = zipFile = new ZipFile(archiveFile);
      this.tempDir = tempDir;
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      final ZipNode rootNode = new ZipNode(new HashMap<String, ZipNode>(), "", null);
      FILES: for (ZipEntry entry : iter(entries))
      {
         final String name = entry.getName();
         final boolean isDirectory = entry.isDirectory();
         final List<String> tokens = PathTokenizer.getTokens(name);
         ZipNode node = rootNode;
         final Iterator<String> it = tokens.iterator();
         while (it.hasNext())
         {
            String token = it.next();
            if (PathTokenizer.isCurrentToken(token) || PathTokenizer.isReverseToken(token)) {
               // invalid file name
               continue FILES;
            }
            final Map<String, ZipNode> children = node.children;
            if (children == null) {
               // todo - log bad zip entry
               continue FILES;
            }
            final String lcToken = token.toLowerCase();
            ZipNode child = children.get(lcToken);
            if (child == null)
            {
               child = it.hasNext() || isDirectory ? new ZipNode(new HashMap<String, ZipNode>(), token, null) : new ZipNode(null, token, entry);
               children.put(lcToken, child);
            }
            node = child;
         }
      }
      this.rootNode = rootNode;
      contentsDir = tempDir.getFile("contents");
      contentsDir.mkdir();
   }

   private static <T> Iterable<T> iter(final Enumeration<T> entries)
   {
      return new EnumerationIterable<T>(entries);
   }

   public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);

      // check if we have cached one already
      File cachedFile = zipNode.cachedFile;
      if (cachedFile != null) {
         return cachedFile;
      }
      synchronized (zipNode) {
         // double-check
         cachedFile = zipNode.cachedFile;
         if (cachedFile != null) {
            return cachedFile;
         }

         // nope, create a cached temp
         final ZipEntry zipEntry = getNodeEntry(zipNode);
         final String name = zipEntry.getName();
         cachedFile = new File(contentsDir, name);
         VFSUtils.copyStreamAndClose(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(cachedFile)));
         zipNode.cachedFile = cachedFile;
         return cachedFile;
      }
   }

   public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);
      final File cachedFile = zipNode.cachedFile;
      if (cachedFile != null) {
         return new FileInputStream(cachedFile);
      }
      final ZipEntry entry = zipNode.entry;
      if (entry == null) {
         throw new IOException("Not a file: \"" + target.getPathName() + "\"");
      }
      return zipFile.getInputStream(entry);
   }

   public boolean delete(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);
      final File cachedFile = zipNode.cachedFile;
      return cachedFile != null && cachedFile.delete();
   }

   public long getSize(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);
      final File cachedFile = zipNode.cachedFile;
      final ZipEntry entry = zipNode.entry;
      return cachedFile != null ? cachedFile.length() : entry == null ? 0L : entry.getSize();
   }

   public long getLastModified(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);
      final File cachedFile = zipNode.cachedFile;
      final ZipEntry entry = zipNode.entry;
      return cachedFile != null ? cachedFile.lastModified() : entry == null ? zipTime : entry.getTime();
   }

   public boolean exists(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = rootNode.find(mountPoint, target);
      if (zipNode == null) {
         return false;
      } else {
         final File cachedFile = zipNode.cachedFile;
         return cachedFile == null || cachedFile.exists();
      }
   }

   public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = rootNode.find(mountPoint, target);
      return zipNode != null && zipNode.entry == null;
   }

   public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(mountPoint, target);
      final Map<String, ZipNode> children = zipNode.children;
      final Collection<ZipNode> values = children.values();
      final List<String> names = new ArrayList<String>(values.size());
      for (ZipNode node : values)
      {
         names.add(node.name);
      }
      return names;
   }

   private ZipEntry getNodeEntry(ZipNode zipNode)
         throws IOException
   {
      final ZipEntry entry = zipNode.entry;
      if (entry == null) {
         throw new IOException("Cannot call this operation on a directory");
      }
      return entry;
   }

   private ZipNode getExistingZipNode(VirtualFile mountPoint, VirtualFile target)
         throws FileNotFoundException
   {
      final ZipNode zipNode = rootNode.find(mountPoint, target);
      if (zipNode == null) {
         throw new FileNotFoundException(target.getPathName());
      }
      return zipNode;
   }

   public boolean isReadOnly()
   {
      return true;
   }

   public void close() throws IOException
   {
      VFSUtils.safeClose(new Closeable() {
         public void close() throws IOException {
            zipFile.close();
         }
      });
      tempDir.close();
   }

   private static final class ZipNode {
      // immutable child map
      private final Map<String, ZipNode> children;
      private final String name;
      private final ZipEntry entry;
      private volatile File cachedFile;

      private ZipNode(Map<String, ZipNode> children, String name, ZipEntry entry)
      {
         this.children = children;
         this.name = name;
         this.entry = entry;
      }

      private ZipNode find(VirtualFile mountPoint, VirtualFile target) {
         if (mountPoint.equals(target)) {
            return this;
         } else {
            final ZipNode parent = find(mountPoint, target.getParent());
            if (parent == null) {
               return null;
            }
            final Map<String, ZipNode> children = parent.children;
            if (children == null) {
               return null;
            }
            return children.get(target.getLowerCaseName());
         }
      }
   }
}