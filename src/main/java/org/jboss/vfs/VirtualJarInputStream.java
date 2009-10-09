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
package org.jboss.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.jboss.vfs.util.PathTokenizer;

/**
 * Virtual JarInputStream used for representing any VFS directory as a JarInputStream.
 *
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class VirtualJarInputStream extends JarInputStream {
   private final Deque<Iterator<VirtualFile>> entryItr = new ArrayDeque<Iterator<VirtualFile>>();

   private final VirtualFile root;

   private InputStream currentEntryStream;

   private boolean closed;

   /**
    * Construct a {@link VirtualJarInputStream} from a {@link VirtualFile} root
    * 
    * @param root VirtualFile directory to use as the base of the virtual Jar.
    * @throws IOException
    */
   public VirtualJarInputStream(VirtualFile root) throws IOException {
      super(VFSUtils.emptyStream());
      this.root = root;
      entryItr.add(root.getChildren().iterator());
   }

   /*
    * {@inheritDoc} 
    * @see java.util.jar.JarInputStream#getNextEntry()
    */
   @Override
   public ZipEntry getNextEntry() throws IOException {
      return getNextJarEntry();
   }

   /*
    *  {@inheritDoc} 
    * @see java.util.jar.JarInputStream#getNextJarEntry()
    */
   @Override
   public JarEntry getNextJarEntry() throws IOException {
      if (currentEntryStream != null) {
         currentEntryStream.close();
      }

      Iterator<VirtualFile> topItr = entryItr.peekFirst();
      if (topItr == null) {
         return null;
      }
      if (!topItr.hasNext()) {
         entryItr.pop();
         return getNextJarEntry();
      }
      VirtualFile current = topItr.next();
      if (current.isDirectory()) {
         entryItr.add(current.getChildren().iterator());
         return getNextJarEntry();
      }

      openCurrent(current);
      String entryName = getEntryName(current);
      Attributes attributes = null;
      Manifest manifest = getManifest();
      if (manifest != null) {
         attributes = manifest.getAttributes(entryName);
      }
      return new VirtualJarEntry(entryName, current, attributes);
   }

   /*
    * {@inheritDoc} 
    * @see java.util.jar.JarInputStream#getManifest()
    */
   @Override
   public Manifest getManifest() {
      try {
         return VFSUtils.getManifest(root);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /*
    * {@inheritDoc} 
    * @see java.util.zip.InflaterInputStream#read()
    */
   @Override
   public int read() throws IOException {
      ensureOpen();
      return checkForEoSAndReturn(currentEntryStream.read());
   }

   /*
    * {@inheritDoc}
    * @see java.io.FilterInputStream#read(byte[])
    */
   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   /*
    * {@inheritDoc}
    * @see java.util.jar.JarInputStream#read(byte[], int, int)
    */
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      ensureOpen();
      return checkForEoSAndReturn(currentEntryStream.read(b, off, len));
   }

   /*
    * {@inheritDoc}
    * @see java.util.zip.ZipInputStream#available()
    */
   @Override
   public int available() throws IOException {
      ensureOpen();
      return currentEntryStream.available() > 0 ? 1 : 0;
   }

   /*
    * {@inheritDoc}
    * @see java.util.zip.ZipInputStream#close()
    */
   @Override
   public void close() throws IOException {
      closed = true;
   }

   /*
    * {@inheritDoc}
    * @see java.util.zip.ZipInputStream#closeEntry()
    */
   @Override
   public void closeEntry() throws IOException {
      if (currentEntryStream != null) {
         currentEntryStream.close();
      }
   }

   /*
    * {@inheritDoc}
    * @see java.util.zip.ZipInputStream#skip(long)
    */
   @Override
   public long skip(long n) throws IOException {
      ensureOpen();
      return currentEntryStream.skip(n);
   }

   /**
    * Ensure there is currently a JarEntry stream open.
    * @throws IOException
    */
   private void ensureOpen() throws IOException {
      if (closed) {
         throw new IOException("Stream is closed");
      }
   }

   /**
    * Check to see if the result is the EOF and if so exchange the current entry stream with the empty stream.
    * 
    * @param result
    * @return int result
    * @throws IOException
    */
   private int checkForEoSAndReturn(int result) throws IOException {
      if (result == -1) {
         closeEntry();
         currentEntryStream = VFSUtils.emptyStream();
      }
      return result;
   }

   /**
    * Open the current virtual file as the current JarEntry stream.
    * @param current
    * @throws IOException
    */
   private void openCurrent(VirtualFile current) throws IOException {
      currentEntryStream = current.openStream();
   }

   /**
    * Get the entry name from a VirtualFile.
    * @param entry
    * @return
    */
   private String getEntryName(VirtualFile entry) {
      List<String> pathParts = VFSUtils.getRelativePath(root, entry);
      return PathTokenizer.getRemainingPath(pathParts, 0);
   }

   /**
    * Virtual JarEntry used for representing a child VirtualFile as a JarEntry.
    *
    * @author <a href="baileyje@gmail.com">John Bailey</a>
    * @version $Revision: 1.1 $
    */
   public static class VirtualJarEntry extends JarEntry {
      private final VirtualFile virtualFile;
      private final Attributes attributes;
      
      /**
       * Construct a new 
       * @param name
       * @param virtualFile
       * @param attributes
       */
      public VirtualJarEntry(String name, VirtualFile virtualFile, Attributes attributes) {
         super(name);
         this.virtualFile = virtualFile;
         this.attributes = attributes;
      }

      /*
       * {@inheritDoc}
       * @see java.util.jar.JarEntry#getAttributes()
       */
      @Override
      public Attributes getAttributes() throws IOException {
         return attributes;
      }

      /*
       * @inheritDoc}
       * @see java.util.jar.JarEntry#getCertificates()
       */
      @Override
      public Certificate[] getCertificates() {
         CodeSigner[] signers = getCodeSigners();
         if (signers == null) {
            return null;
         }
         List<Certificate> certs = new ArrayList<Certificate>();
         for (CodeSigner signer : signers) {
            certs.addAll(signer.getSignerCertPath().getCertificates());
         }
         return certs.toArray(new Certificate[certs.size()]);
      }
      
      /*
       * @inheritDoc}
       * @see java.util.jar.JarEntry#getCodeSigners()
       */
      @Override
      public CodeSigner[] getCodeSigners() {
         return virtualFile.getCodeSigners();
      }
   }
}
