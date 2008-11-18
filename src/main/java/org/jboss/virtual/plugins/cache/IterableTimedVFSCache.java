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

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.net.URI;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.util.TimedCachePolicy;

/**
 * Iterable timed cache policy vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class IterableTimedVFSCache extends TimedVFSCache
{
   public IterableTimedVFSCache()
   {
   }

   public IterableTimedVFSCache(Integer defaultLifetime)
   {
      super(defaultLifetime);
   }

   public IterableTimedVFSCache(Integer defaultLifetime, Boolean threadSafe, Integer resolution)
   {
      super(defaultLifetime, threadSafe, resolution);
   }

   public IterableTimedVFSCache(Map<Object, Object> properties)
   {
      super(properties);
   }

   @SuppressWarnings("unchecked")
   protected VFSContext findContext(URI uri)
   {
      String uriString = stripProtocol(uri);
      TimedCachePolicy tcp = getPolicy();
      List validKeys = tcp.getValidKeys();
      Set<String> keys = new TreeSet<String>(validKeys);
      readLock();
      try
      {
         for (String key : keys)
         {
            if (uriString.startsWith(key))
               return getContext(key);
         }
      }
      finally
      {
         readUnlock();
      }
      return null;
   }

   protected String getCacheName()
   {
      return "Iterable" + super.getCacheName();
   }
}