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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PathTokenizer.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
@SuppressWarnings({"StringEquality"})
public class PathTokenizer
{
   /** The reverse path const */
   private static final String CURRENT_PATH = ".";

   /** The reverse path const */
   private static final String REVERSE_PATH = "..";

   /**
    * Utility class
    */
   private PathTokenizer()
   {
   }

   /**
    * Get the remaining path from some tokens
    *
    * @param tokens the tokens
    * @param i the current location
    * @param end the end index
    * @return the remaining path
    * @throws IllegalArgumentException for null tokens or i is out of range
    */
   protected static String getRemainingPath(List<String> tokens, int i, int end)
   {
      if (tokens == null)
         throw new IllegalArgumentException("Null tokens");
      if (i < 0 || i >= end)
         throw new IllegalArgumentException("i is not in the range of tokens: 0-" + (end-1));

      if (i == end-1)
         return tokens.get(end-1);

      StringBuilder buffer = new StringBuilder();
      for (; i < end-1; ++i)
      {
         buffer.append(tokens.get(i));
         buffer.append("/");
      }
      buffer.append(tokens.get(end-1));
      return buffer.toString();
   }

   /**
    * Get the tokens
    * 
    * @param path the path
    * @return the tokens or null if the path is empty
    * @throws IllegalArgumentException if the path is null, it is empty or it is a relative path
    */
   public static List<String> getTokens(String path)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      char[] chars = path.toCharArray();
      StringBuilder buffer = new StringBuilder();
      List<String> list = new ArrayList<String>();
      String specialToken = null;

      for (int index=0; index < chars.length; index++)
      {
         char ch = chars[index];

         if (ch == '/')
         {
            if (index > 0)
            {
               if (buffer.length() == 0 && specialToken == null)
                  continue;

               if (specialToken != null)
                  list.add(specialToken);
               else
                  list.add(buffer.toString());

               // reset
               buffer.setLength(0);
               specialToken = null;
            }
         }
         else if (ch == '.')
         {
            int bufferLength = buffer.length();

            if (specialToken == null && bufferLength == 0)
               specialToken = CURRENT_PATH;
            else if (specialToken == CURRENT_PATH && bufferLength == 0)
               specialToken = REVERSE_PATH;
            else if (specialToken == REVERSE_PATH && bufferLength == 0)
               throw new IllegalArgumentException("Illegal token (" + specialToken + ch + ") in path: " + path);
            else
               buffer.append(ch);
         }
         else
         {
            // token starts with '.' or '..', but also has some path after that
            if (specialToken != null)
            {
               // we don't allow tokens after '..'
               if (specialToken == REVERSE_PATH)
                  throw new IllegalArgumentException("Illegal token (" + specialToken + ch + ") in path: " + path);

               // after '.' more path is legal == unix hidden directories
               buffer.append(specialToken);
               specialToken = null;
            }
            buffer.append(ch);
         }
      }

      // add last token
      if (specialToken != null)
         list.add(specialToken);
      else if (buffer.length() > 0)
         list.add(buffer.toString());

      return list;
   }
   
   /**
    * Get the remaining path from some tokens
    * 
    * @param tokens the tokens
    * @param i the current location
    * @return the remaining path
    * @throws IllegalArgumentException for null tokens or i is out of range
    */
   public static String getRemainingPath(List<String> tokens, int i)
   {
      if (tokens == null)
         throw new IllegalArgumentException("Null tokens");

      return getRemainingPath(tokens, i, tokens.size());
   }

   /**
    * Apply any . or .. paths in the path param.
    *
    * @param path the path
    * @return simple path, containing no . or .. paths
    * @throws IOException if reverse path goes over the top path
    */
   public static String applySpecialPaths(String path) throws IOException
   {
      List<String> tokens = getTokens(path);
      if (tokens == null)
         return null;

      int i = 0;
      for(int j = 0; j < tokens.size(); j++)
      {
         String token = tokens.get(j);

         if (isCurrentToken(token))
            continue;
         else if (isReverseToken(token))
            i--;
         else
            tokens.set(i++, token);

         if (i < 0)
            throw new IOException("Using reverse path on top path: " + path);
      }
      return getRemainingPath(tokens, 0, i);
   }

   /**
    * Is current token.
    *
    * @param token the token to check
    * @return true if token matches current path token
    */
   public static boolean isCurrentToken(String token)
   {
      return CURRENT_PATH == token;
   }

   /**
    * Is reverse token.
    *
    * @param token the token to check
    * @return true if token matches reverse path token
    */
   public static boolean isReverseToken(String token)
   {
      return REVERSE_PATH == token;
   }
}
