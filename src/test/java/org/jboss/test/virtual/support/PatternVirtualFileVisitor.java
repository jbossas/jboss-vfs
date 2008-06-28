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
package org.jboss.test.virtual.support;

import java.util.List;
import java.util.ArrayList;

import org.jboss.virtual.VirtualFileVisitor;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.VirtualFile;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class PatternVirtualFileVisitor implements VirtualFileVisitor
{
   private String subPattern = ".class";
   private List<String> resources = new ArrayList<String>();

   public PatternVirtualFileVisitor()
   {
   }

   public PatternVirtualFileVisitor(String subPattern)
   {
      this.subPattern = subPattern;
   }

   public VisitorAttributes getAttributes()
   {
      return VisitorAttributes.RECURSE_LEAVES_ONLY;
   }

   public void visit(VirtualFile vf)
   {
      String pathName = vf.getPathName();
      if (pathName.endsWith(subPattern))
         resources.add(pathName);
   }

   public List<String> getResources()
   {
      return resources;
   }

   public int size()
   {
      return resources.size();
   }

   public String toString()
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("sub-pattern: ").append(subPattern);
      buffer.append(", resources: ").append(resources);
      return buffer.toString();
   }
}
