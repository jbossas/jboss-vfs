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
import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Manifest;
import java.util.jar.JarOutputStream;
import java.util.List;

import junit.framework.Test;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.context.zip.ZipEntryContext;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * ZipEntryHandlerUnitTestCase.
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipEntryHandlerUnitTestCase extends JARVirtualFileHandlerUnitTestCase
{
   public ZipEntryHandlerUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(ZipEntryHandlerUnitTestCase.class);
   }

   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getRootResource(name);
      url = JarUtils.createJarURL(url);
      return new ZipEntryContext(url);
   }

   /**
    * Test VirtualFile.delete() for zip archives accessed through FileSystemContext
    *
    * @throws Exception if an error occurs
    */
   public void testFileContextZipDelete() throws Exception
   {
      File tmpRoot = File.createTempFile("vfs", ".root");
      tmpRoot.delete();
      tmpRoot.mkdir();

      File tmp = File.createTempFile("testFileContextZipDelete", ".jar", tmpRoot);
      VFS vfs = VFS.getVFS(tmpRoot.toURL());

      Manifest mf = new Manifest();
      mf.getMainAttributes().putValue("Created-By", getClass().getName() + "." + "testEntryModified");
      FileOutputStream fos = new FileOutputStream(tmp);
      JarOutputStream jos = new JarOutputStream(fos, mf);
      try
      {
         jos.setComment("testJarURLs");
         jos.setLevel(0);
         jos.flush();
      }
      finally
      {
         jos.close();
      }

      // children() exist
      List<VirtualFile> children = vfs.getChildren();
      assertTrue(tmpRoot + ".getChildren().size() == 1", children.size() == 1);

      // specific child exists()
      VirtualFile tmpVF = vfs.getChild(tmp.getName());
      assertTrue(tmp + ".exists()", tmpVF.exists());


      // test jar entry
      // specific zip entry exists(), delete() not, exists()
      VirtualFile entryVF = tmpVF.getChild("META-INF");
      assertTrue(entryVF.getName() + " .exists()", entryVF.exists());
      assertFalse(entryVF.getName() + " .delete() == false", entryVF.delete());
      assertTrue(entryVF.getName() + " .exists()", entryVF.exists());

      // children() exist
      children = tmpVF.getChildren();
      assertTrue(tmpVF + ".getChildren().size() == 1", children.size() == 1);

      // getChild() returns not-null
      entryVF = tmpVF.getChild("META-INF");
      assertNotNull(tmpVF + ".getChild('META-INF') != null", entryVF);


      // continue with jar
      // specific child delete(), exists() not
      assertTrue(tmp + ".delete()", tmpVF.delete());
      assertFalse(tmp + ".exists() == false", tmpVF.exists());

      // children() don't exist
      children = vfs.getChildren();
      assertTrue(tmpRoot + ".getChildren().size() == 0", children.size() == 0);

      // getChild() returns null
      tmpVF = vfs.getChild(tmp.getName());
      assertNull(tmpRoot + ".getChild('" + tmp.getName() + "') == null", tmpVF);

      // directory delete()
      assertTrue(tmpRoot + ".delete()", vfs.getRoot().delete());
   }

   /**
    * Test VirtualFile.delete() for zip archive accessed through ZipEntryContext
    *
    * @throws Exception
    */
   public void testZipContextDelete() throws Exception
   {
      File tmp = File.createTempFile("testZipContextDelete", ".jar");

      Manifest mf = new Manifest();
      mf.getMainAttributes().putValue("Created-By", getClass().getName() + "." + "testEntryModified");
      FileOutputStream fos = new FileOutputStream(tmp);
      JarOutputStream jos = new JarOutputStream(fos, mf);
      try
      {
         jos.setComment("testJarURLs");
         jos.setLevel(0);
         jos.flush();
      }
      finally
      {
         jos.close();
      }

      VFS vfs = VFS.getVFS(tmp.toURL());

      // children() exist
      List<VirtualFile> children = vfs.getChildren();
      assertTrue(tmp + ".getChildren().size() == 1", children.size() == 1);

      // specific child exists(), delete() not, exists()
      VirtualFile tmpVF = vfs.getChild("META-INF");
      assertTrue(tmp + ".exists()", tmpVF.exists());
      assertFalse(tmp + ".delete() == false", tmpVF.delete());
      assertTrue(tmp + ".exists()", tmpVF.exists());

      // children() exist
      children = vfs.getChildren();
      assertTrue(tmp + ".getChildren().size() == 1", children.size() == 1);

      // getChild() returns not-null
      tmpVF = vfs.getChild("META-INF");
      assertNotNull(tmp + ".getChild('META-INF') != null", tmpVF);

      // archive delete(), exists() not
      assertTrue(tmp + ".delete()", vfs.getRoot().delete());
      assertFalse(tmp + ".exists() == false", vfs.getRoot().exists());
   }
}