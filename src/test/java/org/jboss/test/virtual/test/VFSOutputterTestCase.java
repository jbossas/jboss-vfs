/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
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

import junit.framework.Test;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

/**
 * Test vfs utils output.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class VFSOutputterTestCase extends AbstractVFSTest
{
   public VFSOutputterTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(VFSOutputterTestCase.class);
   }

   public void testOutputJarContents() throws Exception
   {
      URL url = getResource("/vfs/test/jar1.jar");
      VirtualFile jar = VFS.getRoot(url);
      
      String output = VFSUtils.outputContents(jar);
      
      String expected = "jar1.jar/\n" +
      		"  META-INF/\n" +
      		"    MANIFEST.MF\n" +
      		"  org/\n" +
      		"    jboss/\n" +
      		"      test/\n" +
      		"        vfs/\n" +
            "          support/\n" +
            "            jar1/\n" +
            "              ClassInJar1$InnerClass.class\n" +
            "              ClassInJar1.class";      

      assertTrue("expected:\n" + expected + "\nwas:\n" + output, output.contains(expected));
   }
}
