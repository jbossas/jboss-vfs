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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.cache.CacheStatistics;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.helpers.NoopVFSCache;

/**
 * Combined vfs cache - permanent entries + real cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class CombinedVFSCache implements VFSCache, CacheStatistics
{
   private PermanentVFSCache permanentCache = new PermanentVFSCache();
   private VFSCache realCache;
   private boolean initializing;

   /**
    * Set permanent roots and its exception handlers.
    *
    * @param initializationEntries the initialization entries
    * @throws IOException for any error
    */
   public void setPermanentRoots(Map<URL, ExceptionHandler> initializationEntries) throws Exception
   {
      if (initializationEntries != null && initializationEntries.isEmpty() == false)
      {
         if (permanentCache.isStarted() == false)
            permanentCache.start();

         initializing = true;
         try
         {
            for (Map.Entry<URL, ExceptionHandler> entry : initializationEntries.entrySet())
            {
               VFS vfs = VFS.getVFS(entry.getKey());
               ExceptionHandler eh = entry.getValue();
               if (eh != null)
                  vfs.setExceptionHandler(eh);
            }
         }
         finally
         {
            initializing = false;
         }
      }
   }

   /**
    * Set the real cache.
    *
    * @param realCache the real cache
    */
   public void setRealCache(VFSCache realCache)
   {
      this.realCache = realCache;
   }

   /**
    * Check at create.
    */
   public void create()
   {
      check();
   }

   /**
    * Check if real cache has been set.
    */
   private void check()
   {
      if (realCache == null)
         realCache = new NoopVFSCache();
   }

   public VirtualFile getFile(URI uri) throws IOException
   {
      VirtualFile file = permanentCache.getFile(uri);
      if (file != null)
         return file;

      check();
      return realCache.getFile(uri);
   }

   public VirtualFile getFile(URL url) throws IOException
   {
      try
      {
         return getFile(url.toURI());
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
      if (initializing)
      {
         permanentCache.putContext(context);
      }
      else
      {
         check();
         realCache.putContext(context);
      }
   }

   public void removeContext(VFSContext context)
   {
      check();
      realCache.removeContext(context);
   }

   public void start() throws Exception
   {
      if (permanentCache.isStarted() == false)
         permanentCache.start();
   }

   public void stop()
   {
      if (permanentCache.isStarted())
         permanentCache.stop();
   }

   public void flush()
   {
      check();
      realCache.flush();
   }

   public Iterable<VFSContext> getCachedContexts()
   {
      List<VFSContext> contexts = new ArrayList<VFSContext>();

      for (VFSContext context : permanentCache.getCachedContexts())
         contexts.add(context);

      if (realCache instanceof CacheStatistics)
      {
         CacheStatistics cs = CacheStatistics.class.cast(realCache);
         for (VFSContext context : cs.getCachedContexts())
            contexts.add(context);
      }

      return contexts;
   }

   public int size()
   {
      int size = permanentCache.size();
      if (realCache instanceof CacheStatistics)
      {
         size += CacheStatistics.class.cast(realCache).size();
      }
      return size;
   }

   public long lastInsert()
   {
      long permanentHit = permanentCache.lastInsert();
      long realHit = -1;
      if (realCache instanceof CacheStatistics)
      {
         realHit = CacheStatistics.class.cast(realCache).lastInsert();
      }
      return permanentHit > realHit ? permanentHit : realHit;
   }

   @Override
   public String toString()
   {
      return "CombinedVFSCache[real-cache: " + realCache + "]";
   }

   private class PermanentVFSCache extends MapVFSCache
   {
      private boolean started;

      protected Map<String, VFSContext> createMap()
      {
         return new TreeMap<String, VFSContext>();
      }

      @Override
      public void start() throws Exception
      {
         super.start();
         started = true;
      }

      /**
       * Is the cache started.
       *
       * @return the started flag
       */
      public boolean isStarted()
      {
         return started;
      }
   }
}