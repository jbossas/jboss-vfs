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
package org.jboss.virtual.plugins.context.jar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * JarUtils.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class JarUtils
{
   /** The jar suffixes (immutable, do not update unless updateLock is held) */
   private static volatile Set<String> jarSuffixes;
   /** The lock to hold in order to update jarSuffixes */
   private static final Object updateLock = new Object();

   // Initialise known suffixes
   static
   {
      final HashSet<String> set = new HashSet<String>();
      set.add(".zip");
      set.add(".ear");
      set.add(".jar");
      set.add(".rar");
      set.add(".war");
      set.add(".sar");
      set.add(".har");
      set.add(".aop");
      synchronized (updateLock) {
         jarSuffixes = set;
      }
   }

   /**
    * Sets the jar suffixes
    * 
    * @param suffixes the suffixes
    * @throws IllegalArgumentException for a null suffix
    */
   public static void setJarSuffixes(Set<String> suffixes)
   {
      if (suffixes == null)
         throw new IllegalArgumentException("Null suffix");

      synchronized (updateLock)
      {
         jarSuffixes = new HashSet<String>(suffixes);
      }
   }

   /**
    * Add a jar suffix
    * 
    * @param suffix the suffix
    * @return true when added
    * @throws IllegalArgumentException for a null suffix
    */
   public static boolean addJarSuffix(String suffix)
   {
      if (suffix == null)
         throw new IllegalArgumentException("Null suffix");

      synchronized (updateLock)
      {
         if (jarSuffixes.contains(suffix))
            return false;
         else
         {
            final HashSet<String> newSet = new HashSet<String>(jarSuffixes);
            newSet.add(suffix);
            jarSuffixes = Collections.unmodifiableSet(newSet);
            return true;
         }
      }
   }

   /**
    * Remove a jar suffix
    * 
    * @param suffix the suffix
    * @return true when removed
    * @throws IllegalArgumentException for a null suffix
    */
   public static boolean removeJarSuffix(String suffix)
   {
      if (suffix == null)
         throw new IllegalArgumentException("Null suffix");

      synchronized (updateLock)
      {
         if (jarSuffixes.contains(suffix))
         {
            final HashSet<String> newSet = new HashSet<String>(jarSuffixes);
            newSet.remove(suffix);
            if (newSet.isEmpty())
               jarSuffixes = Collections.emptySet();
            else
               jarSuffixes = Collections.unmodifiableSet(newSet);
            return true;
         }
         else
            return false;
      }
   }
   
   /**
    * Get the set of jar suffixes
    * 
    * @return the set of suffixes
    */
   public static Set<String> getSuffixes()
   {
      return jarSuffixes;
   }

   /**
    * Clear the set of suffixes
    */
   public static void clearSuffixes()
   {
      synchronized (updateLock) {
         jarSuffixes = Collections.emptySet();
      }
   }

   /**
    * Utilities
    */
   private JarUtils()
   {
   }
   
   /**
    * Whether this is an archive
    *
    * @param name the name
    * @return true when an archive
    * @throws IllegalArgumentException for a null name
    */
   public static boolean isArchive(String name)
   {
      if (name == null)
         throw new IllegalArgumentException("Null name");

      for(String suffix : jarSuffixes)
      {
         if (name.endsWith(suffix))
            return true;
      }
      return false;
   }
   
   /**
    * Create a jar url from a normal url
    * 
    * @param url the normal url
    * @return the jar url
    * @throws MalformedURLException if the url is malformed
    * @throws IllegalArgumentException for a null url
    */
   public static URL createJarURL(URL url) throws MalformedURLException
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");
      return new URL("jar:" + url + "!/");
   }
}
