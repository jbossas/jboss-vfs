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

import java.net.URI;
import java.net.URL;
import java.util.Map;

import junit.framework.Test;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;

/**
 * Test the query in url/uri.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class PathQueryTestCase extends AbstractVFSTest
{
   public PathQueryTestCase(String s)
   {
      super(s);
   }

   public static Test suite()
   {
      return suite(PathQueryTestCase.class);
   }

   protected void assertOption(String name) throws Throwable
   {
      URL url = getResource("/vfs/context/" + name);
      URI uri = new URI(url.toExternalForm() + "?foobar=qwert&useCopyJarHandler=true");
      VirtualFile vf = VFS.getRoot(uri);
      assertOption(vf, "foobar", "qwert");
      assertOption(vf, "useCopyJarHandler", "true");
   }

   protected void assertOption(VirtualFile vf, String key, String value)
   {
      VFSContext context = vf.getHandler().getVFSContext();
      assertNotNull(context);
      Map<String, String> options = context.getOptions();
      assertNotNull(options);
      assertEquals(value, options.get(key));
   }

   public void testURL() throws Throwable
   {
      assertOption("jar/simple.jar");
      assertOption("file/simple/child");
   }
}
