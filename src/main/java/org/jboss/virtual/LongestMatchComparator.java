/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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

package org.jboss.virtual;

import java.util.Comparator;
import java.util.Iterator;
import java.io.Serializable;

/**
 * A comparator which sorts longer values before their shorter initial sublists.
 *
 * @param <E> the element type
 * @param <C> the collection type
 */
final class LongestMatchComparator<E, C extends Iterable<E>> implements Comparator<C>, Serializable
{
   private static final long serialVersionUID = 954089122568817323L;

   private final Comparator<E> comparator;

   private LongestMatchComparator(Comparator<E> comparator)
   {
      this.comparator = comparator;
   }

   /**
    * Create a new instance.
    *
    * @param comparator the element comparator
    * @param <E> the element type
    * @param <C> the collection type
    * @return the new collection comparator
    */
   static <E, C extends Iterable<E>> Comparator<C> create(Comparator<E> comparator) {
      return new LongestMatchComparator<E, C>(comparator);
   }

   /**
    * Create a new instance which uses the native ordering of the element type.
    *
    * @param <E> the element type
    * @param <C> the collection type
    * @return the new collection comparator
    */
   static <E extends Comparable<? super E>, C extends Iterable<E>> Comparator<C> create() {
      return new LongestMatchComparator<E, C>(NativeComparator.<E>getInstance());
   }

   /** {@inheritDoc} */
   public int compare(C o1, C o2)
   {
      final Comparator<E> comparator = this.comparator;
      final Iterator<E> i1 = o1.iterator();
      final Iterator<E> i2 = o2.iterator();
      while (i1.hasNext() && i2.hasNext()) {
         final E t1 = i1.next();
         final E t2 = i2.next();
         final int c = comparator.compare(t1, t2);
         if (c != 0) {
            return c;
         }
      }
      return i1.hasNext() ? -1 : i2.hasNext() ? 1 : 0;
   }

   /**
    * A native-order comparator.
    *
    * @param <E>
    */
   private static final class NativeComparator<E extends Comparable<? super E>> implements Comparator<E>, Serializable
   {
      private static final long serialVersionUID = -4198283451912738802L;

      /** {@inheritDoc} */
      public int compare(E o1, E o2)
      {
         return o1.compareTo(o2);
      }

      private static final NativeComparator INSTANCE = new NativeComparator();

      @SuppressWarnings({ "unchecked" })
      private static <E extends Comparable<? super E>> Comparator<E> getInstance() {
         return INSTANCE;
      }
   }
}
