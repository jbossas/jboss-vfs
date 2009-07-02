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

package org.jboss.virtual.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collections;

public final class RealFileSystem implements FileSystem
{
   public static final RealFileSystem ROOT_INSTANCE = new RealFileSystem(Collections.<String>emptyList());

   private final String base;

   private RealFileSystem(List<String> baseComponents)
   {
      base = implode("", baseComponents) + File.separator;
   }

   public File getFile(List<String> pathComponents) throws IOException
   {
      return new File(implode(base, pathComponents));
   }

   private static String implode(String base, List<String> pathComponents)
   {
      int l = 0;
      for (String s : pathComponents)
      {
         l += s.length() + 1;
      }
      if (l == 0) {
         return base;
      }
      final StringBuilder builder = new StringBuilder(l + base.length());
      builder.append(base);
      for (String s : pathComponents)
      {
         builder.append(File.separatorChar);
         builder.append(s);
      }
      return builder.toString();
   }

   public InputStream openInputStream(List<String> pathComponents) throws IOException
   {
      return new FileInputStream(getFile(pathComponents));
   }

   public boolean isReadOnly()
   {
      return false;
   }

   public boolean delete(List<String> pathComponents) throws IOException
   {
      return getFile(pathComponents).delete();
   }

   public long getSize(List<String> pathComponents) throws IOException
   {
      return getFile(pathComponents).length();
   }

   public long getLastModified(List<String> pathComponents) throws IOException
   {
      return getFile(pathComponents).lastModified();
   }

   public boolean exists(List<String> pathComponents) throws IOException
   {
      return getFile(pathComponents).exists();
   }

   public boolean isDirectory(List<String> pathComponents) throws IOException
   {
      return getFile(pathComponents).isDirectory();
   }

   public Iterator<String> getDirectoryEntries(List<String> directoryPathComponents) throws IOException
   {
      return Arrays.asList(getFile(directoryPathComponents).list()).iterator();
   }

   public void close() throws IOException
   {
      // no operation - the real FS can't be closed
   }
}
