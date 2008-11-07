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
package org.jboss.test.virtual.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.cache.LRUVFSCache;
import org.jboss.virtual.spi.cache.VFSCache;

/**
 * LRU VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class LRUCacheTestCase extends CachePolicyVFSCacheTest
{
   public LRUCacheTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(LRUCacheTestCase.class);
   }

   protected VFSCache createCache()
   {
      return new LRUVFSCache(2, 10);
   }

   protected Iterable<String> populateRequiredSystemProperties()
   {
      System.setProperty(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.min", "2");
      System.setProperty(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.max", "10");      
      return Arrays.asList(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.min", VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.max");
   }

   protected Map<Object, Object> getMap()
   {
      Map<Object, Object> map = new HashMap<Object, Object>();
      map.put(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.min", 2);
      map.put(VFSUtils.VFS_CACHE_KEY + ".LRUPolicyCaching.max", 10);
      return map;
   }
}