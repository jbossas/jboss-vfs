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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.virtual.plugins.context.HierarchyVirtualFileHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * A nested jar contents implementation used to represent a jar within a jar.
 *
 * @author Ales.Justin@jboss.org
 * @author Scott.Stark@jboss.org
 */
public class JarEntryContents extends AbstractJarHandler implements StructuredVirtualFileHandler, HierarchyVirtualFileHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   private URL entryURL;
   private byte[] contents;
   private boolean isJar;
   private NestedJarFromStream njar;
   private InputStream openStream;

   private List<VirtualFileHandler> entryChildren;
   private transient Map<String, VirtualFileHandler> entryMap;

   JarEntryContents(VFSContext context, VirtualFileHandler parent, ZipEntry entry, String entryName, URL jarURL, URL entryURL, byte[] contents)
           throws IOException
   {
      super(context, parent, jarURL, null, entry, entryName);
      try
      {
         setPathName(getChildPathName(entryName, false));
         setVfsUrl(getChildVfsUrl(entryName, false));
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      this.entryURL = entryURL;
      this.isJar = JarUtils.isArchive(getName());
      this.contents = contents;
   }

   protected void initCacheLastModified()
   {
   }

   /**
    * Add a child to an entry
    *
    * @param child the child
    */
   public synchronized void addChild(VirtualFileHandler child)
   {
      if (entryChildren == null)
         entryChildren = new ArrayList<VirtualFileHandler>();
      entryChildren.add(child);
      if (entryMap != null)
         entryMap.put(child.getName(), child);
   }

   /**
    * TODO: removing the entry/jar that resulted in this needs
    * to be detected.
    */
   public boolean exists() throws IOException
   {
      return true;
   }

   public boolean isHidden() throws IOException
   {
      return false;
   }

   byte[] getContents()
   {
      return contents;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      List<VirtualFileHandler> children;

      if (isJar)
      {
         initNestedJar();
         children = njar.getChildren(ignoreErrors);
      }
      else
      {
         children = entryChildren;
      }

      if (children == null)
         return Collections.emptyList();
      else
         return Collections.unmodifiableList(children);
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if ("".equals(path))
         return this;

      if (isJar)
      {
         initNestedJar();
         return njar.findChild(path);
      }
      else if (getEntry().isDirectory())
      {
         return structuredFindChild(path);
      }
      throw new FileNotFoundException("JarEntryContents(" + getName() + ") has no children");
   }

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

   // Convience attribute accessors
   public long getLastModified()
   {
      return getEntry().getTime();
   }

   public long getSize()
   {
      return getEntry().getSize();
   }

   public boolean isLeaf()
   {
      return isJar == false && getEntry().isDirectory() == false;
   }

   // Stream accessor
   public synchronized InputStream openStream() throws IOException
   {
      initNestedJar();
      if (njar != null)
         openStream = njar.openStream();
      else
         openStream = new ByteArrayInputStream(contents);
      return openStream;
   }

   public synchronized void close()
   {
      if (openStream != null)
      {
         try
         {
            openStream.close();
         }
         catch (IOException e)
         {
            log.error("close error", e);
         }
         openStream = null;
      }
   }

   public URI toURI() throws URISyntaxException
   {
      return entryURL.toURI();
   }

   public String toString()
   {
      StringBuffer tmp = new StringBuffer(super.toString());
      tmp.append('[');
      tmp.append("name=");
      tmp.append(getName());
      tmp.append(",size=");
      tmp.append(getSize());
      tmp.append(",time=");
      tmp.append(getLastModified());
      tmp.append(",URI=");
      try
      {
         tmp.append(toURI());
      }
      catch (URISyntaxException ignored)
      {
      }
      tmp.append(']');
      return tmp.toString();
   }

   protected synchronized void initNestedJar() throws IOException
   {
      if (isJar && njar == null)
      {
         ByteArrayInputStream bais = new ByteArrayInputStream(contents);
         ZipInputStream zis = new ZipInputStream(bais);
         njar = new NestedJarFromStream(getVFSContext(), this, zis, entryURL, null, getEntry(), getName());
      }
   }
}
