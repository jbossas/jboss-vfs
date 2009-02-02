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
package org.jboss.virtual.plugins.context.jar;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractJarHandler.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public abstract class AbstractJarHandler extends AbstractURLHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1;

   /**
    * The jar file
    */
   private transient JarFile jar;
   /**
    * The jar entry
    */
   private transient ZipEntry entry;

   /**
    * Create a new JarHandler.
    *
    * @param context the context
    * @param parent  the parent
    * @param url     the url
    * @param jar     the jar
    * @param entry   the entry
    * @param name    the name
    * @throws IOException              for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url or vfsPath
    */
   protected AbstractJarHandler(VFSContext context, VirtualFileHandler parent, URL url, JarFile jar, ZipEntry entry, String name) throws IOException
   {
      super(context, parent, url, name);
      this.jar = jar;
      this.entry = entry;
   }

   protected String getProtocol()
   {
      return "vfsjar";
   }

   /**
    * Get the jar.
    *
    * @return the jar.
    */
   public JarFile getJar()
   {
      if (jar == null)
         throw new IllegalArgumentException("Null jar");
      return jar;
   }

   /**
    * Get the entry.
    *
    * @return jar entry
    */
   public ZipEntry getEntry()
   {
      checkClosed();
      if (entry == null)
         throw new IllegalArgumentException("Null entry");
      return entry;
   }

   /**
    * Create the URL for the entry represented by path.
    * 
    * @param parent - the parent handler
    * @param path - the simple path to the entry without any trailing '/'
    * @param isDirEntry - whether this is a directory entry
    * @return the jar entry URL
    * @throws MalformedURLException if illegal URL form
    */
   protected URL getURL(VirtualFileHandler parent, String path, boolean isDirEntry) throws MalformedURLException
   {
      StringBuilder buffer = new StringBuilder();
      try
      {
         String parentUrl = parent.toURL().toString();
         if (parent instanceof JarEntryHandler || parent instanceof SynthenticDirEntryHandler)
         {
            buffer.append(parentUrl);
         }
         else
         {
            buffer.append("jar:").append(parentUrl).append("!/");
         }

         if (buffer.charAt(buffer.length() - 1) != '/')
            buffer.append('/');
         buffer.append(path);
      }
      catch (URISyntaxException e)
      {
         // Should not happen
         throw new MalformedURLException(e.getMessage());
      }
      // Jar directory URLs must end in /
      if( isDirEntry && buffer.charAt(buffer.length() - 1) != '/')
         buffer.append('/');
      return new URL(buffer.toString());
   }

   public boolean isLeaf()
   {
      checkClosed();
      return false;
   }

   /**
    * Convert a URL into a JarFIle
    *
    * @param url the url to convert
    * @return the jar file
    * @throws IOException for any IO error
    */
   public static JarFile fromURL(URL url) throws IOException
   {
      try
      {
         URLConnection connection = openConnection(url);
         JarURLConnection jarConnection;
         if (connection instanceof JarURLConnection)
         {
            jarConnection = (JarURLConnection)connection;
         }
         else
         {
            // try wrapping it in jar:
            URL jarUrl = new URL("jar:" + url + "!/");
            jarConnection = (JarURLConnection)openConnection(jarUrl);
         }
         return jarConnection.getJarFile();
      }
      catch (IOException original)
      {
         // Fix the context of the error message
         IOException e = new IOException("Error opening jar file: " + url + " reason=" + original.getMessage());
         e.setStackTrace(original.getStackTrace());
         throw e;

      }
   }

   /**
    * Restore the jar file from the jar URL
    *
    * @param in object input string
    * @throws IOException for any IO error
    * @throws ClassNotFoundException if any error reading object 
    */
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      in.defaultReadObject();
      // Initialize the transient values
      URL jarURL = getURL();
      String jarAsString = jarURL.toString();
      if (jarAsString.startsWith("file:"))
      {
         File fp = new File(jarAsString.substring(5));
         jar = new JarFile(fp);
      }
      else
      {
         URLConnection conn = openConnection(jarURL);
         if (conn instanceof JarURLConnection)
         {
            JarURLConnection jconn = (JarURLConnection) conn;
            jar = jconn.getJarFile();
         }
         else
         {
            throw new IOException("Cannot restore from non-JarURLConnection, url: " + jarURL);
         }
      }
      handleJarFile();
   }

   /**
    * Handle jar file after read.
    * Find the real jar file, if nested.
    *
    * @throws IOException for any error
    */
   protected void handleJarFile() throws IOException
   {
      Stack<AbstractJarHandler> handlers = new Stack<AbstractJarHandler>();
      AbstractJarHandler current = this;
      while(current.getParent() instanceof AbstractJarHandler)
      {
         handlers.push(current);
         current = (AbstractJarHandler)current.getParent();
      }
      while(handlers.isEmpty() == false)
      {
         if (entry != null)
         {
            // TODO - change jar
         }
         current = handlers.pop();
         entry = jar.getEntry(current.getName());
      }
   }
}
