/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.virtual.plugins.context.jzip;

import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.jzipfile.ZipEntry;
import org.jboss.jzipfile.Zip;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.List;

public final class ZipFileHandler extends AbstractVirtualFileHandler
{
   private final File zipFile;
   private final ZipEntry zipEntry;

   public ZipFileHandler(VFSContext context, VirtualFileHandler parent, String name)
   {
      super(context, parent, name);
   }

   public URI toURI() throws URISyntaxException
   {
      return null;
   }

   public long getLastModified() throws IOException
   {
      return zipEntry.getModificationTime();
   }

   public long getSize() throws IOException
   {
      return zipEntry.getSize();
   }

   public boolean exists() throws IOException
   {
      return true;
   }

   public boolean isLeaf() throws IOException
   {
      return true;
   }

   public boolean isHidden() throws IOException
   {
      return false;
   }

   public InputStream openStream() throws IOException
   {
      return Zip.openEntry(zipFile, zipEntry);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return null;
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return null;
   }

   public boolean removeChild(String name) throws IOException
   {
      return false;
   }

   public boolean isNested() throws IOException
   {
      return false;
   }
}
