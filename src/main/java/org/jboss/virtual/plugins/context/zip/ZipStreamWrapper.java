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

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZipStreamWrapper - for abstracted access to in-memory zip file
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
class ZipStreamWrapper extends ZipBytesWrapper
{
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
   }

   InputStream openStream(ZipEntry ent) throws IOException
   {
      ZipInputStream zis = new ZipInputStream(getRootAsStream());

      // first find the entry
      ZipEntry entry = zis.getNextEntry();
      while(entry != null)
      {
         if(entry.getName().equals(ent.getName()))
            break;
         entry = zis.getNextEntry();
      }
      if(entry == null)
         throw new IOException("Failed to find nested jar entry: " + ent.getName() + " in zip stream: " + toString());

      // then read it
      return new SizeLimitedInputStream(zis, ent.getSize());
   }

   Enumeration<? extends ZipEntry> entries() throws IOException
   {
      return new ZipStreamEnumeration(new ZipInputStream(getRootAsStream()));
   }

   /**
    * Zip stream enumeration.
    */
   class ZipStreamEnumeration implements Enumeration<ZipEntry>
   {
      private ZipInputStream zis;

      private ZipEntry entry;

      ZipStreamEnumeration(ZipInputStream zis) throws IOException
      {
         this.zis = zis;
         entry = zis.getNextEntry();
      }

      public boolean hasMoreElements()
      {
         return entry != null;
      }

      public ZipEntry nextElement()
      {
         ZipEntry ret = entry;
         try
         {
            entry = zis.getNextEntry();
         }
         catch (IOException ex)
         {
            throw new RuntimeException("Failed to retrieve next entry from zip stream", ex);
         }

         return ret;
      }
   }
}
