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
package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.jboss.util.file.ArchiveBrowser;
import org.jboss.util.file.ArchiveBrowserFactory;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;

/**
 * This is a bridge to an older, crappier API written by myself.
 *
 * @deprecated
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class VfsArchiveBrowserFactory implements ArchiveBrowserFactory
{
   public Iterator create(URL url, ArchiveBrowser.Filter filter)
   {
      try
      {
         VirtualFileURLConnection conn = (VirtualFileURLConnection)url.openConnection();
         VirtualFile vf = conn.getVirtualFile();
         return new VfsArchiveBrowser(filter, vf);
      }
      catch (IOException e)
      {               
         throw new RuntimeException("Unable to browse URL: " + url, e);
      }
   }
}
