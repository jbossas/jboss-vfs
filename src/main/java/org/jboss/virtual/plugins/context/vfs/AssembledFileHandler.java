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
package org.jboss.virtual.plugins.context.vfs;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * The assembled file handler.
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
@Assembled
public class AssembledFileHandler extends DelegatingHandler
{
   public AssembledFileHandler(VFSContext context, AssembledDirectoryHandler parent, String name, VirtualFileHandler delegate) throws IOException
   {
      super(context, parent, name, delegate);
      String path = getPathName();
      if (path.startsWith("/") == false)
         path = "/" + path;
      if (path.endsWith("/") == false)
         path += "/";
      setVfsUrl(new URL("vfs", context.getName(), -1, path, new AssembledUrlStreamHandler(context)));
   }

   @Override
   protected String getProtocol()
   {
      return "vfs";
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      throw new IOException("File cannot have children: " + this);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      throw new IOException("File cannot have children: " + this);
   }
}
