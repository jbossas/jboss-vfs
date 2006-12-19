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
package org.jboss.virtual.protocol.vfsfile;

import org.jboss.virtual.VFS;
import org.jboss.virtual.plugins.vfs.VirtualFileURLConnection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

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
      String file = u.toString().substring(8); // strip out vfsfile:
      URL vfsurl = null;
      String relative;
      File fp = new File(file);
      if (fp.exists())
      {
         vfsurl = fp.getParentFile().toURL();
         relative = fp.getName();
      }
      else
      {
         File curr = fp;
         relative = fp.getName();
         while ((curr = curr.getParentFile()) != null)
         {
            if (curr.exists())
            {
               vfsurl = curr.toURL();
               break;
            }
            else
            {
               relative = curr.getName() + "/" + relative;
            }
         }
      }

      if (vfsurl == null) throw new IOException("vfsfile does not exist: " + u.toString());
      return new VirtualFileURLConnection(u, vfsurl, relative);
   }

   public static void main(String[] args) throws Exception
   {
      System.setProperty("java.protocol.handler.pkgs", "org.jboss.virtual.protocol");

      //URL url = new URL("vfsfile:/c:/tmp/urlstream.java");
      //URL url = new URL("vfsfile:/C:\\jboss\\jboss-head\\build\\output\\jboss-5.0.0.Beta\\server\\default\\lib\\jboss.jar\\schema\\xml.xsd");
      URL furl = new URL("file:/c:/tmp/parent.jar");
      System.out.println("urlpath: " + furl.getPath());
      URL url = new URL("vfsfile:/c:/tmp/parent.jar/foo.jar/urlstream.java");
      InputStream is = url.openStream();
      char curr = 0;
      while (is.available() != 0)
      {
         curr = (char) is.read();
         System.out.print(curr);
      }
      is.close();

      // use a .jar file that would NEVER EVER be in my IDE's classpath
      File fp = new File("c:/jboss/geronimo-1.1.1/lib/geronimo-kernel-1.1.1.jar");
      JarFile jf = new JarFile(fp);
      Enumeration<JarEntry> entries = jf.entries();
      HashSet<String> set = new HashSet<String>();
      while (entries.hasMoreElements())
      {
         String name = entries.nextElement().getName();
         if (name.endsWith(".class")) set.add(name.replace('/', '.').substring(0, name.length() - 6));
      }
      jf.close();
      /*
URL[] urls = {new URL("vfsfile:/c:/tmp/webinf-classes.jar/classes/"),
      new URL("file:/c:/jboss/geronimo-1.1.1/lib/xstream-1.1.3.jar")};
      */
      // use a .jar file that would NEVER EVER be in my IDE's classpath
      URL[] urls = {new URL("vfsfile:/c:/tmp/wrap.jar/geronimo-kernel-1.1.1.jar"),
              new URL("file:/c:/jboss/geronimo-1.1.1/lib/xstream-1.1.3.jar")};
      URLClassLoader loader = new URLClassLoader(urls);
      for (String name : set)
      {
         System.out.println("loading: " + name);
         loader.loadClass(name);
      }
   }

}
