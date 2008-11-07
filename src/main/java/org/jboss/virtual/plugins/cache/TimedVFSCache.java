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

import org.jboss.util.CachePolicy;
import org.jboss.util.TimedCachePolicy;
import org.jboss.virtual.VFSUtils;

/**
 * Timed cache policy vfs cache.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class TimedVFSCache extends CachePolicyVFSCache
{
   private Integer defaultLifetime;
   private Boolean threadSafe;
   private Integer resolution;

   private String info;

   public TimedVFSCache()
   {
   }

   public TimedVFSCache(Integer defaultLifetime)
   {
      this(defaultLifetime, null, null);
   }

   public TimedVFSCache(Integer defaultLifetime, Boolean threadSafe, Integer resolution)
   {
      this.defaultLifetime = defaultLifetime;
      this.threadSafe = threadSafe;
      this.resolution = resolution;
   }

   public TimedVFSCache(Map<Object, Object> properties)
   {
      super(properties);
   }

   protected CachePolicy createCachePolicy()
   {
      if (defaultLifetime == null)
         defaultLifetime = getInteger(readInstanceProperties(VFSUtils.VFS_CACHE_KEY + ".TimedPolicyCaching.lifetime", null, true));
      if (threadSafe == null)
         threadSafe = Boolean.valueOf(readInstanceProperties(VFSUtils.VFS_CACHE_KEY + ".TimedPolicyCaching.threadSafe", Boolean.TRUE, true).toString());
      if (resolution == null)
         resolution = getInteger(readInstanceProperties(VFSUtils.VFS_CACHE_KEY + ".TimedPolicyCaching.resolution", null, true));

      log.debug("Creating timed cache policy, lifetime: " + defaultLifetime + ", threadSafe: " + threadSafe + ", resolution: " + resolution);

      TimedCachePolicy tcp;
      if (defaultLifetime == null)
         tcp = new TimedCachePolicy();
      else if (resolution != null)
         tcp = new TimedCachePolicy(defaultLifetime, threadSafe, resolution);
      else
         tcp = new TimedCachePolicy(defaultLifetime);

      info = "TimedVFSCache{lifetime=" + tcp.getDefaultLifetime() + ", resolution=" + tcp.getResolution() + "}";

      return tcp;
   }

   /**
    * Set default lifetime.
    *
    * @param defaultLifetime the default lifetime
    */
   public void setDefaultLifetime(Integer defaultLifetime)
   {
      this.defaultLifetime = defaultLifetime;
   }

   /**
    * Set threadsafe flag.
    *
    * @param threadSafe the threadsafe flag
    */
   public void setThreadSafe(Boolean threadSafe)
   {
      this.threadSafe = threadSafe;
   }

   /**
    * The resollution.
    *
    * @param resolution the resolution
    */
   public void setResolution(Integer resolution)
   {
      this.resolution = resolution;
   }

   public String toString()
   {
      return info;
   }
}