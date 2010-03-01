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
package org.jboss.vfs.util;

import java.util.regex.Pattern;

import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VirtualFile;

/**
 * Regexp patter filter.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractPatternVirtualFileFilter implements VirtualFileFilter
{
   private Pattern pattern;

   public AbstractPatternVirtualFileFilter(String regexp)
   {
      if (regexp == null)
         throw new IllegalArgumentException("Null regexp");

      pattern = Pattern.compile(regexp);
   }

   /**
    * Extract match string from file.
    *
    * @param file the file
    * @return extracted match string
    */
   protected abstract String getMatchString(VirtualFile file);

   /**
    * Should we match the pattern.
    *
    * @return the match flag
    */
   protected abstract boolean doMatch();

   public boolean accepts(VirtualFile file)
   {
      String string = getMatchString(file);
      return pattern.matcher(string).matches() == doMatch();
   }
}
