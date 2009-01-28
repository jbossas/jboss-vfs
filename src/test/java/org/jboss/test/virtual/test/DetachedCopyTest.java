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
package org.jboss.test.virtual.test;

import org.jboss.virtual.VirtualFile;

/**
 * Detached tests - no parent re-wiring.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class DetachedCopyTest extends CopyTest
{
   public DetachedCopyTest(String s)
   {
      super(s);
   }

   protected abstract boolean isExploded() throws Exception;

   protected abstract boolean isSame(VirtualFile original) throws Exception;

   protected void assertNoReplacement(VirtualFile original, VirtualFile replacement, boolean unpacked) throws Exception
   {
      if (isSame(original))
         assertSame(original, replacement);
      else
         assertReplacement(original, replacement, unpacked || isExploded());
   }

   protected void assertTopLevel(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertReplacement(original, replacement, isExploded());
      assertNull(replacement.getParent());
   }

   @Override
   protected void assertTopLevelParent(VirtualFile originalParent, VirtualFile replacementParent) throws Exception
   {
      assertNull(replacementParent);
   }

   protected void assertNestedLevel(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertExplodedReplacement(original, replacement);
   }

   protected void assertOnURI(VirtualFile original, VirtualFile replacement) throws Exception
   {
      assertReplacement(original, replacement);
   }
}