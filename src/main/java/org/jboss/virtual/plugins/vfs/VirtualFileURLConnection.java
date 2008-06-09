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
import java.util.Collections;
import java.util.Map;

import org.jboss.util.collection.SoftValueHashMap;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Implements basic URLConnection for a VirtualFile
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class VirtualFileURLConnection extends URLConnection
{
   @SuppressWarnings("unchecked")
   public static Map<URL, VFS> urlCache = Collections.<URL, VFS>synchronizedMap(new SoftValueHashMap());

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

   public Object getContent() throws IOException
   {
      return getVirtualFile();
   }

   @SuppressWarnings("deprecation")
   protected static VirtualFile resolveCachedVirtualFile(URL vfsurl, String relativePath) throws IOException
   {
      VFS vfs = urlCache.get(vfsurl);
      if (vfs == null)
      {
         vfs = VFS.getVFS(vfsurl);
         urlCache.put(vfsurl, vfs);
      }
      else
      {
         // if the root of VFS has changed on disk, lets purge it
         // this is important for Jar files as we don't want stale jars as the
         // root of the VFS (i.e., on redeployment)
         if (vfs.getRoot().hasBeenModified())
         {
            vfs = VFS.getVFS(vfsurl);
            urlCache.put(vfsurl, vfs);
         }
      }
      return vfs.findChild(relativePath);
   }

   @SuppressWarnings("deprecation")
   protected static VirtualFile resolveVirtualFile(URL vfsurl, String relativePath) throws IOException
   {
      VFS vfs = VFS.getVFS(vfsurl);
      return vfs.findChild(relativePath);
   }

   protected synchronized VirtualFile getVirtualFile() throws IOException
   {
      if (file == null)
      {
         if (getUseCaches())
         {
            file = resolveCachedVirtualFile(vfsurl, relativePath);
         }
         else
         {
            file = resolveVirtualFile(vfsurl, relativePath);
         }
      }
      return file;
   }

   public InputStream getInputStream() throws IOException
   {
      return getVirtualFile().openStream();
   }
}
