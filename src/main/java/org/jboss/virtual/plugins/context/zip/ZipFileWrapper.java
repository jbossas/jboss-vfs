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
package org.jboss.virtual.plugins.context.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;

/**
 * ZipFileWrapper - for abstracted access to zip files on disk
 *
 * It releases and reacquires the underlying ZipFile as necessary
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
class ZipFileWrapper extends ZipWrapper
{
   private static final Logger log = Logger.getLogger(ZipFileWrapper.class);

   private static boolean forceNoReaper;

   static
   {
      forceNoReaper = AccessController.doPrivileged(new CheckNoReaper());

      if (forceNoReaper)
         log.info("VFS forced no-reaper-mode is enabled.");

   }

   private File file;

   /** zip inflater wrapped around file */
   private ZipFile zipFile;

   /** true for extracted nested jars that we want removed when this wrapper is closed */
   private boolean autoClean;

   /** true if noReaper mode is forced on a per-instance basis */
   private boolean noReaperOverride;

   // used for debugging stream leaks
   //ConcurrentLinkedQueue<ZipEntryInputStream> streams = new ConcurrentLinkedQueue<ZipEntryInputStream>();

   ZipFileWrapper(File archive, boolean autoClean, boolean noReaperOverride)
   {
      this.noReaperOverride = noReaperOverride;
      init(archive, autoClean);
   }

   ZipFileWrapper(URI rootPathURI, boolean autoClean, boolean noReaperOverride)
   {
      this.noReaperOverride = noReaperOverride;
      File rootFile = new File(rootPathURI);
      if(!rootFile.isFile())
         throw new RuntimeException("File not found: " + rootFile);

      init(rootFile, autoClean);
   }

   /**
    * Extra initialization that didn't fit in constructors
    *
    * @param archive the archive file
    * @param autoClean auto clean flag
    */
   private void init(File archive, boolean autoClean)
   {
      file = archive;
      lastModified = file.lastModified();
      this.autoClean = autoClean;
      if (autoClean)
         file.deleteOnExit();
   }

   boolean exists()
   {
      return file.isFile();
   }

   long getLastModified()
   {
      return file.lastModified();
   }

   String getName()
   {
      return file.getName();
   }

   long getSize()
   {
      return file.length();
   }

   private ZipFile ensureZipFile() throws IOException
   {
      if (zipFile == null)
      {
         zipFile = new ZipFile(file);
         if (forceNoReaper == false && noReaperOverride == false)
            ZipFileLockReaper.getInstance().register(this);
      }

      return zipFile;
   }

   synchronized void closeZipFile() throws IOException
   {
      if (zipFile != null && getReferenceCount() <= 0)
      {
         ZipFile zf = zipFile;
         zipFile = null;
         zf.close();
         if (forceNoReaper == false && noReaperOverride == false)
            ZipFileLockReaper.getInstance().unregister(this);
      }
   }

   synchronized InputStream openStream(ZipEntry ent) throws IOException
   {
      ensureZipFile();
      InputStream is = zipFile.getInputStream(ent);
      if (is == null)
         throw new IOException("Entry no longer available: " + ent.getName() + " in file " + file);
      
      ZipEntryInputStream zis = new ZipEntryInputStream(this, is);

      // debugging code
      //streams.add(zis);

      incrementRef();
      return zis;
   }

   InputStream getRootAsStream() throws FileNotFoundException
   {
      return new FileInputStream(file);
   }

   synchronized void acquire() throws IOException
   {
      ensureZipFile();
      incrementRef();
   }

   synchronized void release() {
      super.release();
      if (forceNoReaper || noReaperOverride)
         try
         {
            closeZipFile();
         }
         catch(Exception ex)
         {
            log.warn("Failed to release file: " + file);
         }
   }

   synchronized Enumeration<? extends ZipEntry> entries() throws IOException
   {
      return ensureZipFile().entries();
   }

   void close()
   {
      try
      {
         closeZipFile();
      }
      catch(Exception ignored)
      {
         log.warn("IGNORING: Failed to release file: " + file, ignored);
      }

      if (autoClean)
         file.delete();
   }

   public String toString()
   {
      return super.toString() + " - " + file.getAbsolutePath();
   }

   private static class CheckNoReaper implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_NO_REAPER_KEY, "false");
         return Boolean.valueOf(forceString);
      }
   }
}
