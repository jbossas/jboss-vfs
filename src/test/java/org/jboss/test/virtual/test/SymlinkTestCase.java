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

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.Test;
import org.jboss.test.virtual.support.MockTempStore;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.util.propertyeditor.URLEditor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.cache.CombinedVFSCache;
import org.jboss.virtual.plugins.copy.TrackingTempStore;
import org.jboss.virtual.spi.ExceptionHandler;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.cache.helpers.NoopVFSCache;

/**
 * Symlink VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class SymlinkTestCase extends AbstractVFSTest
{
   private String testPath;
   private String testName;
   private boolean useEditor;

   public SymlinkTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(SymlinkTestCase.class);
   }

   // enable this to run the test -- no Winz though :-)
   private static boolean supportSymlinks()
   {
//      return false;
//
      String os = System.getProperty("os.name");
      return os.contains("Win") == false;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      // enable force canonical
      System.setProperty(VFSUtils.FORCE_CANONICAL, "true");

      // setup symlink dir and test path!

//      System.setProperty("test.dir", "/Users/alesj/projects/jboss6/trunk"); // plain path
      System.setProperty("test.dir", "/Users/alesj/jboss"); // -- this is symlink

      testPath = "/testsuite/output/lib/jboss-seam-booking.ear/jboss-seam.jar/org/jboss/seam/Seam.class";
      testName = "jboss-seam.jar";
      useEditor = true;
   }

   @Override
   protected void tearDown() throws Exception
   {
      System.clearProperty("test.dir");
      testPath = null;
      testName = null;

      super.tearDown();
   }

   public void testAppAsLink() throws Exception
   {
      if (supportSymlinks() == false)
         return;

      URL dir = getResource("/vfs/symlink");

      CombinedVFSCache cache = new CombinedVFSCache();
      VFSCacheFactory.setInstance(cache);
      cache.setPermanentRoots(Collections.<URL, ExceptionHandler>singletonMap(dir, null));

      VirtualFile root = VFS.getRoot(dir);
      VirtualFile app = root.getChild("app.jar");
      VirtualFile clazz = app.getChild("org/jboss/test/vfs/support/CommonClass.class");
      Assert.assertNotNull(clazz);
      URL url = clazz.toURL();

      VirtualFile vf1 = VFS.getRoot(url);
      Assert.assertNotNull(vf1);
      VirtualFile vf2 = VFS.getRoot(url);
      Assert.assertNotNull(vf2);
   }

   public void testCacheUsage() throws Exception
   {
      if (supportSymlinks() == false)
         return;

      assertNotNull(testPath);
      assertNotNull(testName);

      CombinedVFSCache cache = new CombinedVFSCache();                           
      VFSCache realCache = new NoopVFSCache();
      realCache.start();
      cache.setRealCache(realCache);
      VFSCacheFactory.setInstance(cache);
      try
      {
         String rootText = StringPropertyReplacer.replaceProperties("${test.dir}");
         URL rootURL;
         if (useEditor)
         {
            URLEditor editor = new URLEditor();
            editor.setAsText(rootText);
            rootURL = (URL) editor.getValue();
         }
         else
         {
            rootURL = new URL("file://" + rootText);
         }
         cache.setPermanentRoots(Collections.<URL, ExceptionHandler>singletonMap(rootURL, null));
         cache.start();

         // setup VFS
         VFS vfs = VFS.getVFS(rootURL);
         VFSUtils.enableCopy(vfs);
         TrackingTempStore store = new TrackingTempStore(new MockTempStore(new Random().nextLong()));
         vfs.setTempStore(store);

         try
         {
            URL directRootURL = new URL("file://" + rootText);
            VirtualFile root = VFS.getRoot(directRootURL);
            // assertEquals(vfs, root.getVFS()); // this is actually the real cause
            VirtualFile file = root.getChild(testPath);
            assertNotNull(file);
            assertTrue(file.getSize() > 0);
            assertCopies(store);
            URL url = file.toURL();
            URLConnection conn = url.openConnection();
            assertCopies(store);
            assertEquals(file.getLastModified(), conn.getLastModified());

            directRootURL = new URL("vfszip://" + rootText + testPath);
            conn = directRootURL.openConnection();
            assertCopies(store);
            assertEquals(file.getLastModified(), conn.getLastModified());
         }
         finally
         {
            store.clear();
         }
      }
      finally
      {
         VFSCacheFactory.setInstance(null);
         cache.stop();
      }
   }

   protected void assertCopies(TrackingTempStore store)
   {
      int counter = 0;
      for (File file : store.getFiles())
      {
         if (file.getName().contains(testName))
            counter++;
      }
      assertEquals("Test files == 1", 1, counter);
   }
}