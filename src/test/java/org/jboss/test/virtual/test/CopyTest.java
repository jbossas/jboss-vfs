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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Copy tests.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class CopyTest extends AbstractVFSTest
{
   protected CopyTest(String s)
   {
      super(s);
   }

   protected abstract VirtualFile modify(VirtualFile file) throws Exception;

   protected void assertNoReplacement(VFS vfs, String name, boolean unpacked) throws Throwable
   {
      VirtualFile original = vfs.findChild(name);
      VirtualFile replacement = modify(original);
      assertNoReplacement(original, replacement, unpacked);
   }

   protected abstract void assertNoReplacement(VirtualFile original, VirtualFile replacement, boolean unpacked) throws Exception;

   public void testNoReplacement() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);

      assertNoReplacement(vfs, "unpacked-outer.jar", true);
      assertNoReplacement(vfs, "jar1-filesonly.jar", false);
      assertNoReplacement(vfs, "jar1-filesonly.mf", false);
      assertNoReplacement(vfs, "unpacked-with-metadata.jar/META-INF", true);
   }

   public void testCopyOuter() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("outer.jar");
      replacement = modify(original);
      assertTopLevelParent(original.getParent(), replacement.getParent());

      VirtualFile child = replacement.findChild("jar1.jar");
      assertNotNull(child);
      assertNotNull(child.findChild("META-INF/MANIFEST.MF"));
      assertNotNull(replacement.findChild("jar2.jar"));
   }

   protected void assertTopLevelParent(VirtualFile originalParent, VirtualFile replacementParent) throws Exception
   {
      assertEquals(originalParent, replacementParent);
   }

   public void testUnpackTopLevel() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("level1.zip");
      replacement = modify(original);
      assertTopLevel(original, replacement);

      VirtualFile textOne = replacement.findChild("test1.txt");
      testText(textOne);
      VirtualFile two = replacement.findChild("level2.zip");
      VirtualFile textTwo = two.findChild("test2.txt");
      testText(textTwo);
      VirtualFile three = two.findChild("level3.zip");
      VirtualFile textThree = three.findChild("test3.txt");
      testText(textThree);
   }

   protected abstract void assertTopLevel(VirtualFile original, VirtualFile replacement) throws Exception;

   protected abstract void assertNestedLevel(VirtualFile original, VirtualFile replacement) throws Exception;

   public void testUnpack2ndLevel() throws Throwable
   {
      URL rootURL = getResource("/vfs/test");
      VFS vfs = VFS.getVFS(rootURL);
      VirtualFile original;
      VirtualFile replacement;

      original = vfs.findChild("level1.zip/level2.zip");
      replacement = modify(original);
      assertNestedLevel(original, replacement);
      VirtualFile parent = original.getParent();
      VirtualFile child = parent.findChild("level2.zip");
      //assertEquals(replacement, child);
      assertOnURI(child, replacement);

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
      replacement = modify(original);
      assertNestedLevel(original, replacement);
      VirtualFile parent = original.getParent();
      VirtualFile child = parent.findChild("level3.zip");
      //assertEquals(replacement, child);
      assertOnURI(child, replacement);

      VirtualFile textThree = replacement.findChild("test3.txt");
      testText(textThree);
   }

   protected void assertOnURI(VirtualFile original, VirtualFile replacement) throws Exception
   {
      URI originalUri = original.toURI();
      URI replacementUri = replacement.toURI();
      assertEquals(originalUri, replacementUri);
   }

   protected void assertUnpackedReplacement(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertReplacement(original, replacement);
      assertEquals(original.getParent(), replacement.getParent());
   }

   protected void assertExplodedReplacement(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertReplacement(original, replacement);
      assertNull(replacement.getParent());
   }

   protected void assertReplacement(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertReplacement(original, replacement, true);
   }

   protected void assertReplacement(VirtualFile original, VirtualFile replacement, boolean exploded) throws Exception
   {
      assertEquals(original.getName(), replacement.getName());
      // when mounting via DelegatingHandler, getPathName changes because VFSContext changes
      //assertEquals(original.getPathName(), replacement.getPathName());

      // Only check the non-directory file sizes
      if (exploded == false)
         assertEquals(original.getSize(), replacement.getSize());
      assertEquals(original.exists(), replacement.exists());
      assertEquals(original.isLeaf(), replacement.isLeaf());
      assertEquals(original.isHidden(), replacement.isHidden());
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