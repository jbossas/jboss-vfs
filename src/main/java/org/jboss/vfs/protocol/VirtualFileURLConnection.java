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
package org.jboss.vfs.protocol;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;

/**
 * Implements basic URLConnection for a VirtualFile
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
class VirtualFileURLConnection extends AbstractURLConnection
{
   private final VirtualFile file;

   public VirtualFileURLConnection(URL url) throws IOException
   {
      super(url);
      file = VFS.getChild(toURI(url));
   }

   public void connect() throws IOException
   {
   }

   public VirtualFile getContent() throws IOException
   {
      return file;
   }

   public int getContentLength()
   {
      final long size = file.getSize();
      return size > (long) Integer.MAX_VALUE ? -1 : (int) size;
   }

   public long getLastModified()
   {
      return file.getLastModified();
   }

   public InputStream getInputStream() throws IOException
   {
      return file.openStream();
   }

   public Permission getPermission() throws IOException
   {
      String decodedPath = toURI(url).getPath();
      if (File.separatorChar != '/')
         decodedPath = decodedPath.replace('/', File.separatorChar);

      return new FilePermission(decodedPath, "read");
   }

   @Override
   protected String getName() {
      return file.getName();
   }
}
