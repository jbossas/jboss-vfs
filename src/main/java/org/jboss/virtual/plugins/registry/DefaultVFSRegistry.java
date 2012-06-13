/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.virtual.plugins.registry;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.TempInfo;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextConstraints;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.cache.VFSCache;
import org.jboss.virtual.spi.cache.VFSCacheFactory;
import org.jboss.virtual.spi.registry.VFSRegistry;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Default vfs registry.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class DefaultVFSRegistry extends VFSRegistry
{
   /** Do we force canonical lookup */
   private static boolean forceCanonical;
   private static Logger log = Logger.getLogger(DefaultVFSRegistry.class);

   private Map<String,String> pathAliases = new HashMap<String,String>();

   static
   {
      forceCanonical = AccessController.doPrivileged(new CheckForceCanonical());

      if (forceCanonical)
         log.info("VFS force canonical lookup is enabled.");
   }

   /**
    * Get vfs cache.
    *
    * @return the vfs cache
    */
   protected VFSCache getCache()
   {
      return VFSCacheFactory.getInstance();
   }

   /**
    * Is the vfs context cacheable.
    *
    * @param context the vfs context
    * @return true if context is cacheable, false otherwise
    */
   protected boolean isCacheable(VFSContext context)
   {
      Set<VFSContextConstraints> constraints = context.getConstraints();
      return constraints != null && constraints.contains(VFSContextConstraints.CACHEABLE);
   }

   public void addContext(VFSContext context)
   {
      if (isCacheable(context))
      {
         getCache().putContext(context);
      }
   }

   public void removeContext(VFSContext context)
   {
      if (isCacheable(context))
      {
         getCache().removeContext(context);
      }
   }

   /**
    * Retrieve a VFSContext and canonicalized URI if jboss.vfs.forceCanonical is enabled.
    * Walk backward up the path canonicalizing until we find something
    * that is in permanentRoots.  This is essentially a way to figure out
    * what the original path was for the permanentRoot before it got mangled
    * by the URLEditor.
    *
    * @param uri the uri to resolve
    * @return If jboss.vfs.forceCanonical is true, a uri with its first part replaced
    * with a permanent root if possible along with its VFSContext if possible.
    * If jboss.vfs.forceCanonical=false, just try to find the VFSContext without
    * canonicalizing the original URI and return both.
    * @throws IOException for any IO or URI error.
    */
   protected URIResolutionResult resolveURI(URI uri) throws IOException
   {
      //first just try to find it
      VFSContext ctx = getCache().findContext(uri);
      if(ctx != null)
      {
         if(log.isTraceEnabled())
            log.trace("Context found immediately for: " + uri.toString() + " -> " + ctx.toString());

         return new URIResolutionResult(ctx, uri);
      }

      if(forceCanonical)
      {
         String path = VFSUtils.stripProtocol(uri);

         //we weren't lucky.. check to see if we've already done the walking routine to figure out
         //the permanent root.  If we have, build a URI with the permanent root and the relative path
         //of the original URI minus whatever symlink infested stuff it had as parents.
         File file = new File(path);
         try
         {
            URI fixedURI = null;
            for(String key: pathAliases.keySet())
            {
               if(path.startsWith(key))
               {
                  String relative = path.substring(key.length());

                  String alias = pathAliases.get(key);
                  fixedURI = new URI(uri.getScheme(), uri.getHost(),  alias + relative, uri.getQuery(), uri.getFragment());

                  if(log.isTraceEnabled())
                     log.trace("Found aliased context: " + key + " -> " + pathAliases.get(key));

                  return new URIResolutionResult(getCache().findContext(fixedURI), fixedURI);
               }
            }

            //it wasn't aliased, so try to figure out if a corresponding permanentRoot exists
            String relative = "";
            while(ctx == null && file != null)
            {
               URI toFind = new URI(uri.getScheme(), uri.getHost(), file.getCanonicalPath(), uri.getQuery(), uri.getFragment());

               if(log.isTraceEnabled())
                  log.trace("Trying to find in permanentRoots: " + toFind.toString());

               ctx = getCache().findContext(toFind);
               if(ctx == null)
               {
                  if(relative.length() == 0)
                  {
                     relative=file.getName();
                  }
                  else
                  {
                     relative = file.getName() + "/" + relative;
                  }
                  file = file.getParentFile();
               }
            }

            //we found one, so store it for later
            if(ctx != null)
            {
               if(log.isTraceEnabled())
                  log.trace("Found: " + file.getCanonicalPath());

               pathAliases.put(file.getPath(),file.getCanonicalPath());
               fixedURI = new URI(uri.getScheme(), uri.getHost(), file.getCanonicalPath() + "/" + relative, uri.getQuery(), uri.getFragment());
               return new URIResolutionResult(getCache().findContext(fixedURI), fixedURI);
            }
         }
         catch (URISyntaxException e)
         {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
         }

      }
      //we did all we could - let it fall through with the original URI
      return new URIResolutionResult(null, uri);
   }

   public VFSContext getContext(URI uri) throws IOException
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      URIResolutionResult resolutionResult = resolveURI(uri);

      if(log.isTraceEnabled())
         log.trace("Getting context in getContext(): " + resolutionResult.getURI().toString());

      VFSContext context = resolutionResult.getContext();
      if (context != null)
      {
         String relativePath = VFSUtils.getRelativePath(context, resolutionResult.getURI());
         if (relativePath.length() == 0)
            return context;
      }
      return null;
   }

   public VirtualFile getFile(URI uri) throws IOException
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      URIResolutionResult resolutionResult = resolveURI(uri);

      if(log.isTraceEnabled())
         log.trace("Getting context in getFile(): " + resolutionResult.getURI().toString());

      VFSContext context = resolutionResult.getContext();
      if (context != null)
      {
         String relativePath = VFSUtils.getRelativePath(context, resolutionResult.getURI());

         TempInfo ti = context.getFurthestParentTemp(relativePath);
         if (ti != null)
         {
            String path = ti.getPath();
            String subpath = relativePath.substring(path.length());
            VirtualFileHandler child = findHandler(ti.getHandler(), subpath, true);
            if (child != null)
                  return child.getVirtualFile();
         }

         VirtualFileHandler root = context.getRoot();
         VirtualFileHandler child = findHandler(root, relativePath, false);
         return child.getVirtualFile();
      }
      return null;
   }

   /**
    * Find the handler.
    *
    * @param root the root
    * @param path the path
    * @return child handler
    * @param allowNotFound do we allow not found
    * @throws IOException for any error
    */
   protected VirtualFileHandler findHandler(VirtualFileHandler root, String path, boolean allowNotFound) throws IOException
   {
      VirtualFileHandler child = root.getChild(path);
      if (child == null && allowNotFound == false)
      {
         List<VirtualFileHandler> children = root.getChildren(true);
         throw new IOException("Child not found " + path + " for " + root + ", available children: " + children);
      }
      return child;
   }

   /**
    * <tt>PriviligedAction</tt> class for checking a system property
    */
   private static class CheckForceCanonical implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_CANONICAL, "false");
         return Boolean.valueOf(forceString);
      }
   }

   private static class URIResolutionResult
   {
      private VFSContext context;
      private URI uri;

      public URIResolutionResult(final VFSContext context, final URI uri)
      {
         this.context = context;
         this.uri = uri;
      }

      public VFSContext getContext()
      {
         return this.context;
      }

      public URI getURI()
      {
         return this.uri;
      }
   }

}
