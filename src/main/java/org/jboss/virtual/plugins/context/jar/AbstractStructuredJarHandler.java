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
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.plugins.context.HierarchyVirtualFileHandler;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractStructuredJarHandler.
 *
 * @param <T> exact extra wrapper type
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class AbstractStructuredJarHandler<T> extends AbstractJarHandler implements StructuredVirtualFileHandler
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
   protected AbstractStructuredJarHandler(VFSContext context, VirtualFileHandler parent, URL url, JarFile jar, ZipEntry entry, String name) throws IOException
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
      Enumeration<ZipEntryWrapper<T>> enumeration = new JarEntryEnumeration(getJar().entries());
      initJarFile(enumeration);
   }

   /**
    * Initialise the jar file.
    *
    * @param enumeration jar entry enumeration
    * @throws IOException for any error
    */
   protected void initJarFile(Enumeration<ZipEntryWrapper<T>> enumeration) throws IOException
   {
      if (enumeration.hasMoreElements() == false)
      {
         entries = Collections.emptyList();
         entryMap = Collections.emptyMap();
         return;
      }

      // Go through and create a structured representation of the jar
      Map<String, VirtualFileHandler> parentMap = new HashMap<String, VirtualFileHandler>();
      List<ArrayList<ZipEntryWrapper<T>>> levelMapList = new ArrayList<ArrayList<ZipEntryWrapper<T>>>();
      entries = new ArrayList<VirtualFileHandler>();
      entryMap = new HashMap<String, VirtualFileHandler>();
      boolean trace = log.isTraceEnabled();
      while (enumeration.hasMoreElements())
      {
         ZipEntryWrapper<T> wrapper = enumeration.nextElement();
         extraWrapperInfo(wrapper);
         String[] paths = wrapper.getName().split("/");
         int depth = paths.length;
         if (depth >= levelMapList.size())
         {
            for (int n = levelMapList.size(); n <= depth; n++)
               levelMapList.add(new ArrayList<ZipEntryWrapper<T>>());
         }
         ArrayList<ZipEntryWrapper<T>> levelMap = levelMapList.get(depth);
         levelMap.add(wrapper);
         if (trace)
            log.trace("added " + wrapper.getName() + " at depth " + depth);
      }
      // Process each level to build the handlers in parent first order
      int level = 0;
      for (ArrayList<ZipEntryWrapper<T>> levels : levelMapList)
      {
         if (trace)
            log.trace("Level(" + level++ + "): " + levels);
         for (ZipEntryWrapper<T> wrapper : levels)
         {
            String name = wrapper.getName();
            int slash = wrapper.isDirectory() ? name.lastIndexOf('/', name.length() - 2) :
                    name.lastIndexOf('/', name.length() - 1);
            VirtualFileHandler parent = this;
            if (slash >= 0)
            {
               // Need to include the slash in the name to match the ZipEntry.name
               String parentName = name.substring(0, slash + 1);
               parent = parentMap.get(parentName);
               if (parent == null)
               {
                  // Build up the parent(s)
                  parent = buildParents(parentName, parentMap, wrapper);
               }
            }
            // Get the entry name without any directory '/' ending
            int start = slash + 1;
            int end = wrapper.isDirectory() ? name.length() - 1 : name.length();
            String entryName = name.substring(start, end);
            VirtualFileHandler handler = createVirtualFileHandler(parent, wrapper, entryName);
            if (wrapper.isDirectory())
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
            else if (parent instanceof HierarchyVirtualFileHandler)
            {
               HierarchyVirtualFileHandler ehandler = (HierarchyVirtualFileHandler) parent;
               ehandler.addChild(handler);
            }
         }
      }
   }

   /**
    * Handle additional information about wrapper.
    *
    * @param wrapper the zip entry wrapper
    * @throws IOException for any error
    */
   protected void extraWrapperInfo(ZipEntryWrapper<T> wrapper) throws IOException
   {
   }

   /**
    * Create any missing parents.
    *
    * @param parentName full vfs path name of parent
    * @param parentMap  initJarFile parentMap
    * @param wrapper    ZipEntryWrapper missing a parent
    * @return the VirtualFileHandler for the parent
    * @throws java.io.IOException for any IO error
    */
   protected VirtualFileHandler buildParents(String parentName, Map<String, VirtualFileHandler> parentMap, ZipEntryWrapper<T> wrapper)
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
            next = new SynthenticDirEntryHandler(getVFSContext(), parent, path, wrapper.getTime(), url);
            if (parent == this)
            {
               // This is an immeadiate child of the jar handler
               entries.add(next);
               entryMap.put(path, next);
            }
            else if (parent instanceof HierarchyVirtualFileHandler)
            {
               HierarchyVirtualFileHandler ehandler = (HierarchyVirtualFileHandler) parent;
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
    * @param wrapper  the entry wrapper
    * @param entryName - the entry name without any trailing '/'
    * @return the handler
    * @throws java.io.IOException              for any error accessing the file system
    * @throws IllegalArgumentException for a null parent or entry
    */
   protected VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, ZipEntryWrapper<T> wrapper, String entryName)
           throws IOException
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");
      if (wrapper == null)
         throw new IllegalArgumentException("Null entry wrapper");

      ZipEntry entry = wrapper.getEntry();
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

   private class JarEntryEnumeration implements Enumeration<ZipEntryWrapper<T>>
   {
      private Enumeration<JarEntry> enumeration;

      public JarEntryEnumeration(Enumeration<JarEntry> enumeration)
      {
         if (enumeration == null)
            throw new IllegalArgumentException("Null enumeration");
         this.enumeration = enumeration;
      }

      public boolean hasMoreElements()
      {
         return enumeration.hasMoreElements();
      }

      public ZipEntryWrapper<T> nextElement()
      {
         JarEntry entry = enumeration.nextElement();
         return new ZipEntryWrapper<T>(entry);
      }
   }
}
