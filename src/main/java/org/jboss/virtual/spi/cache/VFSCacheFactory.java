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
package org.jboss.virtual.spi.cache;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.spi.cache.helpers.NoopVFSCache;

/**
 * Simple vfs cache factory.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 80615 $
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
      return getInstance(null);
   }
   /**
    * 
    * Get VFS cache instance.
    *
    * @param defaultCacheImpl - the possibly null name of the VFSCache
    * implementation to use. If null, the {@linkplain VFSUtils.VFS_CACHE_KEY}
    * system property will be used.
    * 
    * @return the vfs cache instance
    */
   public static VFSCache getInstance(String defaultCacheImpl)
   {
      if (instance == null)
      {
         synchronized (lock)
         {
            if (instance == null)
               instance = AccessController.doPrivileged(new VFSCacheCreatorAction(defaultCacheImpl));
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
      private String defaultCacheImpl;
      VFSCacheCreatorAction(String defaultCacheImpl)
      {
         this.defaultCacheImpl = defaultCacheImpl;
      }

      public VFSCache run()
      {
         try
         {
            // First look to the input cache imple
            String className = defaultCacheImpl;
            if(className == null || className.length() == 0)
            {
               // Else look at the VFS_CACHE_KEY system property
               className = System.getProperty(VFSUtils.VFS_CACHE_KEY);
            }
            if (className != null)
            {
               log.info("Initializing VFSCache [" + className + "] ...");
               ClassLoader cl = VFSCacheFactory.class.getClassLoader();
               Class<?> clazz = cl.loadClass(className);
               VFSCache cache = VFSCache.class.cast(clazz.newInstance());
               cache.start(); // start here, so we fall back to default no-op in case start fails
               log.info("Using VFSCache [" + cache + "] ...");
               return cache;
            }
         }
         catch (Throwable t)
         {
            log.warn("Exception instantiating VFS cache: ", t);
         }
         log.info("Using VFSCache [NoopVFSCache] ...");
         return new NoopVFSCache();
      }
   }
}
