/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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
import org.jboss.vfs.spi.RootFileSystem;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;

/**
 * Implementation URLConnection that will delegate to the VFS RootFileSystem.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 * @version $Revision$
 */
public class FileURLConnection extends AbstractURLConnection {

   private final RootFileSystem rootFileSystem = RootFileSystem.ROOT_INSTANCE;

   private final VirtualFile mountPoint = VFS.getRootVirtualFile();
    
   private final VirtualFile file;

   public FileURLConnection(URL url) throws IOException
   {
      super(url);
      file = VFS.getChild(toURI(url));
   }

   public File getContent() throws IOException
   {
      return rootFileSystem.getFile(mountPoint, file);
   }

   public int getContentLength()
   {
      final long size = rootFileSystem.getSize(mountPoint, file);
      return size > (long) Integer.MAX_VALUE ? -1 : (int) size;
   }

   public long getLastModified()
   {
      return rootFileSystem.getLastModified(mountPoint, file);
   }

   public InputStream getInputStream() throws IOException
   {
      return rootFileSystem.openInputStream(mountPoint, file);
   }

   @Override
   public Permission getPermission() throws IOException {
      return new FilePermission(file.getPathName(), "read");
   }

   public void connect() throws IOException {
   }

   @Override
   protected String getName() {
      return file.getName();
   }
}
