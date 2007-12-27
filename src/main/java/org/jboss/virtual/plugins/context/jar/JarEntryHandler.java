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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.plugins.context.HierarchyVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * JarEntryHandler.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class JarEntryHandler extends AbstractJarHandler implements StructuredVirtualFileHandler, HierarchyVirtualFileHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   private List<VirtualFileHandler> entryChildren;
   private transient Map<String, VirtualFileHandler> entryMap;

   /**
    * Create a new JarHandler.
    *
    * @param context   the context
    * @param parent    the parent
    * @param jar       the jar file
    * @param entry     the entry
    * @param entryName the entry name
    * @param url       the url
    * @throws IOException              for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url, jar or entry
    */
   public JarEntryHandler(VFSContext context, VirtualFileHandler parent, JarFile jar, ZipEntry entry, String entryName, URL url)
         throws IOException
   {
      super(context, parent, url, jar, entry, entryName);
      try
      {
         setVfsUrl(getChildVfsUrl(entryName, entry.isDirectory()));
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected void initCacheLastModified()
   {
      // complete
   }

   @Override
   public boolean hasBeenModified() throws IOException
   {
      return false; // right now, jar entries should always 
   }

   /**
    * Add a child to an entry
    *
    * @param child the child
    */
   public void addChild(VirtualFileHandler child)
   {
      if (entryChildren == null)
         entryChildren = new ArrayList<VirtualFileHandler>();
      entryChildren.add(child);
   }

   @Override
   public long getLastModified()
   {
      return getEntry().getTime();
   }

   @Override
   public long getSize()
   {
      return getEntry().getSize();
   }

   public boolean isLeaf()
   {
      return getEntry().isDirectory() == false;
   }

   public boolean isHidden()
   {
      checkClosed();
      return false;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      if (entryChildren == null)
         return Collections.emptyList();
      return Collections.unmodifiableList(entryChildren);
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }

   @Override
   public InputStream openStream() throws IOException
   {
      return getJar().getInputStream(getEntry());
   }

   /**
    * TODO: synchronization on lazy entryMap creation
    */
   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      if (entryChildren == null)
         throw new FileNotFoundException(this + " has no children");
      if (entryMap == null)
      {
         entryMap = new HashMap<String, VirtualFileHandler>();
         for (VirtualFileHandler child : entryChildren)
            entryMap.put(child.getName(), child);
      }
      VirtualFileHandler child = entryMap.get(name);
      if (child == null)
         throw new FileNotFoundException(this + " has no child: " + name);
      return child;
   }
}
