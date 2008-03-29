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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Basic tests of URL existence based on URLConnection.getLastModified
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
public class URLExistsUnitTestCase extends OSAwareVFSTest
{
   public URLExistsUnitTestCase(String name)
   {
      super(name);
   }

   /**
    * Test file deletion can be detected via URLConnection.getLastModified == 0.
    * @throws Exception
    */
   public void testFileURLs() throws Exception
   {
      File tmp = File.createTempFile("testFileURLs", null);
      URL tmpURL = tmp.toURL();
      URLConnection conn = tmpURL.openConnection();
      InputStream in = conn.getInputStream();
      long lastModified;
      try
      {
         lastModified = conn.getLastModified();
         System.out.println("lastModified, "+lastModified);
         assertNotSame("lastModified", 0, lastModified);
      }
      finally
      {
         in.close();
      }
      assertTrue(tmp.getAbsolutePath()+" deleted", tmp.delete() || isWindowsOS());
      conn = tmpURL.openConnection();
      lastModified = conn.getLastModified();
      System.out.println("lastModified after delete, "+lastModified);
      assertEquals("lastModified", 0, lastModified);
   }

   /**
    * Test jar deletion can be detected via URLConnection.getLastModified == 0.
    * @throws Exception
    */
   public void testJarURLs() throws Exception
   {
      File tmp = File.createTempFile("testFileURLs", ".jar");
      Manifest mf = new Manifest();
      mf.getMainAttributes().putValue("Created-By", ".URLExistsUnitTestCase.testJarURLs");
      FileOutputStream fos = new FileOutputStream(tmp);
      JarOutputStream jos = new JarOutputStream(fos, mf);
      jos.setComment("testJarURLs");
      jos.setLevel(0);
      jos.close();

      URL tmpURL = new URL("jar:"+tmp.toURL()+"!/");
      URLConnection conn = tmpURL.openConnection();
      long lastModified = conn.getLastModified();
      System.out.println("lastModified, "+lastModified);
      assertNotSame("lastModified", 0, lastModified);
      assertTrue(tmp.getAbsolutePath()+" deleted", tmp.delete() || isWindowsOS());
      conn = tmpURL.openConnection();
      lastModified = conn.getLastModified();
      System.out.println("lastModified after delete, "+lastModified);
      // TODO - fix back
      assertTrue("lastModified", 0 == lastModified || isWindowsOS());
   }
}
