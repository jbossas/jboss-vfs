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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.File;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractJarHandler.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class AbstractJarHandler extends AbstractURLHandler
        implements StructuredVirtualFileHandler
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
    * The jar entries
    */
   private transient List<VirtualFileHandler> entries;
   private transient Map<String, VirtualFileHandler> entryMap;

   /**
    * Get a jar entry name
    *
    * @param entry the entry
    * @return the name
    * @throws IllegalArgumentException for a null entry
    */
   protected static String getEntryName(JarEntry entry)
   {
      if (entry == null)
         throw new IllegalArgumentException("Null entry");
      return entry.getName();
   }

   /**
    * Create a new JarHandler.
    *
    * @param context the context
    * @param parent  the parent
    * @param url     the url
    * @param name    the name
    * @throws IOException              for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url or vfsPath
    */
   protected AbstractJarHandler(VFSContext context, VirtualFileHandler parent, URL url, String name) throws IOException
   {
      super(context, parent, url, name);
   }

   /**
    * Get the jar.
    *
    * @return the jar.
    */
   public JarFile getJar()
   {
      return jar;
   }

   /**
    * Initialise the jar file
    *
    * @param jarFile the jar file
    * @throws IOException              for any error reading the jar file
    * @throws IllegalArgumentException for a null jarFile
    */
   protected void initJarFile(JarFile jarFile) throws IOException
   {
      /* This cannot be checked because of serialization
      if (this.jar != null)
         throw new IllegalStateException("jarFile has already been set");
      */

      this.jar = jarFile;

      Enumeration<JarEntry> enumeration = jar.entries();
      if (enumeration.hasMoreElements() == false)
      {
         entries = Collections.emptyList();
         entryMap = Collections.emptyMap();
         return;
      }

      // Go through and create a structured representation of the jar
      Map<String, VirtualFileHandler> parentMap = new HashMap<String, VirtualFileHandler>();
      ArrayList<ArrayList<JarEntry>> levelMapList = new ArrayList<ArrayList<JarEntry>>();
      entries = new ArrayList<VirtualFileHandler>();
      entryMap = new HashMap<String, VirtualFileHandler>();
      boolean trace = log.isTraceEnabled();
      while (enumeration.hasMoreElements())
      {
         JarEntry entry = enumeration.nextElement();
         String[] paths = entry.getName().split("/");
         int depth = paths.length;
         if (depth >= levelMapList.size())
         {
            for (int n = levelMapList.size(); n <= depth; n++)
               levelMapList.add(new ArrayList<JarEntry>());
         }
         ArrayList<JarEntry> levelMap = levelMapList.get(depth);
         levelMap.add(entry);
         if (trace)
            log.trace("added " + entry.getName() + " at depth " + depth);
      }
      // Process each level to build the handlers in parent first order
      int level = 0;
      for (ArrayList<JarEntry> levels : levelMapList)
      {
         if (trace)
            log.trace("Level(" + level++ + "): " + levels);
         for (JarEntry entry : levels)
         {
            String name = entry.getName();
            int slash = entry.isDirectory() ? name.lastIndexOf('/', name.length() - 2) :
                    name.lastIndexOf('/', name.length() - 1);
            VirtualFileHandler parent = this;
            String entryName = name;
            if (slash >= 0)
            {
               // Need to include the slash in the name to match the JarEntry.name
               String parentName = name.substring(0, slash + 1);
               parent = parentMap.get(parentName);
               if (parent == null)
               {
                  // Build up the parent(s) 
                  parent = buildParents(parentName, parentMap, entry);
               }
            }
            // Get the entry name without any directory '/' ending
            int start = slash + 1;
            int end = entry.isDirectory() ? name.length() - 1 : name.length();
            entryName = name.substring(start, end);
            VirtualFileHandler handler = this.createVirtualFileHandler(parent, entry, entryName);
            if (entry.isDirectory())
            {
               parentMap.put(name, handler);
               if (trace)
                  log.trace("Added parent: " + name);
            }
            if (parent == this)
            {
               // This is an immeadiate child of the jar handler
               entries.add(handler);
               entryMap.put(entryName, handler);
            }
            else if (parent instanceof JarEntryHandler)
            {
               // This is a child of the jar entry handler
               JarEntryHandler ehandler = (JarEntryHandler) parent;
               ehandler.addChild(handler);
            }
            else if (parent instanceof SynthenticDirEntryHandler)
            {
               // This is a child of the jar entry handler
               SynthenticDirEntryHandler ehandler = (SynthenticDirEntryHandler) parent;
               ehandler.addChild(handler);
            }
         }
      }
   }

   /**
    * Create any missing parents.
    *
    * @param parentName full vfs path name of parent
    * @param parentMap  initJarFile parentMap
    * @param entry      JarEntry missing a parent
    * @return the VirtualFileHandler for the parent
    * @throws IOException
    */
   protected VirtualFileHandler buildParents(String parentName,
                                             Map<String, VirtualFileHandler> parentMap, JarEntry entry)
           throws IOException
   {
      VirtualFileHandler parent = this;
      String[] paths = PathTokenizer.getTokens(parentName);
      StringBuilder pathName = new StringBuilder();
      for (String path : paths)
      {
         VirtualFileHandler next = null;
         pathName.append(path);
         pathName.append('/');
         try
         {
            next = parent.findChild(path);
         }
         catch (IOException e)
         {
            // Create a synthetic parent
            URL url = getURL(parent, path, true);
            next = new SynthenticDirEntryHandler(getVFSContext(), parent, path,
                    entry.getTime(), url);
            parentMap.put(pathName.toString(), next);
            if (parent == this)
            {
               // This is an immeadiate child of the jar handler
               entries.add(next);
               entryMap.put(path, next);
            }
            else if (parent instanceof JarEntryHandler)
            {
               // This is a child of the jar entry handler
               JarEntryHandler ehandler = (JarEntryHandler) parent;
               ehandler.addChild(next);
            }
            else if (parent instanceof SynthenticDirEntryHandler)
            {
               // This is a child of the jar entry handler
               SynthenticDirEntryHandler ehandler = (SynthenticDirEntryHandler) parent;
               ehandler.addChild(next);
            }
         }
         parent = next;
      }
      return parent;
   }

   /**
    * Create the URL for the entry represented by path.
    * 
    * @param parent - the parent handler
    * @param path - the simple path to the entry without any trailing '/'
    * @param isDirEntry - whether this is a directory entry
    * @return the jar entry URL
    * @throws MalformedURLException
    */
   protected URL getURL(VirtualFileHandler parent, String path, boolean isDirEntry)
           throws MalformedURLException
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
      URL url = new URL(buffer.toString());
      return url;
   }

   protected void doClose()
   {
      /* TODO Figure out why this breaks things randomly
      try
      {
         if (jar != null)
            jar.close();
      }
      catch (IOException ignored)
      {
      }
      */
   }

   public boolean isLeaf()
   {
      checkClosed();
      return false;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      return entries;
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      return super.structuredFindChild(path);
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      VirtualFileHandler child = entryMap.get(name);
      if (child == null)
         throw new FileNotFoundException(this + " has no child: " + name);
      return child;
   }

   /**
    * Create a new virtual file handler
    *
    * @param parent the parent
    * @param entry  the entry
    * @param entryName - the entry name without any trailing '/'
    * @return the handler
    * @throws IOException              for any error accessing the file system
    * @throws IllegalArgumentException for a null parent or entry
    */
   protected VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent,
         JarEntry entry, String entryName)
           throws IOException
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");
      if (entry == null)
         throw new IllegalArgumentException("Null entry");

      URL url = getURL(parent, entryName, entry.isDirectory());
      VFSContext context = parent.getVFSContext();

      VirtualFileHandler vfh;
      if (JarUtils.isArchive(entry.getName()))
      {
         String flag = context.getOptions().get("useNoCopyJarHandler");
         boolean useNoCopyJarHandler = Boolean.valueOf(flag);

         if (useNoCopyJarHandler)
            vfh = new NoCopyNestedJarHandler(context, parent, jar, entry, url);
         else
            vfh = NestedJarHandler.create(context, parent, jar, entry, url, entryName);
      }
      else
      {
         vfh = new JarEntryHandler(context, parent, jar, entry, entryName, url);
      }

      return vfh;
   }

   /**
    * Convert a URL into a JarFIle
    *
    * @param url the url to convert
    * @return the jar file
    * @throws IOException
    */
   public static JarFile fromURL(URL url) throws IOException
   {
      try
      {
         URLConnection connection = url.openConnection();
         JarURLConnection jarConnection;
         if (connection instanceof JarURLConnection)
         {
            jarConnection = (JarURLConnection)connection;
         }
         else
         {
            // try wrapping it in jar:
            URL jarUrl = new URL("jar:" + url + "!/");
            jarConnection = (JarURLConnection)jarUrl.openConnection();
         }
         jarConnection.setUseCaches(false);
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
    * @param in
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private void readObject(ObjectInputStream in)
           throws IOException, ClassNotFoundException
   {
      in.defaultReadObject();
      // Initialize the transient values
      URL jarURL = super.getURL();
      String jarAsString = jarURL.toString();
      if (jarAsString.startsWith("file:"))
      {
         File fp = new File(jarAsString.substring(5));
         jar = new JarFile(fp);
      }
      else
      {
         URLConnection conn = jarURL.openConnection();
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
   }

}
