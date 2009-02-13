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
package org.jboss.virtual.plugins.context.zip;

import java.io.File;

import org.jboss.virtual.plugins.context.temp.BasicTempInfo;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * ZipEntry temp info.
 * It resets initState of owning zip context.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class ZipEntryTempInfo extends BasicTempInfo
{
   private ZipEntryContext context;

   public ZipEntryTempInfo(String path, File file, VirtualFileHandler handler, ZipEntryContext context)
   {
      super(path, file, handler);
      this.context = context;
   }

   @Override
   public void cleanup()
   {
      context.resetInitStatus();
      super.cleanup();
   }
}