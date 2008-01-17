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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * JarContext.
 * 
 * @author Scott.Stark@jboss.org
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class JarContext extends AbstractVFSContext
{
   /** The root file */
   private final VirtualFileHandler root;
   
   /** A reference to the virtual file of the root to stop it getting closed */
   private final VirtualFile rootFile;
   
   /**
    * Create a new JarContext.
    * 
    * @param rootURL the root url
    * @throws IOException for an error accessing the file system
    * @throws URISyntaxException for an error parsing the URI
    */
   public JarContext(URL rootURL) throws IOException, URISyntaxException
   {
      super(rootURL);
      root = createVirtualFileHandler(null, rootURL);
      rootFile = root.getVirtualFile();
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return root;
   }

   /**
    * Create a new virtual file handler
    * 
    * @param parent the parent
    * @param url the url
    * @return the handler
    * @throws IOException for any error accessing the file system
    * @throws IllegalArgumentException for a null entry or url
    */
   protected VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, URL url) throws IOException
   {
      if (url == null)
         throw new IllegalArgumentException("Null url");

      String urlStr = url.toString();
      String jarName = extractJarName(urlStr);
      String entryPath = urlStr;
      entryPath = entryPath(entryPath);
      JarHandler jar =  new JarHandler(this, parent, url, jarName);
      if (entryPath == null)
         return jar;
      
      // todo This is a hack until we can fix http://jira.jboss.com/jira/browse/JBMICROCONT-164
      AbstractVirtualFileHandler result = (AbstractVirtualFileHandler)jar.getChild(entryPath);
      result.setPathName("");
      return result;
   }

   public static String entryPath(String entryName)
   {
      int index;
      index = entryName.indexOf("!/");
      if (index != -1)
      {
         entryName = entryName.substring(index + 2);
      }
      else
      {
         entryName = null;
      }
      if (entryName == null || "".equals(entryName.trim()))
         return null;
      
      return entryName;
   }

   public static String extractJarName(String urlStr)
   {
      String jarName = urlStr;
      int index = jarName.indexOf('!');
      if (index != -1)
         jarName = jarName.substring(0, index);
      index = jarName.lastIndexOf('/');
      if (index != -1 && index < jarName.length()-1)
         jarName = jarName.substring(index+1);
      return jarName;
   }

   @Override
   protected void finalize() throws Throwable
   {
      if (rootFile != null)
         rootFile.close();
      super.finalize();
   }
}
