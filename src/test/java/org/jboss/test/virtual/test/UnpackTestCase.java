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
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * Unpack tests.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class UnpackTestCase extends AbstractVFSTest
{
   public UnpackTestCase(String s)
   {
      super(s);
   }

   public static Test suite()
   {
      return suite(UnpackTestCase.class);
   }

   protected void assertNoReplacement(VFS vfs, String name) throws Throwable
   {
      VirtualFile original = vfs.findChild(name);
      VirtualFile replacement = VFSUtils.unpack(original);
      assertSame(original, replacement);
   }

   public void testNoReplacement() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      assertNoReplacement(vfs, "unpacked-outer.jar");
      assertNoReplacement(vfs, "jar1-filesonly.mf");
      assertNoReplacement(vfs, "unpacked-with-metadata.jar/META-INF");
   }

   public void testUnpackOuter() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("outer.jar");
      replacement = VFSUtils.unpack(original);
      assertEquals(original.getParent(), replacement.getParent());

      VirtualFile child = replacement.findChild("jar1.jar");
      assertNotNull(child);
      assertNotNull(child.findChild("META-INF/MANIFEST.MF"));
      assertNotNull(replacement.findChild("jar2.jar"));
   }

   public void testUnpackTopLevel() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("level1.zip");
      replacement = VFSUtils.unpack(original);
      assertReplacement(original, replacement);
      VirtualFile parent = original.getParent();
      VirtualFile child = parent.findChild("level1.zip");
      assertEquals(replacement, child);

      VirtualFile textOne = replacement.findChild("test1.txt");
      testText(textOne);
      VirtualFile two = replacement.findChild("level2.zip");
      VirtualFile textTwo = two.findChild("test2.txt");
      testText(textTwo);
      VirtualFile three = two.findChild("level3.zip");
      VirtualFile textThree = three.findChild("test3.txt");
      testText(textThree);
   }

   public void testUnpack2ndLevel() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("level1.zip/level2.zip");
      replacement = VFSUtils.unpack(original);
      assertReplacement(original, replacement);
      VirtualFile parent = original.getParent();
      VirtualFile child = parent.findChild("level2.zip");
      //assertEquals(replacement, child);
      assertEquals(replacement.toURI(), child.toURI());

      VirtualFile textTwo = replacement.findChild("test2.txt");
      testText(textTwo);
      VirtualFile three = replacement.findChild("level3.zip");
      VirtualFile textThree = three.findChild("test3.txt");
      testText(textThree);
   }

   public void testUnpackDeepLevel() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("level1.zip/level2.zip/level3.zip");
      replacement = VFSUtils.unpack(original);
      assertReplacement(original, replacement);
      VirtualFile parent = original.getParent();
      VirtualFile child = parent.findChild("level3.zip");
      //assertEquals(replacement, child);
      assertEquals(replacement.toURI(), child.toURI());

      VirtualFile textThree = replacement.findChild("test3.txt");
      testText(textThree);
   }

   protected void assertReplacement(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertEquals(original.getName(), replacement.getName());
      // when mounting via DelegatingHandler, getPathName changes because VFSContext changes
      //assertEquals(original.getPathName(), replacement.getPathName());
      
      // it's a directory
      assertEquals(0, replacement.getSize());
      assertEquals(original.exists(), replacement.exists());
      assertEquals(original.isLeaf(), replacement.isLeaf());
      assertEquals(original.isHidden(), replacement.isHidden());
      assertEquals(original.getParent(), replacement.getParent());
   }

   protected void testText(VirtualFile file) throws Exception
   {
      InputStream in = file.openStream();
      try
      {
         BufferedReader reader = new BufferedReader(new InputStreamReader(in));
         String line;
         while ((line = reader.readLine()) != null)
         {
            assertEquals("Some test.", line);
         }
      }
      finally
      {
         in.close();
      }
   }
}