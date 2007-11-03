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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.test.BaseTestCase;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Test the caching strategy of VFS with jar files.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class JARCacheUnitTestCase extends BaseTestCase
{
   public JARCacheUnitTestCase(String name)
   {
      super(name);
   }

   public void test1() throws Exception
   {
      // Create a test.jar with v1 in manifest
      File testFile = new File("test.jar");
      {
         Manifest manifest = new Manifest();
         manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1");
         manifest.getMainAttributes().putValue("test", "v1");
         JarOutputStream out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(testFile)), manifest);
         out.flush();
         out.close();
      }
      
      // Verify it via VFS
      File root = new File(".");
      {
         VirtualFile vf = VFS.getVirtualFile(root.toURL(), "test.jar");
//         System.err.println("lastModified = " + vf.getLastModified());
         VirtualFile manifestFile = vf.findChild("META-INF/MANIFEST.MF");
         Manifest manifest = new Manifest(manifestFile.openStream());
         String actual = manifest.getMainAttributes().getValue("test");
         assertEquals("v1", actual);
      }
      
      // If we don't delete, VFS will give ZIP errors (related issue?)
      assertTrue("test file deleted: " + testFile, testFile.delete());
      
      // Create a new test.jar with manifest v2
      {
         Manifest manifest = new Manifest();
         manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1");
         manifest.getMainAttributes().putValue("test", "v2");
         JarOutputStream out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(testFile)), manifest);
         out.flush();
         out.close();
      }
      
      // Verify the manifest the JDK way
      {
         JarFile jarFile = new JarFile(testFile);
         String actual = jarFile.getManifest().getMainAttributes().getValue("test");
         assertEquals("JDK found the wrong manifest", "v2", actual);
         jarFile.close();
      }
      
      // Verify the manifest the VFS way
      {
         VirtualFile vf = VFS.getVirtualFile(root.toURL(), "test.jar");
         // Note that the modification date has not changed according to VFS
//         System.err.println("lastModified = " + vf.getLastModified());
//         System.err.println("modified = " + vf.hasBeenModified());
       
         VirtualFile manifestFile = vf.findChild("META-INF/MANIFEST.MF");
         Manifest manifest = new Manifest(manifestFile.openStream());
         String actual = manifest.getMainAttributes().getValue("test");
         assertEquals("VFS found the wrong manifest", "v2", actual);
      }
   }

   public static Test suite()
   {
      VFS.init();
      return new TestSuite(JARCacheUnitTestCase.class);
   }
}
