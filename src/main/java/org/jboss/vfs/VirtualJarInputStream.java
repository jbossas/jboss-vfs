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
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Virtual JarInputStream used for representing any VFS directory as a JarInputStream.
 *
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class VirtualJarInputStream extends JarInputStream {
   private static final String MANIFEST_NAME = "MANIFEST.MF";

   private static final String META_INF_DIR = "META-INF";

   private final Deque<Iterator<VirtualFile>> entryItr = new ArrayDeque<Iterator<VirtualFile>>();

   private final VirtualFile root;
   private final Manifest manifest;

   private InputStream currentEntryStream = VFSUtils.emptyStream();

   private boolean closed;

   private static final VirtualFileFilter MANIFEST_FILTER = new VirtualFileFilter() {
      public boolean accepts(VirtualFile file) {
         return !MANIFEST_NAME.equalsIgnoreCase(file.getName());
      }
   };

   /**
    * Construct a {@link VirtualJarInputStream} from a {@link VirtualFile} root
    * 
    * @param root VirtualFile directory to use as the base of the virtual Jar.
    * @throws IOException
    */
   public VirtualJarInputStream(VirtualFile root) throws IOException {
      super(VFSUtils.emptyStream());
      this.root = root;
      final VirtualFile manifest = root.getChild(JarFile.MANIFEST_NAME); 
      if(manifest.exists()) {
         entryItr.add(Collections.singleton(manifest).iterator());
         this.manifest = VFSUtils.readManifest(manifest);
      } else {
         this.manifest = null;
      }
      entryItr.add(root.getChildren().iterator());
   }

   /** {@inheritDoc} **/ 
   @Override
   public ZipEntry getNextEntry() throws IOException {
      return getNextJarEntry();
   }

   /** {@inheritDoc} **/ 
   @Override
   public JarEntry getNextJarEntry() throws IOException {
      closeEntry();

      final Iterator<VirtualFile> topItr = entryItr.peekFirst();
      if (topItr == null) {
         return null;
      }
      if (!topItr.hasNext()) {
         entryItr.pop();
         return getNextJarEntry();
      }
      
      final VirtualFile nextEntry = topItr.next();
      String entryName = getEntryName(nextEntry);
      if (nextEntry.isDirectory()) {
         List<VirtualFile> children = nextEntry.getChildren();
         if(entryName.equalsIgnoreCase(META_INF_DIR)) {
            children = nextEntry.getChildren(MANIFEST_FILTER);
         }
         entryItr.add(children.iterator());
         entryName = fixDirectoryName(entryName);
      }
      openCurrent(nextEntry);
      
      Attributes attributes = null;
      final Manifest manifest = getManifest();
      if (manifest != null) {
         attributes = manifest.getAttributes(entryName);
      }
      return new VirtualJarEntry(entryName, nextEntry, attributes);
   }
   

   /** {@inheritDoc} **/ 
   @Override
   public Manifest getManifest() {
      return manifest;
   }

   /** {@inheritDoc} **/
   @Override
   public int read() throws IOException {
      ensureOpen();
      return checkForEoSAndReturn(currentEntryStream.read());
   }

   /** {@inheritDoc} **/
   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   /** {@inheritDoc} **/
   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      ensureOpen();
      return checkForEoSAndReturn(currentEntryStream.read(b, off, len));
   }
   
   /** {@inheritDoc} **/
   @Override
   public int available() throws IOException {
      ensureOpen();
      return currentEntryStream.available() > 0 ? 1 : 0;
   }

   /** {@inheritDoc} **/
   @Override
   public void close() throws IOException {
      closed = true;
   }

   /** {@inheritDoc} **/
   @Override
   public void closeEntry() throws IOException {
      if (currentEntryStream != null) {
         currentEntryStream.close();
      }
   }

   /** {@inheritDoc} **/
   @Override
   public long skip(long n) throws IOException {
      ensureOpen();
      return currentEntryStream.skip(n);
   }

   /** {@inheritDoc} **/
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
      if(current.isDirectory()) {
         currentEntryStream = VFSUtils.emptyStream();
      } else {
         currentEntryStream = current.openStream();
      }
   }

   /**
    * Get the entry name from a VirtualFile.
    * @param entry
    * @return
    */
   private String getEntryName(VirtualFile entry) {
      return entry.getPathNameRelativeTo(root);
   }
   
   /** 
    * Make sure directory names end with a trailing slash
    * @param name
    * @return
    */
   private String fixDirectoryName(String name) {
      if(!name.endsWith("/")) {
         return name + "/";
      }
      return name;
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

      /** {@inheritDoc} **/
      @Override
      public Attributes getAttributes() throws IOException {
         return attributes;
      }

      /** {@inheritDoc} **/
      @Override
      public long getSize() {
         return virtualFile.getSize();
      }

      /** {@inheritDoc} **/
      @Override
      public boolean isDirectory() {
         return virtualFile.isDirectory();
      }

      /** {@inheritDoc} **/
      @Override
      public Certificate[] getCertificates() {
         final CodeSigner[] signers = getCodeSigners();
         if (signers == null) {
            return null;
         }
         final List<Certificate> certs = new ArrayList<Certificate>();
         for (CodeSigner signer : signers) {
            certs.addAll(signer.getSignerCertPath().getCertificates());
         }
         return certs.toArray(new Certificate[certs.size()]);
      }
      
      /** {@inheritDoc} **/
      @Override
      public CodeSigner[] getCodeSigners() {
         return virtualFile.getCodeSigners();
      }
   }
}
