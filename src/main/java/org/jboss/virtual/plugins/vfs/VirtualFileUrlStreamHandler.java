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

import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.VirtualFile;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.IOException;

/**
 * Used when creating VFS urls so we don't have to go through the handlers all the time
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class VirtualFileUrlStreamHandler extends URLStreamHandler
{
   private final VFSContext context;


   public VirtualFileUrlStreamHandler(VirtualFileHandler handler)
   {
      this.context = handler.getVFSContext();
   }

   protected URLConnection openConnection(URL u) throws IOException
   {
      String baseRootUrl = null;
      try
      {
         baseRootUrl = context.getRoot().toVfsUrl().toString();
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      String urlString = u.toString();
      int idx = urlString.indexOf(baseRootUrl);
      if (idx == -1) throw new IOException(u.toString() + " does not belong to the same VFS context as " + baseRootUrl);
      String path = urlString.substring(baseRootUrl.length());
      VirtualFileHandler vf = context.getRoot().findChild(path);
      if (vf == null) throw new IOException(path + " was not found in VFS context " + baseRootUrl);
      return new VirtualFileURLConnection(u, vf.getVirtualFile());
   }
}
