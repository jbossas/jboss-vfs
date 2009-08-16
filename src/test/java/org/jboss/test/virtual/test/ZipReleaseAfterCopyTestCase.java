/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

import junit.framework.Test;

import org.jboss.util.id.GUID;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.cache.LRUVFSCache;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.plugins.context.zip.ZipEntryContext;
import org.jboss.virtual.plugins.copy.AbstractCopyMechanism;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.registry.VFSRegistry;

/**
 * Verify a zip file is released after a temp copy is made.
 *
 * @author Jason T. Greene
 */
public class ZipReleaseAfterCopyTestCase extends AbstractVFSTest
{
   private File tempDir;

   public ZipReleaseAfterCopyTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      VFS.init();
      return suite(ZipReleaseAfterCopyTestCase.class);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      try
      {
         // nullify the temp dir
         Class<?> clazz = AbstractCopyMechanism.class;
         Field field = clazz.getDeclaredField("tempDir");
         field.setAccessible(true);
         field.set(null, null);

         String tempDirKey = System.getProperty("vfs.temp.dir", "jboss.server.temp.dir");
         String tempDirString = System.getProperty(tempDirKey, System.getProperty("java.io.tmpdir"));

         tempDir = new File(new File(tempDirString), GUID.asString());
         tempDir.deleteOnExit();
         if (tempDir.exists())
         {
            deleteTempDir();
         }
         assertTrue("mkdir " + tempDir, tempDir.mkdir());

         System.setProperty("jboss.server.temp.dir", tempDir.getCanonicalPath());

         VFSCache cache = new LRUVFSCache(2, 5);
         cache.start();
         VFSCacheFactory.setInstance(cache);
      }
      catch (Exception e)
      {
         tearDown();
         throw e;
      }
   }

   protected String getProtocol()
   {
      return "vfszip:";
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         deleteTempDir();
      }
      catch (Throwable ignored)
      {
      }

      try
      {
         VFSCacheFactory.getInstance().stop();
         VFSCacheFactory.setInstance(null);

         System.clearProperty("jboss.server.temp.dir");
      }
      catch (Throwable ignored)
      {
      }
      finally
      {
         super.tearDown();
      }
   }

   protected void deleteTempDir() throws IOException
   {
      // use vfs to disable possible reaper
      VirtualFile td = VFS.getRoot(tempDir.toURI());
      td.cleanup();
      td.delete();
   }


   protected void assertNoRegistryEntry(URI uri) throws Exception
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile file = registry.getFile(uri);
      assertNull("" + uri, file);
   }

   public void testReleaseAfterCopy() throws Exception
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);

      VirtualFile copy = VFSUtils.explode(root); // ::temp doesn't create zipFile, explode does
      try
      {
         assertNotNull(copy);
         assertTrue(VFSUtils.isTemporaryFile(copy));

         Method method = VirtualFile.class.getDeclaredMethod("getHandler");
         method.setAccessible(true);
         VirtualFileHandler h = (VirtualFileHandler)method.invoke(root);
         if (h instanceof DelegatingHandler)
            h = ((DelegatingHandler)h).getDelegate();

         method = h.getClass().getDeclaredMethod("getZipEntryContext");
         method.setAccessible(true);

         Field field = ZipEntryContext.class.getDeclaredField("zipSource");
         field.setAccessible(true);
         Object object = field.get(method.invoke(h));

         field = object.getClass().getDeclaredField("zipFile");
         field.setAccessible(true);

         assertNull(field.get(object));
         assertNotNull(root.openStream());
      }
      finally
      {
         copy.cleanup();
         root.cleanup();
      }
      assertNoRegistryEntry(root.toURI());
   }
}