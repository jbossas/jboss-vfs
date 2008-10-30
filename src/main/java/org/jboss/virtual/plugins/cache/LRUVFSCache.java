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

import org.jboss.util.CachePolicy;
import org.jboss.util.LRUCachePolicy;
import org.jboss.virtual.VFSUtils;

/**
 * LRU cache policy vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class LRUVFSCache extends CachePolicyVFSCache
{
   private Integer min;
   private Integer max;

   public LRUVFSCache()
   {
   }

   public LRUVFSCache(Integer min, Integer max)
   {
      this.min = min;
      this.max = max;
   }

   protected CachePolicy createCachePolicy()
   {
      if (min == null)
         min = parseInteger(readSystemProperty(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.min", null));
      if (max == null)
         max = parseInteger(readSystemProperty(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.max", null));

      if (min == null || max == null)
         throw new IllegalArgumentException("Missing min (" + min + ") or max (" + max + ").");

      log.debug("Creating LRU cache policy, min: " + min + ", max: " + max);

      return new LRUCachePolicy(min, max);
   }

   /**
    * Set min.
    *
    * @param min the min
    */
   public void setMin(Integer min)
   {
      this.min = min;
   }

   /**
    * set max.
    *
    * @param max the max
    */
   public void setMax(Integer max)
   {
      this.max = max;
   }
}