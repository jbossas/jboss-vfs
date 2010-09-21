/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
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

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilterWithAttributes;
import org.jboss.virtual.VisitorAttributes;

/**
 * Copy of org.jboss.deployers.plugins.structure.MetaDataMatchFilter for FileVFSUnitTestCase.testGetMetaDataPackedJar()
 * 
 * @author adrian@jboss.org
 * @version $Revision: 44223 $
 */
public class MetaDataMatchFilter implements VirtualFileFilterWithAttributes
{
   /** The name */
   private String name;

   /** The suffix */
   private String suffix;
   
   /** The attributes */
   private VisitorAttributes attributes;
   
   /**
    * Create a new MetaDataMatchFilter.
    * using {@link VisitorAttributes#LEAVES_ONLY}
    * 
    * @param name the name to exactly match
    * @param suffix the suffix to partially match
    * @throws IllegalArgumentException if both the name and suffix are null
    */
   public MetaDataMatchFilter(String name, String suffix)
   {
      this(name, suffix, null);
   }
   
   /**
    * Create a new MetaDataMatchFilter.
    * 
    * @param name the name to exactly match
    * @param suffix the suffix to partially match
    * @param attributes the attributes, pass null to use {@link VisitorAttributes#LEAVES_ONLY}
    * @throws IllegalArgumentException if both the name and suffix are null
    */
   public MetaDataMatchFilter(String name, String suffix, VisitorAttributes attributes)
   {
      if (name == null && suffix == null)
         throw new IllegalArgumentException("Null name and suffix");
      this.name = name;
      this.suffix = suffix;
      if (attributes == null)
         attributes = VisitorAttributes.LEAVES_ONLY;
      this.attributes = attributes;
   }
   
   public VisitorAttributes getAttributes()
   {
      return attributes;
   }

   public boolean accepts(VirtualFile file)
   {
      String fileName = file.getName();
      if (name != null && fileName.equals(name))
         return true;
      if (suffix != null)
         return fileName.endsWith(suffix);
      return false;
   }
}
