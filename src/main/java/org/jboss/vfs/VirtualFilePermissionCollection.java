/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.vfs;

import java.io.FilePermission;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

final class VirtualFilePermissionCollection extends PermissionCollection {
    private final PermissionCollection collection;
    private final ArrayList<Permission> list;

    VirtualFilePermissionCollection(final PermissionCollection collection) {
        this.collection = collection;
        list = new ArrayList<Permission>();
    }

    VirtualFilePermissionCollection(final PermissionCollection collection, final ArrayList<Permission> list) {
        this.collection = collection;
        this.list = list;
    }

    public void add(final Permission permission) {
        if (permission instanceof VirtualFilePermission) {
            final VirtualFilePermission virtualFilePermission = (VirtualFilePermission) permission;
            add(virtualFilePermission);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void add(final VirtualFilePermission permission) {
        if (permission != null) {
            collection.add(permission.getFilePermission());
            list.add(permission);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean implies(final Permission permission) {
        return permission instanceof VirtualFilePermission && implies((VirtualFilePermission) permission);
    }

    public boolean implies(final VirtualFilePermission permission) {
        return permission != null && collection.implies(permission.getFilePermission());
    }

    public Enumeration<Permission> elements() {
        return Collections.enumeration(list);
    }

    Object writeReplace() {
        return new Serialized(list.toArray(new VirtualFilePermission[list.size()]));
    }

    static final class Serialized implements Serializable {
        private static final long serialVersionUID = 1L;

        final VirtualFilePermission[] permissions;

        Serialized(final VirtualFilePermission[] permissions) {
            this.permissions = permissions;
        }

        Object readResolve() {
            return new VirtualFilePermissionCollection(new FilePermission("/", "*").newPermissionCollection(), new ArrayList<Permission>(Arrays.asList(permissions)));
        }
    }
}
