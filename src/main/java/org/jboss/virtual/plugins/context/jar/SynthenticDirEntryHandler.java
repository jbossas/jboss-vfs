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
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * SynthenticDirEntryHandler represents non-existent directory jar entry.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class SynthenticDirEntryHandler extends AbstractURLHandler
   implements StructuredVirtualFileHandler
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The jar file */
   private long lastModified;
   private transient List<VirtualFileHandler> entryChildren;
   private transient Map<String, VirtualFileHandler> entryMap;
   
   /**
    * Create a new SynthenticDirEntryHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param entryName - the simple name for the dir
    * @param lastModified the timestamp for the dir
    * @param url the full url
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url, jar or entry
    */
   public SynthenticDirEntryHandler(VFSContext context, VirtualFileHandler parent,
      String entryName, long lastModified, URL url)
      throws IOException
   {
      super(context, parent, url, entryName);
      try
      {
         URL parentVfsUrl = parent.toVfsUrl();
         String vfsParentUrl = parentVfsUrl.toString();
         if (vfsParentUrl.endsWith("/"))
         {
            vfsUrl = new URL(vfsParentUrl + entryName);
         }
         else
         {
            vfsUrl = new URL(vfsParentUrl + "/" + entryName + "/");
         }
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
      this.lastModified = lastModified;
   }

   /**
    * Add a child to an entry
    * @param child
    */
   public synchronized void addChild(VirtualFileHandler child)
   {
      if( entryChildren == null )
         entryChildren = new ArrayList<VirtualFileHandler>();
      entryChildren.add(child);
      if( entryMap != null )
         entryMap.put(child.getName(), child);
   }

   @Override
   public long getLastModified()
   {
      return lastModified;
   }

   @Override
   public long getSize()
   {
      return 0;
   }

   /**
    * TODO: removing the entry/jar that resulted in this needs
    * to be detected.
    */
   public boolean exists() throws IOException
   {
      return true;
   }

   public boolean isLeaf()
   {
      return false;
   }

   public boolean isHidden()
   {
      checkClosed();
      return false;
   }

   @Override
   public InputStream openStream() throws IOException
   {
      throw new IOException("Directories cannot be opened");
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      List<VirtualFileHandler> children = entryChildren;
      if( entryChildren == null )
         children = Collections.emptyList();
      return children;
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      return super.structuredFindChild(path);
   }

   /**
    * Create a child handler for the given name. This looks to the entryMap
    * for an existing child.
    * @param name - the simple name of an immeadiate child.
    * @return the VirtualFileHandler previously added via addChild.
    * @throws IOException - thrown if there are no children or the
    *  name does not match a child
    */
   public synchronized VirtualFileHandler createChildHandler(String name)
      throws IOException
   {
      if( entryChildren == null )
         throw new FileNotFoundException(this+" has no children");
      if( entryMap == null )
      {
         entryMap = new HashMap<String, VirtualFileHandler>();
         for(VirtualFileHandler child : entryChildren)
            entryMap.put(child.getName(), child);
      }
      VirtualFileHandler child = entryMap.get(name);
      if( child == null )
         throw new FileNotFoundException(this+" has no child: "+name);
      return child;
   }

}
