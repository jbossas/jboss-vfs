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

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import junit.framework.Test;
import org.jboss.test.virtual.support.PatternVirtualFileVisitor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * Custom tests - ported from issues found.
 *
 * @author Ales.Justin@jboss.org
 */
public class CustomTestCase extends AbstractVFSTest
{
   public CustomTestCase(String name)
   {
      super(name);
   }

   protected CustomTestCase(String name, boolean forceCopy)
   {
      super(name, forceCopy);
   }

   public static Test suite()
   {
      return suite(CustomTestCase.class);
   }

   public void testNestedDirLookup() throws Exception
   {
      URL url = getResource("/vfs/test/spring-ear.ear");
      String urlString = url.toExternalForm();
      int p = urlString.indexOf(":/");
      url = new URL("vfszip" + urlString.substring(p) + "/lib/spring-beans.jar/org/jboss/test/spring");
      VirtualFile file = VFS.getRoot(url);
      assertNotNull(file);
      PatternVirtualFileVisitor visitor = new PatternVirtualFileVisitor();
      file.visit(visitor);
      List<String> resources = visitor.getResources();
      assertNotNull(resources);
      assertTrue("Resources empty", resources.size() > 0);
      for (String path : resources)
      {
         VirtualFile clazz = file.getChild(path);
         assertNotNull(clazz);
         assertTrue(isClass(clazz));
      }
   }

   protected boolean isClass(VirtualFile file) throws Exception
   {
      InputStream is = file.openStream();
      try
      {
         int read = is.read();
         String cafebabe = "";
         while(read >= 0 && cafebabe.length() < 8)
         {
            cafebabe += Integer.toHexString(read);
            read = is.read();
         }
         return "CAFEBABE".equalsIgnoreCase(cafebabe);
      }
      finally
      {
         is.close();
      }
   }
}