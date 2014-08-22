/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.vfs.util.automount;

/**
 * Simple mount owner wrapper relying on any object type as the owner.
 *
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class SimpleMountOwner extends AbstractMountOwner<Object> {
    /**
     * Construct with a new object as the owner
     */
    public SimpleMountOwner() {
        this(new Object());
    }

    /**
     * Construct with the provided object as the owner
     *
     * @param object the actual owner
     */
    public SimpleMountOwner(Object owner) {
        super(owner);
    }

    /**
     * No-op cleanup method
     */
    @Override
    public void onCleanup() {
    }
}
