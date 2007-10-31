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
package org.jboss.virtual.plugins.vfs.helpers;

import java.util.StringTokenizer;

/**
 * PathTokenizer.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class PathTokenizer
{
   /**
    * Utility class
    */
   private PathTokenizer()
   {
   }
   
   /**
    * Get the tokens
    * 
    * @param path the path
    * @return the tokens or null if the path is empty
    * @throws IllegalArgumentException if the path is null, it is empty or it is a relative path
    */
   public static String[] getTokens(String path)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      StringTokenizer tokenizer = new StringTokenizer(path, "/");
      int count = tokenizer.countTokens();
      if (count == 0)
         return null;

      String[] tokens = new String[count];
      int i = 0;
      while (tokenizer.hasMoreTokens())
      {
         String token = tokenizer.nextToken();

         if (token.equals(""))
            throw new IllegalArgumentException("A path element is empty: " + path);
         if (token.equals(".") || token.equals(".."))
            throw new IllegalArgumentException("Reverse paths are not allowed (containing a . or ..), use getParent(): " + path);

         tokens[i++] = token;
      }
      return tokens;
   }
   
   /**
    * Get the remaining path from some tokens
    * 
    * @param tokens the tokens
    * @param i the current location
    * @return the remaining path
    * @throws IllegalArgumentException for null tokens or i is out of range
    */
   public static String getRemainingPath(String[] tokens, int i)
   {
      if (tokens == null)
         throw new IllegalArgumentException("Null tokens");
      if (i < 0 || i >= tokens.length)
         throw new IllegalArgumentException("i is not in the range of tokens: 0-" + (tokens.length-1));
      
      if (i == tokens.length-1)
         return tokens[tokens.length-1];
      
      StringBuilder buffer = new StringBuilder();
      for (; i < tokens.length-1; ++i)
      {
         buffer.append(tokens[i]);
         buffer.append("/");
      }
      buffer.append(tokens[tokens.length-1]);
      return buffer.toString();
   }
}
