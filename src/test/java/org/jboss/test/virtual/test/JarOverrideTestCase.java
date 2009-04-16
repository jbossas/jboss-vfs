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
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * Test jar file override.
 *
 * @author ales.justin@jboss.org
 */
public class JarOverrideTestCase extends AbstractVFSTest
{
   public JarOverrideTestCase(String name)
   {
      super(name, true, false);
   }

   public static Test suite()
   {
      return suite(JarOverrideTestCase.class);
   }

   @SuppressWarnings("deprecation")
   public void testOverride() throws Exception
   {
      URL topURL = getResource("/vfs/test/nested/nested.jar");
      URI topURI = topURL.toURI();
      File source = new File(topURI);

      // create temp so we can override it, not worrying about other tests
      File tempDest = File.createTempFile("nested", "-temp.jar");
      tempDest.deleteOnExit();
      copy(source, tempDest);
      long ts1 = tempDest.lastModified();

      VirtualFile nested = VFS.createNewRoot(tempDest.toURI());
      VirtualFile complex = nested.findChild("complex.jar");
      // mock war unjaring
      VirtualFile unjared = VFSUtils.unjar(complex);
      assertNotSame(complex, unjared);

      // override
      copy(source, tempDest);
      long ts2 = tempDest.lastModified();
      // was it really overridden
      assertFalse(ts1 == ts2);

      // undeploy
      nested.cleanup();
      complex.cleanup();
      // check we really deleted unjared temp
      URL url = VFSUtils.getRealURL(unjared);
      File unjaredTemp = new File(url.toURI());
      assertFalse(unjaredTemp.exists());

      // 2nd pass to mock
      complex = nested.findChild("complex.jar");
      // this is where JBAS-6715 fails
      unjared = VFSUtils.unjar(complex);
      assertNotSame(complex, unjared);
   }

   protected void copy(File source, File dest) throws Exception
   {
      VFSUtils.copyStreamAndClose(new FileInputStream(source), new FileOutputStream(dest));
   }
}