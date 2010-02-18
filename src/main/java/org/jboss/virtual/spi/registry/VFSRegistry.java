/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.virtual.spi.registry;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;

/**
 * VFS registry.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class VFSRegistry
{
   /**
    * Get an instance of vfs registry.
    *
    * @return the vfs registry instance
    */
   public static VFSRegistry getInstance()
   {
      return VFSRegistryBuilder.getInstance();
   }

   /**
    * Add new vfs context.
    *
    * @param context the context
    */
   public abstract void addContext(VFSContext context);

   /**
    * Remove the context.
    *
    * @param context the context
    */
   public abstract void removeContext(VFSContext context);

   /**
    * Get the context.
    *
    * @param uri the uri to match
    * @return matching context or null
    * @throws IOException for any IO error
    */
   public abstract VFSContext getContext(URI uri) throws IOException;

   /**
    * Get the context.
    *
    * @param url the url to match
    * @return matching context or null
    * @throws IOException for any IO error
    */
   public VFSContext getContext(URL url) throws IOException
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");

      try
      {
         return getContext(VFSUtils.toURI(url));
      }
      catch (URISyntaxException e)
      {
         IOException ioe = new IOException();
         ioe.initCause(e);
         throw ioe;
      }
   }

   /**
    * Get the file.
    * Check the cache for cached entry,
    * return null if no matching entry exists.
    *
    * @param uri the file's uri
    * @return virtual file instance or null if it doesn't exist in cache
    * @throws IOException for any error
    */
   public abstract VirtualFile getFile(URI uri) throws IOException;

   /**
    * Get the file.
    * Check the cache for cached entry,
    * return null if no matching entry exists.
    *
    * @param url the file's url
    * @return virtual file instance or null if it doesn't exist in cache
    * @throws IOException for any error
    */
   public VirtualFile getFile(URL url) throws IOException
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");

      try
      {
         return getFile(VFSUtils.toURI(url));
      }
      catch (URISyntaxException e)
      {
         IOException ioe = new IOException();
         ioe.initCause(e);
         throw ioe;
      }
   }
}
