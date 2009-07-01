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

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import junit.framework.Test;

/**
 * Combined VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class CombinedVFSCacheTestCase extends VFSCacheTest
{
   public CombinedVFSCacheTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(CombinedVFSCacheTestCase.class);
   }

   @Override
   protected void configureCache(VFSCache cache) throws Exception
   {
      if (cache instanceof CombinedVFSCache)
      {
         CombinedVFSCache cvc = CombinedVFSCache.class.cast(cache);

         URL url = getResource("/vfs/test/nested");
         Map<URL, ExceptionHandler> map = Collections.singletonMap(url, null);
         cvc.setPermanentRoots(map);

         IterableTimedVFSCache realCache = new IterableTimedVFSCache(5);
         realCache.start();
         cvc.setRealCache(realCache);

         cvc.create();
      }
   }

   @Override
   protected void stopCache(VFSCache cache)
   {
      if (cache != null)
      {
         if (cache instanceof CombinedWrapperVFSCache)
         {
            CombinedWrapperVFSCache cwvc = (CombinedWrapperVFSCache)cache;
            cwvc.getTemp().stop();
         }
         cache.stop();
      }
   }

   @Override
   protected Class<? extends VFSCache> getCacheClass()
   {
      return CombinedVFSCache.class;
   }

   protected CombinedVFSCache createCache()
   {
      return new CombinedWrapperVFSCache();
   }

   protected Map<Object, Object> getMap()
   {
      return null;
   }

   protected void testCachedContexts(Iterable<VFSContext> iter)
   {
      VFSContext context = iter.iterator().next();
      assertNotNull(context);
   }

   private class CombinedWrapperVFSCache extends CombinedVFSCache
   {
      private VFSCache temp;

      @Override
      public void setRealCache(VFSCache realCache)
      {
         super.setRealCache(realCache);
         temp = realCache;
      }

      public VFSCache getTemp()
      {
         return temp;
      }
   }
}