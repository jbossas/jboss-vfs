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
package org.jboss.virtual.plugins.cache;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.cache.CacheStatistics;
import org.jboss.virtual.spi.cache.VFSCache;

/**
 * Abstract vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractVFSCache implements VFSCache, CacheStatistics
{
   protected Logger log = Logger.getLogger(getClass());

   private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private long timestamp;

   public long lastInsert()
   {
      return timestamp;
   }

   /**
    * Is cache valid.
    */
   protected abstract void check();

   public VFSContext findContext(URL url)
   {
      try
      {
         return findContext(VFSUtils.toURI(url));
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the cached context.
    *
    * @param path the path to match
    * @return cached context or null if not found
    */
   protected abstract VFSContext getContext(String path);

   public void putContext(VFSContext context)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");

      check();

      if (log.isTraceEnabled())
      {
         StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
         log.trace("VFSContext: " + context + ", Stack-trace:\n" + Arrays.toString(stackTraceElements));
      }

      String path = VFSUtils.getKey(context);
      writeLock();
      try
      {
         putContext(path, context);
         timestamp = System.currentTimeMillis();
      }
      finally
      {
         writeUnlock();
      }
   }

   /**
    * Put vfs context and its path key into cache.
    *
    * @param path    the context's path
    * @param context the vfs context
    */
   protected abstract void putContext(String path, VFSContext context);

   public void removeContext(VFSContext context)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");

      check();

      String path = VFSUtils.getKey(context);
      writeLock();
      try
      {
         removeContext(path, context);
      }
      finally
      {
         writeUnlock();
      }
   }

   /**
    * Remove vfs context and its path key from cache.
    *
    * @param path    the context's path
    * @param context the vfs context
    */
   protected abstract void removeContext(String path, VFSContext context);

   /**
    * Read lock.
    */
   protected void readLock()
   {
      lock.readLock().lock();
   }

   /**
    * Read unlock.
    */
   protected void readUnlock()
   {
      lock.readLock().unlock();
   }

   /**
    * Write lock.
    */
   protected void writeLock()
   {
      lock.writeLock().lock();
   }

   /**
    * Write unlock.
    */
   protected void writeUnlock()
   {
      lock.writeLock().unlock();
   }
}
