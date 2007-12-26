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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractStructuredJarHandler.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public class AbstractStructuredJarHandler extends AbstractJarHandler implements StructuredVirtualFileHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1;

   /**
    * The jar entries
    */
   private transient List<VirtualFileHandler> entries;
   private transient Map<String, VirtualFileHandler> entryMap;

   /**
    * Create a new JarHandler.
    *
    * @param context the context
    * @param parent  the parent
    * @param url     the url
    * @param jar     the jar
    * @param entry   the entry
    * @param name    the name
    * @throws java.io.IOException              for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url or vfsPath
    */
   protected AbstractStructuredJarHandler(VFSContext context, VirtualFileHandler parent, URL url, JarFile jar, JarEntry entry, String name) throws IOException
   {
      super(context, parent, url, jar, entry, name);
   }

   /**
    * Initialise the jar file
    *
    * @throws java.io.IOException for any error reading the jar file
    * @throws IllegalArgumentException for a null jarFile
    */
   protected void initJarFile() throws IOException
   {
      /* This cannot be checked because of serialization
      if (this.jar != null)
         throw new IllegalStateException("jarFile has already been set");
      */

      Enumeration<JarEntry> enumeration = getJar().entries();
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
            String entryName = name.substring(start, end);
            VirtualFileHandler handler = createVirtualFileHandler(parent, entry, entryName);
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
    * @throws java.io.IOException for any IO error
    */
   protected VirtualFileHandler buildParents(String parentName, Map<String, VirtualFileHandler> parentMap, JarEntry entry)
           throws IOException
   {
      VirtualFileHandler parent = this;
      String[] paths = PathTokenizer.getTokens(parentName);
      StringBuilder pathName = new StringBuilder();
      for (String path : paths)
      {
         VirtualFileHandler next;
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
            next = new SynthenticDirEntryHandler(getVFSContext(), parent, path, entry.getTime(), url);
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
         parentMap.put(pathName.toString(), next);
         parent = next;
      }
      return parent;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      if (entries == null)
         return Collections.emptyList();
      else
         return Collections.unmodifiableList(entries);
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      return structuredFindChild(path);
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
    * @throws java.io.IOException              for any error accessing the file system
    * @throws IllegalArgumentException for a null parent or entry
    */
   protected VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, JarEntry entry, String entryName)
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
            vfh = new NoCopyNestedJarHandler(context, parent, getJar(), entry, url, entryName);
         else
            vfh = NestedJarHandler.create(context, parent, getJar(), entry, url, entryName);
      }
      else
      {
         vfh = new JarEntryHandler(context, parent, getJar(), entry, entryName, url);
      }

      return vfh;
   }

   /**
    * Restore the jar file
    *
    * @param in the input stream
    * @throws IOException for any error reading the jar file
    * @throws ClassNotFoundException for any jar class finding errors
    */
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      // Initial the parent jar entries
      initJarFile();
   }
}
