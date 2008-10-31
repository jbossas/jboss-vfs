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

import java.util.Collections;
import java.util.Map;

import org.jboss.virtual.spi.VFSContext;

/**
 * Map vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class MapVFSCache extends AbstractVFSCache
{
   private Map<String, VFSContext> cache;

   public Iterable<VFSContext> getCachedContexts()
   {
      if (cache == null)
         return Collections.emptySet();
      else
         return cache.values(); 
   }

   public int size()
   {
      return cache != null ? cache.size() : -1;
   }

   protected void check()
   {
      if (cache == null)
         throw new IllegalArgumentException("Cache needs to be started first.");
   }

   protected VFSContext getContext(String path)
   {
      return cache.get(path);
   }

   protected void putContext(String path, VFSContext context)
   {
      cache.put(path, context);
   }

   protected void removeContext(String path, VFSContext context)
   {
      cache.remove(path);
   }

   /**
    * Create cache map.
    *
    * @return cache map
    */
   protected abstract Map<String, VFSContext> createMap();

   public void start() throws Exception
   {
      cache = createMap();
   }

   public void stop()
   {
      flush();
   }

   public void flush()
   {
      if (cache != null)
         cache.clear();
   }
}