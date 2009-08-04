/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.vfs.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.jboss.logging.Logger;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;

/**
 * Matches a file name against a list of suffixes. 
 * 
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 44223 $
 */
public class SuffixMatchFilter extends AbstractVirtualFileFilterWithAttributes
{
   private static Logger log = Logger.getLogger(SuffixMatchFilter.class);
   /** The suffixes */
   private Collection<String> suffixes;
   private boolean trace;

   /**
    * Create a new SuffixMatchFilter,
    * using {@link VisitorAttributes#DEFAULT}
    * 
    * @param suffix the suffix
    * @throws IllegalArgumentException for a null suffix
    */
   public SuffixMatchFilter(String suffix)
   {
      this(suffix, null);
   }
   
   /**
    * Create a new SuffixMatchFilter.
    * 
    * @param suffix the suffix
    * @param attributes the attributes, pass null to use {@link VisitorAttributes#DEFAULT}
    * @throws IllegalArgumentException for a null suffix
    */
   @SuppressWarnings("unchecked")      
   public SuffixMatchFilter(String suffix, VisitorAttributes attributes)
   {
      this(Collections.singleton(suffix), attributes);
   }
   /**
    * Create a new SuffixMatchFilter.
    * @param suffixes - the list of file suffixes to accept.
    * @throws IllegalArgumentException for a null suffixes
    */
   public SuffixMatchFilter(Collection<String> suffixes)
   {
      this(suffixes, null);
   }
   /**
    * Create a new SuffixMatchFilter.
    * @param suffixes - the list of file suffixes to accept.
    * @param attributes the attributes, pass null to use {@link VisitorAttributes#DEFAULT}
    * @throws IllegalArgumentException for a null suffixes
    */
   public SuffixMatchFilter(Collection<String> suffixes, VisitorAttributes attributes)
   {
      super(attributes == null ? VisitorAttributes.DEFAULT : attributes);
      if (suffixes == null)
         throw new IllegalArgumentException("Null suffixes");
      this.suffixes = new LinkedHashSet<String>();
      this.suffixes.addAll(suffixes);
      trace = log.isTraceEnabled();
   }

   /**
    * Accept any file that ends with one of the filter suffixes. This checks
    * that the file.getName() endsWith a suffix.
    * @return true if the file matches a suffix, false otherwise.
    */
   public boolean accepts(VirtualFile file)
   {
      String name = file.getName();
      boolean accepts = false;
      for(String suffix : suffixes)
      {
         if (name.endsWith(suffix))
         {
            accepts = true;
            break;
         }
      }
      if( trace )
         log.trace(file+" accepted: "+accepts);
      return accepts;
   }
}
