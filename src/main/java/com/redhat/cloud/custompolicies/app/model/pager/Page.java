/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.custompolicies.app.model.pager;

import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Page<T> implements List<T> {

    private List<T> wrapped;
    @Getter
    private Pager pager;
    @Getter
    private long totalCount;

    public Page(List<T> wrapped, Pager pager, long totalCount) {
        this.wrapped = wrapped;
        this.pager = pager;
        this.totalCount = totalCount;
    }


    @Override
    public int size() {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return wrapped.contains(o);
    }

    @Override
    @NotNull
    public Iterator<T> iterator() {
        Iterator<T> it = wrapped.iterator();
        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next();
            }
        };
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return wrapped.toArray();
    }

    @Override
    @NotNull
    public <T1> T1[] toArray(T1[] t1s) {
        return wrapped.toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    @NotNull
    public boolean containsAll(Collection<?> collection) {
        return wrapped.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("addAll");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    @Override
    public T get(int i) {
        return wrapped.get(i);
    }

    @Override
    public T set(int i, T t) {
        throw new UnsupportedOperationException("set");
    }

    @Override
    public void add(int i, T t) {
        throw new UnsupportedOperationException("add");
    }

    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public int indexOf(Object o) {
        return wrapped.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return wrapped.lastIndexOf(o);
    }

    @Override
    @NotNull
    public ListIterator<T> listIterator() {
        return new WrapperListIterator<>(wrapped.listIterator());
    }

    @Override
    @NotNull
    public ListIterator<T> listIterator(int i) {
        return new WrapperListIterator<>(wrapped.listIterator(i));
    }

    @Override
    @NotNull
    public List<T> subList(int i, int j) {
        return new Page<>(wrapped.subList(i, j), pager, totalCount);
    }

    class WrapperListIterator<T> implements ListIterator<T> {

        private ListIterator<T> wrapped;

        private WrapperListIterator(ListIterator<T> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public T next() {
            return wrapped.next();
        }

        @Override
        public boolean hasPrevious() {
            return wrapped.hasPrevious();
        }

        @Override
        public T previous() {
            return wrapped.previous();
        }

        @Override
        public int nextIndex() {
            return wrapped.nextIndex();
        }

        @Override
        public int previousIndex() {
            return wrapped.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException("set");
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException("add");
        }
    }


}
