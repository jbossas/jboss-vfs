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

import java.util.zip.ZipInputStream;

import org.jboss.virtual.spi.zip.jdk.JDKZipEntryProvider;
import org.jboss.virtual.spi.zip.ZipEntry;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TrueZipEntryProvider extends JDKZipEntryProvider
{
   public TrueZipEntryProvider(ZipInputStream zis)
   {
      super(zis);
   }

   @Override
   protected ZipEntry wrap(java.util.zip.ZipEntry entry)
   {
      de.schlichtherle.util.zip.ZipEntry zipEntry = new de.schlichtherle.util.zip.ZipEntry(entry.getName());
      zipEntry.setComment(entry.getComment());
      zipEntry.setCompressedSize(entry.getCompressedSize());
      zipEntry.setCrc(entry.getCrc());
      zipEntry.setExtra(entry.getExtra());
      zipEntry.setMethod(entry.getMethod());
      zipEntry.setSize(entry.getSize());
      zipEntry.setTime(entry.getTime());

      TrueZipEntry trueZipEntry = new TrueZipEntry(zipEntry);
      trueZipEntry.setDirectory(entry.isDirectory());
      return trueZipEntry;
   }
}