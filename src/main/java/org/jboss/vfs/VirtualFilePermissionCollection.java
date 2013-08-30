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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;

final class VirtualFilePermissionCollection extends PermissionCollection {
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("list", VirtualFilePermission[].class)
    };

    private static final VirtualFilePermission[] NO_PERMISSIONS = new VirtualFilePermission[0];

    private volatile VirtualFilePermission[] permissions = NO_PERMISSIONS;

    private static final Field listField;

    static {
        listField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                final Field field;
                try {
                    field = VirtualFilePermissionCollection.class.getDeclaredField("permissions");
                } catch (NoSuchFieldException e) {
                    throw new NoSuchFieldError(e.getMessage());
                }
                field.setAccessible(true);
                return field;
            }
        });
    }

    VirtualFilePermissionCollection() {
    }

    public void add(final Permission permission) {
        if (permission instanceof VirtualFilePermission) {
            add((VirtualFilePermission) permission);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public synchronized void add(final VirtualFilePermission permission) {
        if (permission != null) {
            final VirtualFilePermission[] permissions = this.permissions;
            final int length = permissions.length;
            final VirtualFilePermission[] newPermissions = Arrays.copyOf(permissions, length + 1);
            newPermissions[length] = permission;
            this.permissions = newPermissions;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean implies(final Permission permission) {
        return permission instanceof VirtualFilePermission && implies((VirtualFilePermission) permission);
    }

    private boolean implies(final VirtualFilePermission permission) {
        assert permission != null; // else the above check would have failed
        int remainingFlags = permission.getActionFlags();
        if (remainingFlags == 0) { return true; }
        // snapshot
        final VirtualFilePermission[] permissions = this.permissions;
        final String theirName = permission.getName();
        for (VirtualFilePermission ourPermission : permissions) {
            if (VirtualFilePermission.impliesPath(ourPermission.getName(), theirName)) {
                remainingFlags &= ~ourPermission.getActionFlags();
                if (remainingFlags == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Enumeration<Permission> elements() {
        final VirtualFilePermission[] permissions = this.permissions;
        return new Enumeration<Permission>() {
            private int idx = 0;

            public boolean hasMoreElements() {
                return idx < permissions.length;
            }

            public Permission nextElement() {
                return permissions[idx++];
            }
        };
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        try {
            listField.set(this, ois.readFields().get("list", null));
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }
}
