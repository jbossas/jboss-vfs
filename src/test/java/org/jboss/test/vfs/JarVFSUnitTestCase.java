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
package org.jboss.test.vfs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.vfs.VirtualFile;
import org.junit.Assert;

/**
 * Tests of the VFS implementation
 *
 * @author ales.justin@jboss.org
 */
public class JarVFSUnitTestCase extends AbstractVFSTest
{
   public JarVFSUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(JarVFSUnitTestCase.class);
   }

   public void testDuplicateName() throws Throwable
   {
      VirtualFile jar = getVirtualFile("/vfs/test/dup.jar");
      recursiveMount(jar);

      VirtualFile lower = jar.getChild("org/jboss/acme/Dummy.class");
      Assert.assertNotNull(lower);
      Assert.assertTrue(lower.exists());
      VirtualFile upper = jar.getChild("org/jboss/acme/DuMMy.class");
      Assert.assertNotNull(upper);
      Assert.assertTrue(upper.exists());
      String ll = readLine(lower);
      String ul = readLine(upper);
      Assert.assertFalse("Lines match", ll.equals(ul));
   }

   static String readLine(VirtualFile file) throws Throwable
   {
      InputStream is = file.openStream();
      try
      {
         return new BufferedReader(new InputStreamReader(is)).readLine();
      }
      finally
      {
         is.close();
      }
   }
}
