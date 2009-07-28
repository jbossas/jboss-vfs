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

package org.jboss.virtual;

import java.io.Closeable;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A temporary directory which exists until it is closed, at which time its contents will be removed.
 */
public final class TempDir implements Closeable
{
   private final TempFileProvider provider;
   private final File root;
   private final AtomicBoolean open = new AtomicBoolean(true);

   TempDir(TempFileProvider provider, File root)
   {
      this.provider = provider;
      this.root = root;
   }

   /**
    * Get the {@code File} that represents the root of this temporary directory.  The returned file is only valid
    * as long as the tempdir exists.
    *
    * @return the root file
    * @throws IOException if the directory was closed at the time of this invocation
    */
   public File getRoot() throws IOException
   {
      if (! open.get()) {
         throw new IOException("Temp directory closed");
      }
      return root;
   }

   /**
    * Get the {@code File} for a relative path.  The returned file is only valid
    * as long as the tempdir exists.
    *
    * @param relativePath the relative path
    * @return the corresponding file
    * @throws IOException if the directory was closed at the time of this invocation
    */
   public File getFile(String relativePath) throws IOException {
      if (! open.get()) {
         throw new IOException("Temp directory closed");
      }
      return new File(root, relativePath);
   }

   /**
    * Create a file within this temporary directory, prepopulating the file from the given
    * input stream.
    *
    * @param relativePath the relative path name
    * @param sourceData the source input stream to use
    * @return the file
    * @throws IOException if the directory was closed at the time of this invocation or an error occurs
    */
   public File createFile(String relativePath, InputStream sourceData) throws IOException {
      final File tempFile = getFile(relativePath);
      boolean ok = false;
      try {
         final FileOutputStream fos = new FileOutputStream(tempFile);
         try {
            VFSUtils.copyStream(sourceData, fos);
            fos.close();
            sourceData.close();
            return tempFile;
         } finally {
            VFSUtils.safeClose(fos);
         }
      } finally {
         VFSUtils.safeClose(sourceData);
         if (! ok) {
            tempFile.delete();
         }
      }
   }

   /**
    * Close this directory.  The contents of the directory will be removed.
    *
    * @throws IOException if an I/O error occurs
    */
   public void close() throws IOException
   {
      if (open.getAndSet(false)) {
         provider.new DeleteTask(root).run();
      }
   }

   protected void finalize() throws Throwable
   {
      VFSUtils.safeClose(this);
   }
}
