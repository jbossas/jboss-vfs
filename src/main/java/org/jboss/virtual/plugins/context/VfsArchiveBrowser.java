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
package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jboss.util.file.ArchiveBrowser;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.vfs.helpers.FilterVirtualFileVisitor;
import org.jboss.virtual.plugins.vfs.helpers.SuffixesExcludeFilter;

/**
 * This is a bridge to an older, crappier API written by myself.
 *
 * @deprecated
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class VfsArchiveBrowser implements Iterator
{
   /** TODO WHAT DOES THIS DO? It is unused */
   private ArchiveBrowser.Filter filter;
   private VirtualFile vf;
   private Iterator<VirtualFile> it;


   public VfsArchiveBrowser(final ArchiveBrowser.Filter filter, VirtualFile vf)
   {
      this.filter = filter;
      this.vf = vf;
      List<VirtualFile> classes = getResources(new VirtualFileFilter() {
         public boolean accepts(VirtualFile file)
         {
            return filter.accept(file.getName());
         }
      });

      it = classes.iterator();
   }

   public List<VirtualFile> getResources(VirtualFileFilter filter)
   {
      VisitorAttributes va = new VisitorAttributes();
      va.setLeavesOnly(true);
      SuffixesExcludeFilter noJars = new SuffixesExcludeFilter(JarUtils.getSuffixes());
      va.setRecurseFilter(noJars);
      FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(filter, va);

      try
      {
         vf.visit(visitor);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      return visitor.getMatched();
   }


   public boolean hasNext()
   {
      return it.hasNext();
   }

   public Object next()
   {
      try
      {
         return it.next().openStream();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void remove()
   {
      it.remove();
   }
}
