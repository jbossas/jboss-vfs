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

import org.jboss.jzipfile.ZipEntry;
import org.jboss.jzipfile.Zip;
import org.jboss.jzipfile.ZipCatalog;
import org.jboss.jzipfile.ZipEntryType;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.TempFileProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;

/**
 * {@inheritDoc}
 * <p/>
 * This implementation is backed by a zip file.  The provided file must be owned by this instance; otherwise, if the file
 * disappears unexpectedly, the filesystem will malfunction.
 */
public final class ZipFileSystem implements FileSystem
{
   private final File zipFile;
   private final ZipNode rootNode;
   private final TempFileProvider tempFileProvider;

   /**
    * Create a new instance.
    *
    * @param name the name of the source archive
    * @param inputStream an input stream from the source archive
    * @param tempFileProvider the temp file provider to use
    * @throws IOException if an I/O error occurs
    */
   public ZipFileSystem(String name, InputStream inputStream, TempFileProvider tempFileProvider) throws IOException {
      this(tempFileProvider.createTempFile(name, name.hashCode(), inputStream), tempFileProvider);
   }

   public ZipFileSystem(File zipFile, TempFileProvider tempFileProvider) throws IOException
   {
      this.zipFile = zipFile;
      this.tempFileProvider = tempFileProvider;
      final ZipCatalog catalog = Zip.readCatalog(zipFile);
      final Collection<ZipEntry> entries = catalog.allEntries();
      final ZipNode rootNode = new ZipNode(new HashMap<String, ZipNode>(), null);
      FILES: for (ZipEntry zipEntry : entries)
      {
         final List<String> tokens = PathTokenizer.getTokens(zipEntry.getName());
         ZipNode node = rootNode;
         final Iterator<String> it = tokens.iterator();
         while (it.hasNext())
         {
            String token = it.next();
            final Map<String, ZipNode> children = node.children;
            if (children == null) {
               // todo - log bad zip entry
               continue FILES;
            }
            ZipNode child = children.get(token);
            if (child == null)
            {
               child = it.hasNext() || zipEntry.getEntryType() == ZipEntryType.DIRECTORY ? new ZipNode(new HashMap<String, ZipNode>(), null) : new ZipNode(null, zipEntry);
               children.put(token, child);
            }
            node = child;
         }
      }
      this.rootNode = rootNode;
   }

   public File getFile(List<String> pathComponents) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(pathComponents);

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
         cachedFile = tempFileProvider.createTempFile(name, zipEntry.hashCode());
         VFSUtils.copyStreamAndClose(Zip.openEntry(zipFile, zipEntry), new FileOutputStream(cachedFile));
         zipNode.cachedFile = cachedFile;
         return cachedFile;
      }
   }

   public InputStream openInputStream(List<String> pathComponents) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(pathComponents);
      final File cachedFile = zipNode.cachedFile;
      if (cachedFile != null) {
         return new FileInputStream(cachedFile);
      }
      final ZipEntry entry = getNodeEntry(zipNode);
      return Zip.openEntry(zipFile, entry);
   }

   private ZipEntry getFileEntry(List<String> pathComponents)
         throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(pathComponents);
      return getNodeEntry(zipNode);
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

   private ZipNode getExistingZipNode(List<String> pathComponents)
         throws FileNotFoundException
   {
      final ZipNode zipNode = rootNode.find(pathComponents.iterator());
      if (zipNode == null) {
         throw new FileNotFoundException(join(pathComponents));
      }
      return zipNode;
   }

   private static String join(List<String> pathComponents)
   {
      int l = 0;
      for (String pathComponent : pathComponents)
      {
         l += pathComponent.length();
      }
      final StringBuilder sb = new StringBuilder(l);
      for (String pathComponent : pathComponents)
      {
         sb.append('/');
         sb.append(pathComponent);
      }
      return sb.toString();
   }

   public boolean isReadOnly()
   {
      return true;
   }

   public boolean delete(List<String> pathComponents) throws IOException
   {
      return false;
   }

   public long getSize(List<String> pathComponents) throws IOException
   {
      final ZipEntry entry = getFileEntry(pathComponents);
      return entry.getSize();
   }

   public long getLastModified(List<String> pathComponents) throws IOException
   {
      final ZipNode zipNode = getExistingZipNode(pathComponents);
      final ZipEntry entry = zipNode.entry;
      if (entry != null) {
         return entry.getModificationTime();
      } else {
         return 0L;
      }
   }

   public boolean exists(List<String> pathComponents) throws IOException
   {
      return rootNode.find(pathComponents.iterator()) != null;
   }

   public boolean isDirectory(List<String> pathComponents) throws IOException
   {
      final ZipNode zipNode = rootNode.find(pathComponents.iterator());
      return zipNode != null && zipNode.children != null;
   }

   public Iterator<String> getDirectoryEntries(List<String> directoryPathComponents) throws IOException
   {
      return null;
   }

   public void close() throws IOException
   {
      
   }

   private static final class ZipNode {
      private final Map<String, ZipNode> children;
      private final ZipEntry entry;
      private volatile File cachedFile;

      private ZipNode(Map<String, ZipNode> children, ZipEntry entry)
      {
         this.children = children;
         this.entry = entry;
      }

      private ZipNode find(Iterator<String> node) {
         if (node.hasNext())
         {
            final ZipNode next = children.get(node.next());
            return next == null ? null : next.find(node);
         }
         else
         {
            return this;
         }
      }
   }
}
