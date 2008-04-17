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
package org.jboss.virtual;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;
import org.jboss.virtual.spi.VFSContext;

/**
 * Memory VFS API.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class MemoryFileFactory
{
   private static final MemoryContextFactory factory = MemoryContextFactory.getInstance();

   /**
    * Find host's VFS.
    *
    * @param host the host
    * @return host's vfs
    */
   public static VFS find(String host)
   {
      VFSContext context = factory.find(host);
      return context != null ? context.getVFS() : null;
   }

   /**
    * Create the memory root.
    *
    * @param uri the uri
    * @return root's vfs
    * @throws IOException for any error
    */
   public static VFS createRoot(URI uri) throws IOException
   {
      return createRoot(uri.toURL());
   }

   /**
    * Create root vfs.
    *
    * @param url the url
    * @return root's vfs
    */
   public static VFS createRoot(URL url)
   {
      return factory.createRoot(url).getVFS();
   }

   /**
    * Create memory directory.
    *
    * @param url the url
    * @return vfs directory
    */
   public static VirtualFile createDirectory(URL url)
   {
      return factory.createDirectory(url);
   }

   /**
    * Put file.
    *
    * @param url the url
    * @param contents the contents
    * @return vfs file
    */
   public static VirtualFile putFile(URL url, byte[] contents)
   {
      return factory.putFile(url, contents);
   }

   /**
    * Delete root.
    *
    * @param url the url
    * @return true if deleted
    */
   public static boolean deleteRoot(URL url)
   {
      return factory.deleteRoot(url);
   }

   /**
    * Delete.
    *
    * @param url the url
    * @return true if deleted
    */
   public static boolean delete(URL url)
   {
      return factory.delete(url);
   }
}
