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
package org.jboss.virtual.plugins.copy;

import java.io.IOException;
import java.io.File;

import org.jboss.virtual.plugins.context.jar.NestedJarHandler;
import org.jboss.virtual.plugins.context.jar.JarHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Copy any non-temp file into temp dir.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class TempCopyMechanism extends AbstractCopyMechanism
{
   public static final TempCopyMechanism INSTANCE = new TempCopyMechanism();
   
   protected String getType()
   {
      return "temp";
   }

   protected boolean isAlreadyModified(VirtualFileHandler handler) throws IOException
   {
      return handler instanceof NestedJarHandler;
   }

   protected File copy(File guidDir, VirtualFileHandler handler) throws IOException
   {
      // leave top level archives or leaves in one piece
      if (handler instanceof JarHandler || handler.isLeaf())
      {
         File temp = new File(guidDir, handler.getName());
         temp.deleteOnExit();
         rewrite(handler, temp);
         return temp;
      }
      return super.copy(guidDir, handler);
   }

   protected boolean replaceOldHandler(VirtualFileHandler parent, VirtualFileHandler oldHandler, VirtualFileHandler newHandler) throws IOException
   {
      return false;
   }
}