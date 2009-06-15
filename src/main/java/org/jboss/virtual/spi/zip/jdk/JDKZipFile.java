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
package org.jboss.virtual.spi.zip.jdk;

import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.jboss.virtual.spi.zip.ZipEntry;
import org.jboss.virtual.spi.zip.ZipFile;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class JDKZipFile implements ZipFile
{
   private java.util.zip.ZipFile file;

   public JDKZipFile(java.util.zip.ZipFile file)
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      this.file = file;
   }

   public InputStream getInputStream(ZipEntry entry) throws IOException
   {
      Object unwrap = entry.unwrap();
      InputStream delegate = file.getInputStream(java.util.zip.ZipEntry.class.cast(unwrap));
      return new IgnoreCloseInputStream(delegate);
   }

   public void close() throws IOException
   {
      file.close();
   }

   public Enumeration<? extends ZipEntry> entries()
   {
      final Enumeration<? extends java.util.zip.ZipEntry> entries = file.entries();
      return new Enumeration<ZipEntry>()
      {
         public boolean hasMoreElements()
         {
            return entries.hasMoreElements();
         }

         public ZipEntry nextElement()
         {
            java.util.zip.ZipEntry entry = entries.nextElement();
            return entry != null ? new JDKZipEntry(entry) : null;
         }
      };
   }
}