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
package org.jboss.virtual.plugins.context.file;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.context.AbstractURLHandler;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.plugins.context.StructuredVirtualFileHandler;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.LinkInfo;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * A handler for link directories.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class LinkHandler extends AbstractURLHandler implements StructuredVirtualFileHandler
{
   private static final long serialVersionUID = 1;
   /** The link information */
   private List<LinkInfo> links;
   /** The link targets */
   private HashMap<String, VirtualFileHandler> linkTargets = new HashMap<String, VirtualFileHandler>(3);

   class ParentOfLink extends AbstractURLHandler implements StructuredVirtualFileHandler
   {
      private static final long serialVersionUID = 1;

      private HashMap<String, VirtualFileHandler> children = new HashMap<String, VirtualFileHandler>(1);

      public ParentOfLink(VFSContext context, VirtualFileHandler parent, URL url, String name)
      {
         super(context, parent, url, name);
         try
         {
            setVfsUrl(new URL("vfs" + url.toString()));
         }
         catch (MalformedURLException e)
         {
            throw new RuntimeException(e);
         }
      }

      void addChild(VirtualFileHandler child, String name)
      {
         children.put(name, child);
      }

      public VirtualFileHandler createChildHandler(String name) throws IOException
      {
         return children.get(name);
      }

      public VirtualFileHandler getChild(String path) throws IOException
      {
         return structuredFindChild(path);
      }

      public boolean removeChild(String name) throws IOException
      {
         return children.remove(name) != null;
      }

      public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
      {
         return Collections.unmodifiableList(new ArrayList<VirtualFileHandler>(children.values()));
      }

      public boolean isLeaf() throws IOException
      {
         return false;
      }

      public boolean isNested() throws IOException
      {
         return false;
      }
   }

   /**
    * Create a new LinkHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param uri the uri
    * @param name the name
    * @param links the links
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url
    * @throws URISyntaxException if the uri cannot be parsed
    */
   public LinkHandler(FileSystemContext context, VirtualFileHandler parent, URI uri, String name,
         List<LinkInfo> links)
      throws IOException, URISyntaxException
   {
      // TODO: This URL is not consistent with the getName, but does point to the raw link file
      super(context, parent, uri.toURL(), name);
      this.links = links;
      setVfsUrl(new URL("vfs" + uri.toURL().toString()));
      // Create handlers for the links and add
      for(LinkInfo link : links)
      {
         String linkName = link.getName();
         if( linkName == null )
            linkName = VFSUtils.getName(link.getLinkTarget());
         if( linkName != null )
         {
            List<String> paths = PathTokenizer.getTokens(linkName);
            int n = 0;
            VirtualFileHandler linkParent = this;
            String atom;
            // Look for an existing parent
            VirtualFileHandler previous;
            for(; n < paths.size()-1; n ++)
            {
               previous = linkParent;
               atom = paths.get(n);
               linkParent = getChildPrivate(previous, atom);
               if (linkParent == null)
               {
                  linkParent = previous;
                  break;
               }
            }
            // Create any missing parents
            for(; n < paths.size()-1; n ++)
            {
               atom = paths.get(n);
               URL polURL = new URL(linkParent.toURI().toURL(), atom);
               ParentOfLink pol = new ParentOfLink(this.getVFSContext(), linkParent, polURL, atom);
               if( linkParent == this )
               {
                  linkTargets.put(atom, pol);
               }
               else if (linkParent instanceof ParentOfLink)
               {
                  ParentOfLink prevPOL = (ParentOfLink) linkParent;
                  prevPOL.addChild(pol, atom);
               }
               else
               {
                  throw new IOException("Link parent not ParentOfLink.");
               }
               linkParent = pol;
            }
               
            // Create the link handler
            atom = paths.get(n);
            VirtualFileHandler linkHandler = createLinkHandler(linkParent, atom, link.getLinkTarget());
            if( linkParent == this )
            {
               linkTargets.put(atom, linkHandler);
            }
            else if (linkParent instanceof ParentOfLink)
            {
               ParentOfLink prevPOL = (ParentOfLink) linkParent;
               prevPOL.addChild(linkHandler, atom);
            }            
         }
      }
   }

   private VirtualFileHandler getChildPrivate(VirtualFileHandler parent, String name) throws IOException
   {
      // avoid infinite recursion due to LinkHandler delegation during init phase
      if (parent instanceof LinkHandler)
         return ((LinkHandler) parent).structuredFindChild(name);
      else
         return parent.getChild(name);
   }

   public boolean isLeaf()
   {
      return false;
   }

   public boolean isNested() throws IOException
   {
      return false;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      // LinkHandler delegation: if configuration has changed, delegate to properly configured LinkHandler
      VirtualFileHandler upToDateHandler = getParent().getChild(getName());
      if (upToDateHandler != this)
         return upToDateHandler.getChildren(ignoreErrors);
      else
         return new ArrayList<VirtualFileHandler>(linkTargets.values());
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      return linkTargets.get(name);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      // LinkHandler delegation: if configuration has changed, delegate to properly configured LinkHandler
      VirtualFileHandler upToDateHandler = getParent().getChild(getName());
      if (upToDateHandler != this)
         return upToDateHandler.getChild(path);
      else
         return structuredFindChild(path);
   }

   public boolean removeChild(String name) throws IOException
   {
      // LinkHandler delegation: if configuration has changed, delegate to properly configured LinkHandler
      VirtualFileHandler upToDateHandler = getParent().getChild(getName());
      if (upToDateHandler != this)
         return upToDateHandler.removeChild(name);
      else
         return linkTargets.remove(name) != null;
   }

   @Override
   protected void doClose()
   {
      super.doClose();
      links.clear();
   }
   
   protected VirtualFileHandler createLinkHandler(VirtualFileHandler parent, String name, URI linkURI)
      throws IOException
   {
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(linkURI);
      VFSContext context = factory.getVFS(linkURI);
      VirtualFileHandler rootHandler = context.getRoot();
      // Wrap the handler in a delegate so we can change the parent and name
      // TODO: if the factory caches contexts the root handler may not point to the link
      return new DelegatingHandler(this.getVFSContext(), parent, name, rootHandler);
   }
}
