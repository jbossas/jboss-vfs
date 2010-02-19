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

import junit.framework.Test;
import org.jboss.test.virtual.support.MockTempStore;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.util.propertyeditor.URLEditor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.cache.MapVFSCache;
import org.jboss.virtual.plugins.copy.TrackingTempStore;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Symlink VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class SymlinkTestCase extends AbstractVFSTest
{
   private String testPath;

   public SymlinkTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(SymlinkTestCase.class);
   }

   private static boolean supportSymlinks()
   {
      return false;

        // enable this to run the test -- no Winz :-)
//      String os = System.getProperty("os.name");
//      return os.contains("Win") == false;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      // setup symlink dir and test path!

      //System.setProperty("test.dir", "/Users/alesj/projects/jboss6/trunk"); // plain path
      System.setProperty("test.dir", "/Users/alesj/jboss"); // -- this is symlink

      testPath = "/testsuite/output/lib/jboss-seam-booking.ear/jboss-seam.jar/org/jboss/seam/Seam.class";
   }

   @Override
   protected void tearDown() throws Exception
   {
      System.clearProperty("test.dir");
      testPath = null;

      super.tearDown();
   }

   public void testCacheUsage() throws Exception
   {
      if (supportSymlinks() == false)
         return;

      assertNotNull(testPath);

      VFSCache cache = new MapVFSCache()
      {
         @Override
         protected Map<String, VFSContext> createMap()
         {
            return new HashMap<String, VFSContext>();
         }
      };
      cache.start();
      VFSCacheFactory.setInstance(cache);
      try
      {
         URLEditor editor = new URLEditor();
         String rootText = StringPropertyReplacer.replaceProperties("${test.dir}");
         editor.setAsText(rootText);
         URL rootURL = (URL) editor.getValue();
         VFS vfs = VFS.getVFS(rootURL);
         VFSUtils.enableCopy(vfs);
         TrackingTempStore store = new TrackingTempStore(new MockTempStore(new Random().nextLong()));
         vfs.setTempStore(store);
         try
         {
            VirtualFile root = vfs.getRoot();
            VirtualFile file = root.getChild(testPath);
            assertNotNull(file);
            assertCopies(store);
            URL url = file.toURL();
            URLConnection conn = url.openConnection();
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
         if (file.getName().contains("jboss-seam.jar"))
            counter++;
      }
      assertEquals("Seam files == 1", 1, counter);
   }
}