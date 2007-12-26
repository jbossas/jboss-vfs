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
package org.jboss.virtual.plugins.context.vfs;

import java.util.concurrent.ConcurrentHashMap;
import java.net.URISyntaxException;
import java.io.IOException;

/**
 * Factory for creating AssembledDirectory.
 *
 * @author <a href="bill@jboss.com">Bill Burke</a>
 * @version $Revision: 1.1 $
 */
public class AssembledContextFactory
{
   private ConcurrentHashMap<String, AssembledDirectory> registry = new ConcurrentHashMap<String, AssembledDirectory>();
   private volatile int count;
   private static AssembledContextFactory instance = new AssembledContextFactory();

   /**
    * Creates an assembly returning the root AssembledDirectory .
    * Creates an assembly base and registers it with a local hashmap under name.
    *
    * @param name the name of this assembly
    * @param rootName the name of the root directory you want
    * @return
    */
   public AssembledDirectory create(String name, String rootName)
   {
      if (registry.containsKey(name))
         throw new RuntimeException("Assembled context already exists for name: " + name);
      try
      {
         AssembledContext context = new AssembledContext(name, rootName);
         AssembledDirectory directory = (AssembledDirectory)context.getRoot().getVirtualFile();
         registry.put(name, directory);
         return directory;
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Find an assembly.  Usually used only by the URL protocol handlers.
    *
    * @param name
    * @return
    */
   public AssembledDirectory find(String name)
   {
      return registry.get(name);
   }

   /**
    * Creates an assembly returning the root AssembledDirectory .
    * 
    * The assembly name will be randomly generated and registered with the internal hashmap registry.
    *
    * @param rootName the name of the root AssembledDirectory
    * @return
    */
   public AssembledDirectory create(String rootName)
   {
      String name = "" + System.currentTimeMillis() + "" + count++;
      return create(name, rootName);
   }

   /**
    * Remove an assembly
    *
    * @param directory
    */
   public void remove(AssembledDirectory directory)
   {
      try
      {
         if (directory.getParent() != null) throw new RuntimeException("This is not the root of assembly");
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      registry.remove(((AssembledContext)directory.getHandler().getVFSContext()).getName());
   }

   public static AssembledContextFactory getInstance()
   {
      return instance;
   }

   public static void setInstance(AssembledContextFactory instance)
   {
      AssembledContextFactory.instance = instance;
   }
}
