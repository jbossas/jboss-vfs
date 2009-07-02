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

import java.io.File;
import java.io.IOException;
import java.io.Closeable;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A provider for temporary physical files and directories.
 */
public final class TempFileProvider implements Closeable
{
   private static final String TMP_DIR_PROPERTY = "jboss.server.temp.dir";
   private static final File TMP_ROOT;

   static {
      final String configTmpDir = System.getProperty(TMP_DIR_PROPERTY);
      try
      {
         TMP_ROOT = configTmpDir == null ? File.createTempFile("jboss-vfs-temp", "") : new File(configTmpDir, "vfs");
         TMP_ROOT.mkdirs();
      }
      catch (IOException e)
      {
         throw new RuntimeException("Can't set up temp file provider", e);
      }
   }

   /**
    * Create a temporary file provider for a given type.
    *
    * @param providerType the provider type string (used as a prefix in the temp file dir name)
    * @return the new provider
    * @throws IOException if an I/O error occurs
    */
   public static TempFileProvider create(String providerType, ScheduledExecutorService executor) throws IOException
   {
      return new TempFileProvider(File.createTempFile(providerType, "", TMP_ROOT), 0, executor);
   }

   /**
    * Create a temporary file provider for a given type and hash depth.  The hash depth is used to limit the number
    * of files in a single directory.
    *
    * @param providerType the provider type string (used as a prefix in the temp file dir name)
    * @param hashDepth the hash directory tree depth
    * @return the new provider
    * @throws IOException if an I/O error occurs
    */
   public static TempFileProvider create(String providerType, int hashDepth, ScheduledExecutorService executor) throws IOException
   {
      return new TempFileProvider(File.createTempFile(providerType, "", TMP_ROOT), hashDepth, executor);
   }

   private final File providerRoot;
   private final int hashDepth;
   private final ScheduledExecutorService executor;

   private TempFileProvider(File providerRoot, int hashDepth, ScheduledExecutorService executor)
   {
      this.providerRoot = providerRoot;
      this.executor = executor;
      if (hashDepth < 0 || hashDepth > 4) {
         throw new IllegalArgumentException("Bad hashDepth");
      }
      this.hashDepth = hashDepth;
   }

   /**
    * Create a temp file within this provider.
    *
    * @param originalName the original file name
    * @return the temporary file
    * @throws IOException if an error occurs
    */
   public File createTempFile(String originalName) throws IOException {
      return createTempFile(originalName, originalName.hashCode());
   }

   /**
    * Create a temp file within this provider, using an alternate hash code.
    *
    * @param originalName the original file name
    * @param hashCode the hash code to use
    * @return the temporary file
    * @throws IOException if an error occurs
    */
   public File createTempFile(String originalName, int hashCode) throws IOException {
      File root = providerRoot;
      for (int i = 0; i < hashDepth; i ++) {
         final int dc = hashCode & 0x7f;
         root = new File(root, dc < 16 ? "0" + Integer.toHexString(dc) : Integer.toHexString(dc));
         root.mkdir();
         hashCode >>= 7;
      }
      return File.createTempFile("", "-" + originalName, root);
   }

   /**
    * Create a temp file within this provider, using an alternate hash code, and prepopulating the file from the given
    * input stream.
    *
    * @param originalName the original file name
    * @param hashCode the hash code to use
    * @param sourceData the source input stream to use
    * @return the temporary file
    * @throws IOException if an error occurs
    */
   public File createTempFile(String originalName, int hashCode, InputStream sourceData) throws IOException {
      final File tempFile = createTempFile(originalName, hashCode);
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
    * Close this provider and delete any temp files associated with it.
    */
   public void close() throws IOException
   {
      final Runnable task = new Runnable()
      {
         public void run()
         {
            if (! recursiveDelete(providerRoot)) {
               executor.schedule(this, 30L, TimeUnit.SECONDS);
            }
         }
      };
      task.run();
   }

   private static boolean recursiveDelete(File root) {
      boolean ok = true;
      if (root.isDirectory()) {
         final File[] files = root.listFiles();
         for (File file : files)
         {
            ok &= recursiveDelete(file);
         }
         return ok && (root.delete() || ! root.exists());
      } else {
         ok &= root.delete() || ! root.exists();
      }
      return ok;
   }
}
