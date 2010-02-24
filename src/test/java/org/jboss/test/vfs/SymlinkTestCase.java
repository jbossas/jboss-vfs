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
package org.jboss.test.vfs;

import junit.framework.Test;
import org.jboss.util.StringPropertyReplacer;
import org.jboss.util.propertyeditor.URLEditor;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.Closeable;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Symlink VFSCache Test.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class SymlinkTestCase extends AbstractVFSTest
{
   private String testOuterJar;
   private String testInnerJar;
   private String testInnerFile;
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
      return false;

//      String os = System.getProperty("os.name");
//      return os.contains("Win") == false;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      // setup symlink dir and test path!

//      System.setProperty("test.dir", "/Users/alesj/projects/jboss6/trunk"); // plain path
      System.setProperty("test.dir", "/Users/alesj/jboss"); // -- this is symlink

      testOuterJar = "/testsuite/output/lib/jboss-seam-booking.ear";
      testInnerJar = "jboss-seam.jar";
      testInnerFile = "org/jboss/seam/Seam.class";
      useEditor = true;
   }

   @Override
   protected void tearDown() throws Exception
   {
      System.clearProperty("test.dir");
      testOuterJar = null;
      testInnerJar = null;
      testInnerFile = null;

      super.tearDown();
   }

   public void testSmoke() throws Exception
   {
      if (supportSymlinks() == false)
         return;

      assertNotNull(testOuterJar);
      assertNotNull(testInnerJar);
      assertNotNull(testInnerFile);

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

      VirtualFile root = VFS.getChild(rootURL);
      VirtualFile outerJar = root.getChild(testOuterJar);
      List<Closeable> closables = recursiveMount(outerJar);
      try
      {
         VirtualFile innerJar = outerJar.getChild(testInnerJar);

         VirtualFile file = innerJar.getChild(testInnerFile);
         assertNotNull(file);
         assertTrue(file.exists());
         assertTrue(file.getSize() > 0);
         URL url = file.toURL();
         URLConnection conn = url.openConnection();
         long expected = file.getLastModified();
         long actual1 = conn.getLastModified();
         assertEquals(expected, actual1);

         URL directRootURL = new URL("file://" + rootText + testOuterJar + "/" + testInnerJar + "/" + testInnerFile);
         conn = directRootURL.openConnection();
         long actual2 = conn.getLastModified();
         assertEquals(expected, actual2); // TODO -- FIXME!
      }
      finally
      {
         VFSUtils.safeClose(closables);
      }
   }
}