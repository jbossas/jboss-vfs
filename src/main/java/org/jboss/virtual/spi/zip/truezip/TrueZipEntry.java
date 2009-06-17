/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.virtual.spi.zip.truezip;

import org.jboss.virtual.spi.zip.ZipEntry;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TrueZipEntry implements ZipEntry
{
   private de.schlichtherle.util.zip.ZipEntry entry;
   private Boolean directory;

   public TrueZipEntry(de.schlichtherle.util.zip.ZipEntry entry)
   {
      if (entry == null)
         throw new IllegalArgumentException("Null entry");
      this.entry = entry;
   }

   public String getName()
   {
      return entry.getName();
   }

   public boolean isDirectory()
   {
      return directory != null ? directory : entry.isDirectory();
   }

   public void setDirectory(boolean directory)
   {
      this.directory = directory;
   }

   public long getTime()
   {
      return entry.getTime();
   }

   public void setTime(long time)
   {
      entry.setTime(time);
   }

   public long getSize()
   {
      return entry.getSize();
   }

   public void setSize(long size)
   {
      entry.setSize(size);
   }

   public String getComment()
   {
      return entry.getComment();
   }

   public void setComment(String comment)
   {
      entry.setComment(comment);
   }

   public long getCrc()
   {
      return entry.getCrc() & 0xffffffffL;
   }

   public void setCrc(long crc)
   {
      entry.setCrc(crc);
   }

   public Object unwrap()
   {
      return entry;
   }
}