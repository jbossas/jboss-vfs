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
package org.jboss.virtual.plugins.context.helpers;

import java.util.Collections;
import java.util.Set;

import org.jboss.virtual.plugins.context.AbstractExceptionHandler;

/**
 * Match names to ignore the exception.
 * 
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class NamesExceptionHandler extends AbstractExceptionHandler
{
   private Set<String> names;

   public NamesExceptionHandler(String name)
   {
      this(Collections.singleton(name));
   }

   public NamesExceptionHandler(Set<String> names)
   {
      if (names == null)
         throw new IllegalArgumentException("Null names");

      this.names = names;
   }

   @Override
   public void handleZipEntriesInitException(Exception e, String zipName)
   {
      for (String name : names)
      {
         if (zipName.contains(name))
         {
            log.debug("Exception while reading " + zipName, e);
            return;
         }
      }

      super.handleZipEntriesInitException(e, zipName);
   }
}
