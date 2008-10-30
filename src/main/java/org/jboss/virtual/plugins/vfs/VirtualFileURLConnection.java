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
package org.jboss.virtual.plugins.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.VFSCacheFactory;

/**
 * Implements basic URLConnection for a VirtualFile
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VirtualFileURLConnection extends URLConnection
{
   protected VirtualFile file;
   protected URL vfsurl;
   protected String relativePath;

   public VirtualFileURLConnection(URL url, URL vfsurl, String relativePath)
   {
      super(url);
      this.vfsurl = vfsurl;
      this.relativePath = relativePath;
   }

   public VirtualFileURLConnection(URL url, VirtualFile file)
   {
      super(url);
      this.file = file;
   }

   public void connect() throws IOException
   {
   }

   public VirtualFile getContent() throws IOException
   {
      return getVirtualFile();
   }

   public int getContentLength()
   {
      try
      {
         return (int)getVirtualFile().getSize();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public long getLastModified()
   {
      try
      {
         return getVirtualFile().getLastModified();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public InputStream getInputStream() throws IOException
   {
      return getVirtualFile().openStream();
   }

   @SuppressWarnings("deprecation")
   protected static VirtualFile resolveCachedVirtualFile(URL vfsurl, String relativePath) throws IOException
   {
      return resolveVirtualFile(vfsurl, relativePath);
   }

   @SuppressWarnings("deprecation")
   protected static VirtualFile resolveVirtualFile(URL vfsurl, String relativePath) throws IOException
   {
      VFSCache cache = VFSCacheFactory.getInstance();
      VirtualFile file = cache.getFile(vfsurl);
      return file.findChild(relativePath);
   }

   /**
    * Get the virtual file.
    *
    * @return the underlying virtual file
    * @throws IOException for any error
    */
   protected synchronized VirtualFile getVirtualFile() throws IOException
   {
      if (file == null)
         file = resolveVirtualFile(vfsurl, relativePath);
      
      return file;
   }
}
