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
import java.io.FilenameFilter;
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
import org.jboss.virtual.spi.registry.VFSRegistry;

/**
 * Test file closing
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class FileCleanupUnitTest extends AbstractVFSRegistryTest
{
   private File tempDir;

   protected FileCleanupUnitTest(String name)
   {
      super(name, true, true);
   }

   protected FileCleanupUnitTest(String name, boolean forceCopy, boolean forceNoReaper)
   {
      super(name, forceCopy, forceNoReaper);
   }

   public static Test suite()
   {
      VFS.init();
      return suite(FileCleanupUnitTest.class);
   }

   protected abstract VirtualFile modify(VirtualFile original) throws Exception;

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
      catch (Exception e)
      {
         super.tearDown();
         throw e;
      }
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

   protected void assertTempFiles(int size) throws Exception
   {
      File dir = new File(tempDir, "vfs-nested.tmp");
      File[] files = dir.listFiles();
      if (dir.exists())
      {
         assertEquals(size, files.length);
      }
      else if (size == 0)
      {
         assertNull(files);
      }
      else
      {
         fail("Illegal dir: " + dir);
      }
   }

   protected void assertCopyMechanismFiles(int size) throws Exception
   {
      File[] files = tempDir.listFiles(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.contains("vfs-nested.tmp") == false;
         }
      });
      assertNotNull(files);
      int counter = 0;
      for (File dir : files)
      {
         File[] realFiles = dir.listFiles();
         counter += realFiles.length;
      }
      assertEquals(size, counter);
   }

   protected void assertRegistryEntryExists(URI uri) throws Exception
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile file = registry.getFile(uri);
      assertNotNull(file);
   }

   protected void assertNoRegistryEntry(URI uri) throws Exception
   {
      VFSRegistry registry = VFSRegistry.getInstance();
      VirtualFile file = registry.getFile(uri);
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
      assertRegistryEntryExists(nestedChild.toURI());

      root.cleanup();

      assertTempFiles(0);
      assertNoRegistryEntry(root.toURI());
   }

   public void testExplicitCopyCleanup() throws Exception
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);

      VirtualFile copy = modify(root);
      assertNotNull(copy);
      assertTrue(VFSUtils.isTemporaryFile(copy));

      assertCopyMechanismFiles(1);

      copy.cleanup();

      assertCopyMechanismFiles(0);

      root.cleanup();
      assertNoRegistryEntry(root.toURI());
   }

   public void test3Levels() throws Exception
   {
      URL url = getResource("/vfs/test");
      VFS root = VFS.getVFS(url);

      VirtualFile ear = root.getChild("level1.zip");
      VirtualFile earCopy = modify(ear);

      VirtualFile l3 = ear.getChild("level2.zip/level3.zip/test3.txt");
      assertNotNull(l3);
      assertTempFiles(2);

      VirtualFile l3copy = earCopy.getChild("level2.zip/level3.zip/test3.txt");
      assertNotNull(l3copy);
      assertCopyMechanismFiles(1);

      earCopy.cleanup();

      assertTempFiles(0);
      assertCopyMechanismFiles(0);
   }

   public void testDirectURLUsage() throws Exception
   {
      URL url = getResource("/vfs/test");
      VFS root = VFS.getVFS(url);

      VirtualFile ear = root.getChild("level1.zip");
      assertTempFiles(0);
      VirtualFile earCopy = modify(ear);

      VirtualFile l3 = earCopy.getChild("level2.zip/level3.zip/test3.txt");
      assertNotNull(l3);
      assertCopyMechanismFiles(1);

      url = new URL(root.getRoot().toURL().toExternalForm() + "level1.zip/level2.zip/level3.zip/test3.txt");
      VirtualFile l3url = VFS.getRoot(url);

      assertEquals(l3, l3url);
      assertTempFiles(getTempFiles());
      assertCopyMechanismFiles(1);

      earCopy.cleanup();

      assertCopyMechanismFiles(0);
   }

   protected int getTempFiles()
   {
      return 2;
   }

   // TODO - move this test
   public void testTempUrls() throws Exception
   {
      URL url = getResource("/vfs/test");
      String urlString = VFSUtils.stripProtocol(VFSUtils.toURI(url));
      VFS root = VFS.getVFS(url);

      VirtualFile ear = root.getChild("level1.zip");
      VirtualFile earCopy = modify(ear);
      assertEquals(ear.toURL(), earCopy.toURL());
      assertEquals(getProtocol() + urlString + "level1.zip/", earCopy.toURL().toExternalForm());

      VirtualFile o2 = ear.getChild("level2.zip");
      VirtualFile l2 = earCopy.getChild("level2.zip");
      assertEquals(o2.toURL(), l2.toURL());
      assertEquals(getProtocol() + urlString + "level1.zip/level2.zip/", l2.toURL().toExternalForm());

      VirtualFile o2sub = o2.getChild("test2.txt");
      VirtualFile l2sub = l2.getChild("test2.txt");
      assertEquals(o2sub.toURL(), l2sub.toURL());
      assertEquals(getProtocol() + urlString + "level1.zip/level2.zip/test2.txt", l2sub.toURL().toExternalForm());

      VirtualFile o3 = o2.getChild("level3.zip");
      VirtualFile l3 = l2.getChild("level3.zip");
      assertEquals(o3.toURL(), l3.toURL());
      assertEquals(getProtocol() + urlString + "level1.zip/level2.zip/level3.zip/", l3.toURL().toExternalForm());

      VirtualFile o3sub = o3.getChild("test3.txt");
      VirtualFile l3sub = l3.getChild("test3.txt");
      assertEquals(o3sub.toURL(), l3sub.toURL());
      assertEquals(getProtocol() + urlString + "level1.zip/level2.zip/level3.zip/test3.txt", l3sub.toURL().toExternalForm());

      ear.cleanup();
   }

   protected String getProtocol()
   {
      return "vfszip:";
   }

}