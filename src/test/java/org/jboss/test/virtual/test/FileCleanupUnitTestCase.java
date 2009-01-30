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
import java.net.URI;
import java.net.URL;

import junit.framework.Test;
import org.jboss.util.id.GUID;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.cache.LRUVFSCache;
import org.jboss.virtual.plugins.copy.AbstractCopyMechanism;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.registry.VFSRegistryBuilder;

/**
 * Test file closing
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class FileCleanupUnitTestCase extends AbstractVFSTest
{
   private File tempDir;

   public FileCleanupUnitTestCase(String name)
   {
      super(name, true, true);
   }

   protected FileCleanupUnitTestCase(String name, boolean forceCopy, boolean forceNoReaper)
   {
      super(name, forceCopy, forceNoReaper);
   }

   public static Test suite()
   {
      VFS.init();
      return suite(FileCleanupUnitTestCase.class);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      // nullify the temp dir
      Class<?> clazz = AbstractCopyMechanism.class;
      Field field = clazz.getDeclaredField("tempDir");
      field.setAccessible(true);
      field.set(null, null);

      // nullify the registry
      clazz = VFSRegistryBuilder.class;
      field = clazz.getDeclaredField("singleton");
      field.setAccessible(true);
      field.set(null, null);

      String tempDirKey = System.getProperty("vfs.temp.dir", "jboss.server.temp.dir");
      String tempDirString = System.getProperty(tempDirKey, System.getProperty("java.io.tmpdir")) + GUID.asString();

      tempDir =  new File(tempDirString);
      tempDir.deleteOnExit();      
      if (tempDir.exists())
      {
         deleteTempDir();
      }
      assertTrue(tempDir.mkdir());

      System.setProperty("jboss.server.temp.dir", tempDirString);

      VFSCache cache = new LRUVFSCache(2, 5);
      cache.start();
      VFSCacheFactory.setInstance(cache);
   }

   @Override
   protected void tearDown() throws Exception
   {
      try
      {
         deleteTempDir();

         VFSCacheFactory.getInstance().stop();
         VFSCacheFactory.setInstance(null);

         System.clearProperty("jboss.server.temp.dir");
      }
      catch (Throwable t)
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

   protected void assertTempFiles(int size) throws Exception
   {
      File dir = new File(tempDir, "vfs-nested.tmp");
      File[] files = dir.listFiles();
      assertNotNull(files);
      assertEquals(size, files.length);
   }

   protected void assertCopyMechanismFiles(int size) throws Exception
   {
      File[] files = tempDir.listFiles();
      assertNotNull(files);
      int counter = 0;
      for (File dir : files)
      {
         counter += dir.listFiles().length;
      }
      assertEquals(size, counter);
   }

   protected void assertCacheExists(URI uri) throws Exception
   {
      VFSCache cache = VFSCacheFactory.getInstance();
      VirtualFile file = cache.getFile(uri);
      assertNotNull(file);
   }

   protected void assertNoCache(URI uri) throws Exception
   {
      VFSCache cache = VFSCacheFactory.getInstance();
      VirtualFile file = cache.getFile(uri);
      assertNull("" + uri, file);
   }

   public void testNestedJarCleanup() throws Exception
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);
      VirtualFile child = root.getChild("complex.jar");
      assertNotNull(child);
      VirtualFile nestedChild = child.getChild("child");
      assertNotNull(nestedChild);

      assertTempFiles(1);

      nestedChild.cleanup();
      assertCacheExists(nestedChild.toURI());

      root.cleanup();

      assertTempFiles(0);
      assertNoCache(root.toURI());
   }

   public void testExplicitCopyCleanup() throws Exception
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);

      VirtualFile copy = VFSUtils.temp(root);
      assertNotNull(copy);
      assertTrue(VFSUtils.isTemporaryFile(copy));

      assertCopyMechanismFiles(1);

      copy.cleanup();

      assertCopyMechanismFiles(0);

      root.cleanup();
      assertNoCache(root.toURI());
   }
}