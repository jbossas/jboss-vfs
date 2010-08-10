/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.file.FileSystemContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * This test case is a dumping ground for tests that don't fit anywhere else
 * but since somebody bothered (in most cases this would be Carlo) to produce them,
 * we don't see a point in throwing them away.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class DumpTestCase extends AbstractVFSTest
{
   public DumpTestCase(String name)
   {
      super(name, true, true);
   }

   public static Test suite()
   {
      return suite(DumpTestCase.class);
   }

   public void testJBVFS122() throws IOException
   {
      URL url = getResource("/vfs/test/nested/nested.jar");
      VirtualFile root = VFS.getRoot(url);
      assertNotNull(root);

      VirtualFile child = root.getChild("complex.jar/META-INF/MANIFEST.MF");
      InputStream in = child.openStream();
      in.close();
   }

   public void testJBVFS160() throws Exception
   {
      URI uri = new URI("file:////127.0.0.1/shared");
      File file = new File("\\\\127.0.0.1\\shared");

      FileSystemContext fsc = new FileSystemContext(uri);
      VirtualFileHandler vfh = fsc.createVirtualFileHandler(null, file);
      // this doesn't test anything as we don't have shared dir
      // more of a helper for actual usage
      System.out.println(vfh);
   }
}