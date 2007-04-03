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
package org.jboss.virtual.plugins.context.vfs;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;
import org.jboss.virtual.plugins.vfs.helpers.SuffixesExcludeFilter;
import org.jboss.virtual.spi.VirtualFileHandler;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.List;

/**
 * comment
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class AssembledDirectory extends VirtualFile
{
   private AssembledDirectoryHandler directory;

   public AssembledDirectory(VirtualFileHandler handler)
   {
      super(handler);
      directory = (AssembledDirectoryHandler) handler;
   }

   public void addClass(Class clazz)
   {
      addClass(clazz.getName(), clazz.getClassLoader());
   }

   public void addClass(String className)
   {
      addClass(className, Thread.currentThread().getContextClassLoader());
   }

   public void addClass(String className, ClassLoader loader)
   {
      String resource = className.replace('.', '/') + ".class";
      URL url = loader.getResource(resource);
      if (url == null) throw new RuntimeException("Could not find resource: " + resource);
      AssembledDirectory p = mkdirs(resource);
      p.addResource(resource, loader);
   }

   /**
    * Make any directories for the give path to a file.
    *
    * @param path must be a path to a file as last element in path does not have a directory created
    * @return directory file will live in
    */
   public AssembledDirectory mkdirs(String path)
   {
      String[] pkgs = path.split("/");
      AssembledDirectoryHandler dir = directory;
      for (int i = 0; i < pkgs.length - 1; i++)
      {
         AssembledDirectoryHandler next = (AssembledDirectoryHandler) dir.getChild(pkgs[i]);
         if (next == null)
         {
            try
            {
               next = new AssembledDirectoryHandler((AssembledContext) dir.getVFSContext(), dir, pkgs[i]);
            }
            catch (IOException e)
            {
               throw new RuntimeException(e);
            }
            dir.addChld(next);
         }
         dir = next;
      }
      AssembledDirectory p = (AssembledDirectory) dir.getVirtualFile();
      return p;
   }

   public void addResources(Class baseResource, String[] includes, String[] excludes)
   {
      String resource = baseResource.getName().replace('.', '/') + ".class";
      addResources(resource, includes, excludes);
   }

   public void addResources(String baseResource, final String[] includes, final String[] excludes)
   {
      addResources(baseResource, includes, excludes, Thread.currentThread().getContextClassLoader());   
   }

   public void addResources(String baseResource, final String[] includes, final String[] excludes, ClassLoader loader)
   {
      URL url = loader.getResource(baseResource);
      if (url == null) throw new RuntimeException("Could not find baseResource: " + baseResource);
      String urlString = url.toString();
      int idx = urlString.lastIndexOf(baseResource);
      urlString = urlString.substring(0, idx);
      try
      {
         url = new URL(urlString);
         VirtualFile parent = VFS.getRoot(url);

         VisitorAttributes va = new VisitorAttributes();
         va.setLeavesOnly(true);
         SuffixesExcludeFilter noJars = new SuffixesExcludeFilter(JarUtils.getSuffixes());
         va.setRecurseFilter(noJars);

         VirtualFileFilter filter = new VirtualFileFilter()
         {

            public boolean accepts(VirtualFile file)
            {
               boolean matched = false;
               String path = file.getPathName();
               for (String include : includes)
               {
                  if (antMatch(path, include))
                  {
                     matched = true;
                     break;
                  }
               }
               if (!matched) return false;
               if (excludes != null)
               {
                  for (String exclude : excludes)
                  {
                     if (antMatch(path, exclude)) return false;
                  }
               }
               return true;
            }

         };

         FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, va);
         parent.visit(visitor);
         List<VirtualFile> files = visitor.getMatched();
         for (VirtualFile vf : files)
         {
            mkdirs(vf.getPathName()).addChild(vf);
         }
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public static Pattern getPattern(String matcher)
   {
      matcher = matcher.replace(".", "\\.");
      matcher = matcher.replace("*", ".*");
      matcher = matcher.replace("?", ".{1}");
      return Pattern.compile(matcher);

   }

   public static boolean antMatch(String path, String expression)
   {
      if (path.startsWith("/")) path = path.substring(1);
      if (expression.endsWith("/")) expression += "**";
      String[] paths = path.split("/");
      String[] expressions = expression.split("/");

      int x = 0, p = 0;
      Pattern pattern = getPattern(expressions[0]);

      for (p = 0; p < paths.length && x < expressions.length; p++)
      {
         if (expressions[x].equals("**"))
         {
            do
            {
               x++;
            } while (x < expressions.length && expressions[x].equals("**"));
            if (x == expressions.length) return true; // "**" with nothing after it
            pattern = getPattern(expressions[x]);
         }
         String element = paths[p];
         if (pattern.matcher(element).matches())
         {
            x++;
            if (x < expressions.length)
            {
               pattern = getPattern(expressions[x]);
            }
         }
         else if (!(x > 0 && expressions[x - 1].equals("**"))) // our previous isn't "**"
         {
            return false;
         }
      }
      if (p < paths.length) return false;
      if (x < expressions.length) return false;
      return true;
   }

   public void addChild(VirtualFile vf)
   {
      directory.addChld(vf.getHandler());
   }

   public void addChild(VirtualFile vf, String newName)
   {
      try
      {
         directory.addChld(new AssembledFileHandler((AssembledContext) directory.getVFSContext(), directory, newName, vf.getHandler()));
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void addResource(String resource)
   {
      addResource(resource, Thread.currentThread().getContextClassLoader());
   }

   public void addResource(String resource, String newName)
   {
      addResource(resource, Thread.currentThread().getContextClassLoader(), newName);
   }

   public void addResource(String resource, ClassLoader loader)
   {
      URL url = loader.getResource(resource);
      if (url == null) throw new RuntimeException("Could not find resource: " + resource);

      addResource(url);
   }

   public void addResource(URL url)
   {
      try
      {
         VirtualFile vf = VFS.getRoot(url);
         addChild(vf);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void addResource(String resource, ClassLoader loader, String newName)
   {
      URL url = loader.getResource(resource);
      if (url == null) throw new RuntimeException("Could not find resource: " + resource);
      try
      {
         VirtualFile vf = VFS.getRoot(url);
         addChild(vf, newName);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public AssembledDirectory mkdir(String name)
   {
      AssembledDirectoryHandler handler = null;
      try
      {
         handler = new AssembledDirectoryHandler((AssembledContext) directory.getVFSContext(), directory, name);
         directory.addChld(handler);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      return new AssembledDirectory(handler);
   }
}
