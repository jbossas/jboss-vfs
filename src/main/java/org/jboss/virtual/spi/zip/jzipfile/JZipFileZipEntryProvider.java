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

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jzipfile.Zip;
import org.jboss.jzipfile.ZipCatalog;
import org.jboss.virtual.spi.zip.ZipEntry;
import org.jboss.virtual.spi.zip.ZipEntryProvider;
import org.jboss.virtual.VFSUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JZipFileZipEntryProvider implements ZipEntryProvider
{
   private InputStream copy;
   private List<org.jboss.jzipfile.ZipEntry> entries;
   private int index;
   private int size;

   public JZipFileZipEntryProvider(InputStream is) throws IOException
   {
      if (is == null)
         throw new IllegalArgumentException("Null input stream");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      VFSUtils.copyStreamAndClose(is, baos);
      copy = new ByteArrayInputStream(baos.toByteArray());

      ZipCatalog catalog = Zip.readCatalog(is);
      entries = new ArrayList<org.jboss.jzipfile.ZipEntry>(catalog.allEntries());
      size = entries.size();
   }

   public ZipEntry getNextEntry() throws IOException
   {
      if (index >= size)
         return null;

      org.jboss.jzipfile.ZipEntry entry = entries.get(index++);
      return new JZipFileZipEntry(entry);
   }

   public InputStream currentStream() throws IOException
   {
      return Zip.openEntry(copy, entries.get(index));
   }
}