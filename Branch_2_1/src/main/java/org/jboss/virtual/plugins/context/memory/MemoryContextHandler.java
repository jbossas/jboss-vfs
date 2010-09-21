/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.virtual.plugins.context.memory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;


/**
 * Virtual memory context handler.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MemoryContextHandler extends AbstractURLHandler implements StructuredVirtualFileHandler
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;
   /** The entries */
   private Map<String, MemoryContextHandler> entryMap = Collections.emptyMap();
   /** The content */
   private byte[] contents;

   public MemoryContextHandler(VFSContext context, VirtualFileHandler parent, URL url, String name)
   {
      super(context, parent, url, name);
      if (parent != null && parent instanceof MemoryContextHandler)
      {
         ((MemoryContextHandler)parent).addChild(name, this);
      }
   }

   protected synchronized void addChild(String name, MemoryContextHandler child)
   {
      if (entryMap == Collections.EMPTY_MAP)
      {
         entryMap = new LinkedHashMap<String, MemoryContextHandler>();
      }
      entryMap.put(name, child);
   }
   
   boolean deleteChild(MemoryContextHandler child)
   {
      return entryMap.remove(child.getName()) != null;
   }
   
   MemoryContextHandler getDirectChild(String name)
   {
      return entryMap.get(name);
   }
   
   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return new ArrayList<VirtualFileHandler>(entryMap.values());
   }

   public boolean isLeaf()
   {
      return contents != null;
   }

   /**
    * Called by structuredFindChild
    */
   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      return entryMap.get(name);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }

   public boolean removeChild(String name) throws IOException
   {
      return entryMap.remove(name) != null;
   }

   @Override
   public boolean exists() throws IOException
   {
      return true;
   }
   
   byte[] getContents()
   {
      return contents;
   }

   public void setContents(byte[] contents)
   {
      if (entryMap.size() > 0)
      {
         throw new RuntimeException("Cannot set contents for non-leaf node");
      }
      initCacheLastModified();
      this.contents = contents;
   }
   
   @Override
   protected void initCacheLastModified()
   {
      this.cachedLastModified = System.currentTimeMillis();
   }

   @Override
   public long getSize() throws IOException
   {
      if (contents != null)
      {
         return contents.length;
      }
      return 0;
   }
   
   @Override
   public long getLastModified() throws IOException
   {
      return cachedLastModified;
   }

   public boolean isNested() throws IOException
   {
      return false;
   }

   @Override
   public InputStream openStream() throws IOException
   {
      if (contents != null)
      {
         return new ByteArrayInputStream(contents);
      }
      return new ByteArrayInputStream(new byte[0]);
   }

   @Override
   public URL toVfsUrl() throws MalformedURLException, URISyntaxException
   {
      if (getVfsUrl() == null)
      {
         if (isLeaf())
         {
            setVfsUrl(getURL());
         }
         else
         {
            String vfsString = getURL().toString(); 
            if (vfsString.endsWith("/") == false)
               setVfsUrl(new URL(vfsString + "/"));
         }
      }
      return getVfsUrl();
   }
}
