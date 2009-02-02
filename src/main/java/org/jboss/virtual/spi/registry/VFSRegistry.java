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
import java.net.URL;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;

/**
 * VFS registry.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class VFSRegistry
{
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
   public abstract VirtualFile getFile(URL url) throws IOException;
}
