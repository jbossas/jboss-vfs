/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
import java.io.FileOutputStream;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * jar entry tests.
 *
 * @author Ales.Justin@jboss.org
 */
public class JAREntryTestCase extends OSAwareVFSTest
{
   public JAREntryTestCase(String name)
   {
      super(name);
   }

   protected JAREntryTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy);
   }

   public static Test suite()
   {
      return suite(JAREntryTestCase.class);
   }

   public void testEntryModified() throws Exception
   {
      File tmp = File.createTempFile("testJarEntry", ".jar");
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

      VFS vfs = VFS.getVFS(tmp.toURI());
      VirtualFile root = vfs.getRoot();
      VirtualFile metainf = root.findChild("META-INF");
      List<VirtualFile> children = metainf.getChildren();
      assertEquals(1, children.size());

      fos = new FileOutputStream(tmp);
      jos = new JarOutputStream(fos, mf);
      try
      {
         ZipEntry entry = new ZipEntry("META-INF/some.txt");
         entry.setComment("some_comment");
         entry.setExtra("qwerty".getBytes());
         entry.setSize(1);
         jos.putNextEntry(entry);
         jos.closeEntry();

         entry = new ZipEntry("META-INF/other.txt");
         entry.setComment("other_comment");
         entry.setExtra("foobar".getBytes());
         entry.setSize(1);
         jos.putNextEntry(entry);
         jos.closeEntry();
      }
      finally
      {
         jos.close();
      }

      // TODO - JBVFS-7 ... should work w/o creating new vfs 
      vfs = VFS.getVFS(tmp.toURI());
      metainf = vfs.findChild("META-INF");
      System.out.println("root = " + root.hasBeenModified());
      System.out.println("metainf = " + metainf.hasBeenModified());
      children = metainf.getChildren();
      assertEquals(3, children.size());
   }
}