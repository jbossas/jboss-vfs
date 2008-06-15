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

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZipStreamWrapper - for abstracted access to in-memory zip file
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
class ZipStreamWrapper extends ZipBytesWrapper
{
   /** Logger */
   private static final Logger log = Logger.getLogger(ZipStreamWrapper.class);

   /** Is optimizeForMemory turned on */
   private static boolean optimizeForMemory;

   static
   {
      optimizeForMemory = AccessController.doPrivileged(new CheckOptimizeForMemory());

      if (optimizeForMemory)
         log.info("VFS optimizeForMemory is enabled.");
   }

   /** zip archive - as individual inflated in-memory files */
   private Map<String, InMemoryFile> inMemoryFiles = new LinkedHashMap<String, InMemoryFile>();

   /** size of the zip file composed back from inMemoryFiles */
   private int size;

   /**
    * ZipStreamWrapper is not aware of actual zip source so it can not detect
    * if it's been modified, like ZipFileWrapper does.
    *
    * @param zipStream the current zip stream
    * @param name the name
    * @param lastModified passed by zip stream provider - constant value
    * @throws IOException for any error
    */
   ZipStreamWrapper(InputStream zipStream, String name, long lastModified) throws IOException
   {
      super(zipStream, name, lastModified);

      ZipInputStream zis = new ZipInputStream(super.getRootAsStream());
      ZipEntry ent = zis.getNextEntry();
      while (ent != null)
      {
         byte [] fileBytes;
         if (ent.isDirectory() == false)
         {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copyStream(zis, baos);
            fileBytes = baos.toByteArray();
            ent.setSize(fileBytes.length);
         }
         else
         {
            fileBytes = new byte[0];
         }

         inMemoryFiles.put(ent.getName(), new InMemoryFile(ent, fileBytes));
         ent = zis.getNextEntry();
      }

      if (optimizeForMemory) {
         initZipSize();

         // we don't need memory buffer any more
         super.close();
      }
   }

   InputStream openStream(ZipEntry ent) throws IOException
   {
      InMemoryFile memFile = inMemoryFiles.get(ent.getName());

      if (memFile == null)
         throw new FileNotFoundException("Failed to find nested jar entry: " + ent.getName() + " in zip stream: " + toString());

      return new ByteArrayInputStream(memFile.fileBytes);
   }

   Enumeration<? extends ZipEntry> entries() throws IOException
   {
      return new ZipStreamEnumeration();
   }

   InputStream getRootAsStream() throws FileNotFoundException
   {
      if (optimizeForMemory)
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         try
         {
            recomposeZip(baos);
            return new ByteArrayInputStream(baos.toByteArray());
         }
         catch (IOException ex)
         {
            FileNotFoundException e = new FileNotFoundException("Failed to recompose inflated nested archive " + getName());
            e.initCause(ex);
            throw e;
         }
      }
      else
      {
         return super.getRootAsStream();
      }
   }

   long getSize()
   {
      if (optimizeForMemory)
         return size;
      else
         return super.getSize();
   }

   void close() {
      inMemoryFiles = null;
      super.close();
   }

   private void initZipSize() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      recomposeZip(baos);
      this.size = baos.size();
   }

   private void recomposeZip(ByteArrayOutputStream baos) throws IOException
   {
      ZipOutputStream zout = new ZipOutputStream(baos);
      zout.setMethod(ZipOutputStream.STORED);

      Iterator<InMemoryFile> it = inMemoryFiles.values().iterator();
      while(it.hasNext())
      {
         InMemoryFile memFile = it.next();
         zout.putNextEntry(memFile.entry);
         zout.write(memFile.fileBytes);
      }
      zout.close();
   }

   private static void copyStream(InputStream is, OutputStream os) throws IOException
   {
      byte [] buff = new byte[4096];
      int rc = is.read(buff);
      while (rc != -1)
      {
         os.write(buff, 0, rc);
         rc = is.read(buff);
      }
   }

   static class InMemoryFile
   {
      ZipEntry entry;
      byte [] fileBytes;

      public InMemoryFile(ZipEntry entry, byte[] fileBytes)
      {
         this.entry = entry;
         this.fileBytes = fileBytes;
      }
   }

   class ZipStreamEnumeration implements Enumeration<ZipEntry>
   {
      private Iterator<InMemoryFile> it;

      ZipStreamEnumeration()
      {
         it = inMemoryFiles.values().iterator();
      }

      public boolean hasMoreElements()
      {
         return it.hasNext();
      }

      public ZipEntry nextElement()
      {
         return it.next().entry;
      }
   }

   private static class CheckOptimizeForMemory implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.OPTIMIZE_FOR_MEMORY_KEY, "false");
         return Boolean.valueOf(forceString);
      }
   }
}
