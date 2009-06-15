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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.jboss.logging.Logger;
import org.jboss.virtual.spi.zip.ZipEntry;

/**
 * ZipWrapper represents abstracted access to zip archive
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
abstract class ZipWrapper
{
   /** the log */
   protected static final Logger log = Logger.getLogger(ZipWrapper.class);

   /** last known modifyTime of the zip source */
   protected long lastModified;

   /** timestamp of last call to getLastModified()
    * it's an expensive call - we don't do it more then once every second */
   private long lastChecked;

   /** last known activity */
   private long lastUsed;

   /** number of streams currently open on this wrapper */
   private int refCount = 0;

   /**
    * Returns true if underlying source's lastModified time has changed since previous call.
    *
    * @return true if modified, false othwewise
    */
   boolean hasBeenModified()
   {
      long now = System.currentTimeMillis();
      if (now - lastChecked < 1000)
         return false;

      lastChecked = now;
      long lm = getLastModified();
      if (lm != lastModified)
      {
         lastModified = lm;
         return true;
      }

      return false;
   }

   /**
    * get lastUsed timestamp
    *
    * @return
    */
   long getLastUsed()
   {
      return lastUsed;
   }

   /**
    * Returns the number of streams currently open
    *
    * @return
    */
   int getReferenceCount()
   {
      return refCount;
   }

   /**
    * Increment usage count by one
    */
   void incrementRef()
   {
      refCount++;
      lastUsed = System.currentTimeMillis();
   }

   /**
    * Decrement usage count by one
    */
   synchronized void release()
   {
      refCount--;
      if (refCount <= 0)
      {
         lastUsed = System.currentTimeMillis();
      }
   }

   /**
    * Acquire lock.
    *
    * @throws IOException for any error
    */
   abstract void acquire() throws IOException;

   /**
    * Get lastModified of this archive
    *
    * @return lastModified timestamp
    */
   abstract long getLastModified();

   /**
    * Get the name of this archive
    *
    * @return name
    */
   abstract String getName();

   /**
    * Check if archive exists
    *
    * @return true if archive exists
    */
   abstract boolean exists();

   /**
    * Get the size of the archive
    *
    * @return size in bytes
    */
   abstract long getSize();

   /**
    * Enumerate contents of this archive
    *
    * @return enumeration of ZipEntries
    * @throws IOException for any error
    */
   abstract Enumeration<? extends ZipEntry> entries() throws IOException;

   /**
    * Get the contents of a given entry as stream
    *
    * @param ent zip entry
    * @return InputStream with entry's contents
    * @throws IOException for any error
    */
   abstract InputStream openStream(ZipEntry ent) throws IOException;

   /**
    * Get raw bytes of this archive in its compressed form
    *
    * @return InputStream containing raw archive
    * @throws FileNotFoundException if archive doesn't exist
    */
   abstract InputStream getRootAsStream() throws FileNotFoundException;

   /**
    * Close this archive
    */
   abstract void close();

   /**
    * Delete this archive
    *
    * @param gracePeriod maximum time to wait for any locks
    * @return true if file was deleted, false otherwise
    * @throws IOException for any error
    */
   abstract boolean delete(int gracePeriod) throws IOException;

   /**
    * Recompose an input stream as having <em>path</em> entry for its root.
    * All entries' names have to be shifted accordingly.
    *
    * @param path root path
    * @return InputStream containing a new ZIP archive
    * @throws FileNotFoundException for any error that prevented recomposition
    */
   protected InputStream recomposeZipAsInputStream(String path) throws FileNotFoundException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try
      {
         recomposeZip(baos, path);
         return new ByteArrayInputStream(baos.toByteArray());
      }
      catch (IOException ex)
      {
         FileNotFoundException e = new FileNotFoundException("Failed to recompose inflated nested archive " + getName());
         e.initCause(ex);
         throw e;
      }
   }

   /**
    * Recompose an input streamas having <em>path</em> entry for its root, and write it to a given OutputStream.
    * All entries' names have to be shifted accordingly.
    *
    * @param os OutputStream that will contain newly create archive
    * @param path root path
    * @throws IOException for any error
    */
   protected void recomposeZip(OutputStream os, String path) throws IOException
   {
      // do nothing
   }
}
