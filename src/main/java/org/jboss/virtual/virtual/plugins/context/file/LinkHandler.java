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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
public class LinkHandler extends AbstractURLHandler
   implements StructuredVirtualFileHandler
{
   private static final long serialVersionUID = 1;
   /** The link information */
   private List<LinkInfo> links;
   private HashMap<String, VirtualFileHandler> linkTargets =
      new HashMap<String, VirtualFileHandler>(3);

   class ParentOfLink extends AbstractURLHandler
      implements StructuredVirtualFileHandler
   {
      private static final long serialVersionUID = 1;
      private HashMap<String, VirtualFileHandler> children = 
         new HashMap<String, VirtualFileHandler>(1);

      public ParentOfLink(VFSContext context, VirtualFileHandler parent, URL url, String name)
      {
         super(context, parent, url, name);
         try
         {
            this.vfsUrl = new URL("vfs" + url.toString());
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
      public VirtualFileHandler findChild(String path) throws IOException
      {
         return structuredFindChild(path);
      }

      public VirtualFileHandler createChildHandler(String name) throws IOException
      {
         return children.get(name);
      }

      public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
      {
         return null;
      }

      public boolean isLeaf() throws IOException
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
      this.vfsUrl = new URL("vfs" + uri.toURL().toString());
      // Create handlers for the links and add
      for(LinkInfo link : links)
      {
         String linkName = link.getName();
         if( linkName == null )
            linkName = VFSUtils.getName(link.getLinkTarget());
         if( linkName != null )
         {
            String[] paths = PathTokenizer.getTokens(linkName);
            int n = 0;
            VirtualFileHandler linkParent = this;
            String atom;
            // Look for an existing parent           
            for(; n < paths.length-1; n ++)
            {
               atom = paths[n];
               try
               {
                  linkParent = linkParent.findChild(atom);
               }
               catch(IOException e)
               {
                  break;
               }
            }
            // Create any missing parents
            for(; n < paths.length-1; n ++)
            {
               atom = paths[n];
               URL polURL = new URL(linkParent.toURI().toURL(), atom);
               ParentOfLink pol = new ParentOfLink(this.getVFSContext(), linkParent, polURL, atom);
               if( linkParent == this )
               {
                  linkTargets.put(atom, pol);
               }
               else
               {
                  ParentOfLink prevPOL = (ParentOfLink) linkParent;
                  prevPOL.addChild(pol, atom);
               }
               linkParent = pol;
            }
               
            // Create the link handler
            atom = paths[n];
            VirtualFileHandler linkHandler = createLinkHandler(linkParent, atom, link.getLinkTarget());
            if( linkParent == this )
            {
               linkTargets.put(atom, linkHandler);
            }
            else
            {
               ParentOfLink prevPOL = (ParentOfLink) linkParent;
               prevPOL.addChild(linkHandler, atom);
            }            
         }
      }
   }

   public boolean isLeaf()
   {
      return false;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return new ArrayList<VirtualFileHandler>(linkTargets.values());
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }
   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      VirtualFileHandler handler = linkTargets.get(name);
      if( handler == null )
      {
         throw new FileNotFoundException("Failed to find link for: "+name+", parent: "+this);
      }
      return handler;
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
