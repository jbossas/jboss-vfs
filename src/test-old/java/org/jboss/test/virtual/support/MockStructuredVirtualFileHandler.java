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

import java.io.IOException;

/**
 * MockStructuredVirtualFileHandler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class MockStructuredVirtualFileHandler extends AbstractMockVirtualFileHandler implements StructuredVirtualFileHandler
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -7967261672121081602L;

   /**
    * Create a new MockStructuredVirtualFileHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param name the name
    */
   public MockStructuredVirtualFileHandler(MockVFSContext context, MockStructuredVirtualFileHandler parent, String name)
   {
      super(context, parent, name);
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      for (VirtualFileHandler child : getChildren(false))
      {
         if (name.equals(child.getName()))
            return child;
      }
      return null;
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      return structuredFindChild(path);
   }
}
