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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.jboss.virtual.spi.zip.ZipEntry;
import org.jboss.virtual.spi.zip.ZipFile;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TrueZipFile implements ZipFile
{
   private de.schlichtherle.util.zip.ZipFile file;

   public TrueZipFile(de.schlichtherle.util.zip.ZipFile file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      this.file = file;
   }

   public InputStream getInputStream(ZipEntry entry) throws IOException
   {
      Object unwraped = entry.unwrap();
      return file.getInputStream(de.schlichtherle.util.zip.ZipEntry.class.cast(unwraped));
   }

   public void close() throws IOException
   {
      file.close();
   }

   public Enumeration<? extends ZipEntry> entries()
   {
      @SuppressWarnings("unchecked")
      final Enumeration<? extends de.schlichtherle.util.zip.ZipEntry> entries = file.entries();
      return new Enumeration<ZipEntry>()
      {
         public boolean hasMoreElements()
         {
            return entries.hasMoreElements();
         }

         public ZipEntry nextElement()
         {
            de.schlichtherle.util.zip.ZipEntry entry = entries.nextElement();
            return entry != null ? new TrueZipEntry(entry) : null;
         }
      };
   }
}