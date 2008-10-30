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
package org.jboss.virtual.spi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * Simple vfs cache factory.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class VFSCacheFactory
{
   private static final Object lock = new Object();
   private static Logger log = Logger.getLogger(VFSCacheFactory.class);

   private static VFSCache instance;

   private VFSCacheFactory()
   {
   }

   /**
    * Get VFS cache instance.
    *
    * @return the vfs cache instance
    */
   public static VFSCache getInstance()
   {
      if (instance == null)
      {
         synchronized (lock)
         {
            instance = AccessController.doPrivileged(new VFSCacheCreatorAction());
         }
      }

      return instance;
   }

   /**
    * Set instance.
    *
    * This should be used with care.
    * Better to leave it to getInstance method creation.
    *
    * @param cache cache instance to set
    */
   public static void setInstance(VFSCache cache)
   {
      if (cache != null && instance != null && instance instanceof NoopVFSCache == false)
         throw new IllegalArgumentException("Instance already set!");

      instance = cache;
   }

   private static class VFSCacheCreatorAction implements PrivilegedAction<VFSCache>
   {
      public VFSCache run()
      {
         try
         {
            String className = System.getProperty(VFSUtils.VFS_CACHE_KEY);
            if (className != null)
            {
               ClassLoader cl = VFSCacheFactory.class.getClassLoader();
               Class<?> clazz = cl.loadClass(className);
               VFSCache cache = VFSCache.class.cast(clazz.newInstance());
               cache.start(); // start here, so we fall back to default no-op in case start fails
               return cache;
            }
         }
         catch (Throwable t)
         {
            log.warn("Exception instantiating VFS cache: " + t);
         }
         return new NoopVFSCache();
      }
   }

   /**
    * Noop cache.
    * Doesn't do any caching.
    */
   private static class NoopVFSCache implements VFSCache
   {
      public VirtualFile getFile(URI uri) throws IOException
      {
         return VFS.getRoot(uri);
      }

      public VirtualFile getFile(URL url) throws IOException
      {
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

      public void putContext(VFSContext context)
      {
      }

      public void removeContext(VFSContext context)
      {
      }

      public void start() throws Exception
      {
      }

      public void stop()
      {
      }
   }
}