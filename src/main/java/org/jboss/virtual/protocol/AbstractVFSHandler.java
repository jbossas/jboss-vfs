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
package org.jboss.virtual.protocol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;
import org.jboss.virtual.spi.registry.VFSRegistry;

/**
 * VFS's file URL handler.
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractVFSHandler extends URLStreamHandler
{
   private static Map<Class, Integer> lengths = new WeakHashMap<Class, Integer>();

   /**
    * Get protocol name length.
    * e.g. vfsfile - 7, vfszip - 6, ...
    *
    * @return protocol name lenght
    */
   protected int getProtocolNameLength()
   {
      Class<?> clazz = getClass();
      Integer length = lengths.get(clazz);
      if (length == null)
      {
         Package pck = clazz.getPackage();
         String pckName = pck.getName();
         int p = pckName.lastIndexOf('.');
         length = pckName.substring(p + 1).length();
         lengths.put(clazz, length);
      }
      return length;
   }

   protected URLConnection openConnection(URL url) throws IOException
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile vf = registry.getFile(url);
      if (vf != null)
         return new VirtualFileURLConnection(url, vf);

      String file = URLDecoder.decode(url.toExternalForm(), "UTF-8").substring(getProtocolNameLength() + 1); // strip out vfs protocol + :
      URL vfsurl = null;
      String relative;
      String queryStr = url.getQuery();
      if (queryStr != null)
         file = file.substring(0, file.lastIndexOf('?'));

      File fp = new File(file);
      if (fp.exists())
      {
         vfsurl = fp.getParentFile().toURL();
         relative = fp.getName();
      }
      else
      {
         File curr = fp;
         relative = fp.getName();
         while ((curr = curr.getParentFile()) != null)
         {
            if (curr.exists())
            {
               vfsurl = curr.toURL();
               break;
            }
            else
            {
               relative = curr.getName() + "/" + relative;
            }
         }
      }

      if (vfsurl == null)
         throw new IOException("VFS file does not exist: " + url);
      if (queryStr != null)
         vfsurl = new URL(vfsurl + "?" + queryStr);
      
      return new VirtualFileURLConnection(url, vfsurl, relative);
   }
}