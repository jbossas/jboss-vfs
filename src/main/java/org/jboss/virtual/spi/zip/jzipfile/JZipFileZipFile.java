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
package org.jboss.virtual.spi.zip.jzipfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.jboss.jzipfile.Zip;
import org.jboss.jzipfile.ZipCatalog;
import org.jboss.virtual.spi.zip.ZipEntry;
import org.jboss.virtual.spi.zip.ZipFile;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JZipFileZipFile implements ZipFile
{
   private File file;
   private ZipCatalog catalog;

   public JZipFileZipFile(File file) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      this.file = file;
      catalog = Zip.readCatalog(file);
   }

   public InputStream getInputStream(ZipEntry entry) throws IOException
   {
      Object unwrap = entry.unwrap();
      return Zip.openEntry(file, org.jboss.jzipfile.ZipEntry.class.cast(unwrap));
   }

   public void close() throws IOException
   {
   }

   public Enumeration<? extends ZipEntry> entries()
   {
      Collection<org.jboss.jzipfile.ZipEntry> entries = catalog.allEntries();
      final Iterator<org.jboss.jzipfile.ZipEntry> iterator = entries.iterator();
      return new Enumeration<ZipEntry>()
      {
         public boolean hasMoreElements()
         {
            return iterator.hasNext();
         }

         public ZipEntry nextElement()
         {
            org.jboss.jzipfile.ZipEntry entry = iterator.next();
            return entry != null ? new JZipFileZipEntry(entry) : null;
         }
      };
   }
}