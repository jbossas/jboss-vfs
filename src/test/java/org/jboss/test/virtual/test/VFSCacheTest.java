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

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.cache.CacheStatistics;
import org.jboss.virtual.spi.VFSContext;

/**
 * VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class VFSCacheTest extends AbstractVFSTest
{
   public VFSCacheTest(String name)
   {
      super(name);
   }

   protected abstract VFSCache createCache();

   @SuppressWarnings("deprecation")
   public void testCache() throws Exception
   {
      URL url = getResource("/vfs/test/nested");

      VFSCache cache = createCache();
      cache.start();
      try
      {
         VFSCacheFactory.setInstance(cache);
         try
         {
            VirtualFile root = VFS.getRoot(url);

            VirtualFile file = root.findChild("/nested.jar/META-INF/empty.txt");
            URL fileURL = file.toURL();
            VirtualFile nested = root.findChild("/nested.jar/complex.jar/subfolder/subsubfolder/subsubchild");
            URL nestedURL = nested.toURL();

            assertEquals(file, cache.getFile(fileURL));
            assertEquals(nested, cache.getFile(nestedURL));

            VFSCacheFactory.setInstance(null);
            VFSCache wrapper = new WrapperVFSCache(cache);
            VFSCacheFactory.setInstance(wrapper);

            assertEquals(file, wrapper.getFile(fileURL));
            assertEquals(nested, wrapper.getFile(nestedURL));
         }
         finally
         {
            VFSCacheFactory.setInstance(null);
         }
      }
      finally
      {
         cache.stop();
      }
   }

   protected abstract void testCachedContexts(Iterable<VFSContext> iter);

   public void testCacheStatistics() throws Exception
   {
      URL url = getResource("/vfs/test/nested");

      VFSCache cache = createCache();
      cache.start();
      try
      {
         if (cache instanceof CacheStatistics)
         {
            CacheStatistics statistics = CacheStatistics.class.cast(cache);
            VFSCacheFactory.setInstance(cache);
            try
            {
               VirtualFile root = VFS.getRoot(url);
               assertNotNull(root);

               Iterable<VFSContext> iter = statistics.getCachedContexts();
               testCachedContexts(iter);

               assertEquals(1, statistics.size());
               assertTrue(statistics.lastInsert() != 0);
            }
            finally
            {
               VFSCacheFactory.setInstance(null);
            }
         }
      }
      finally
      {
         cache.stop();
      }
   }

   private class WrapperVFSCache implements VFSCache
   {
      private VFSCache delegate;

      private WrapperVFSCache(VFSCache delegate)
      {
         this.delegate = delegate;
      }

      public VirtualFile getFile(URI uri) throws IOException
      {
         return delegate.getFile(uri);
      }

      public VirtualFile getFile(URL url) throws IOException
      {
         return delegate.getFile(url);
      }

      public void putContext(VFSContext context)
      {
         throw new IllegalArgumentException("Context should already be there: " + context);
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