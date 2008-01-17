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
package org.jboss.virtual.protocol.vfsmemory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.memory.MemoryContext;
import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;
import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;

/**
 * URLStreamHandler for VFS
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class Handler extends URLStreamHandler
{
   protected URLConnection openConnection(URL u) throws IOException
   {
      String host = u.getHost();
      MemoryContext ctx = MemoryContextFactory.getInstance().find(host);
      if (ctx == null)
         throw new IOException("vfs does not exist: " + u.toString());

      VirtualFile vf = ctx.getChild(ctx.getRoot(), u.getPath()).getVirtualFile();
      if (vf == null)
         throw new IOException("vfs does not exist: " + u.toString());

      return new VirtualFileURLConnection(u, vf);
   }

   public static void main(String[] args) throws Exception
   {
      System.setProperty("java.protocol.handler.pkgs", "org.jboss.virtual.protocol");
      //URL url = new URL("vfsfile:/c:/tmp/urlstream.java");
      //URL url = new URL("vfsfile:/C:\\jboss\\jboss-head\\build\\output\\jboss-5.0.0.Beta\\server\\default\\lib\\jboss.jar\\schema\\xml.xsd");
//      URL url = new URL("vfsjar:file:/c:/tmp/parent.jar!/foo.jar/urlstream.java");
      
      URL rootURL = new URL("vfsmemory://aopdomain2");
      MemoryContextFactory.getInstance().createRoot(rootURL);

      URL url = new URL("vfsmemory://aopdomain2/org/foo/Test.class");
      MemoryContextFactory.getInstance().putFile(url, new byte[] {'a', 'b', 'c'});
      URL url2 = new URL("vfsmemory://aopdomain2/org/bar/Test.class");
      MemoryContextFactory.getInstance().putFile(url2, new byte[] {'d', 'e', 'f'});
      
      System.out.println("---------");
      InputStream is = url.openStream();
      while (is.available() != 0)
      {
         System.out.print((char)is.read());
      }
      is.close();

      System.out.println("---------");
      MemoryContextFactory.getInstance().createRoot(rootURL);
      InputStream is2 = url2.openStream();
      while (is2.available() != 0)
      {
         System.out.print((char)is2.read());
      }
      is.close();
   }
}
