/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.maven.api.Project;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Internal stable identity for a project whose state is represented by immutable snapshots.
 *
 * @param <A> the legacy attached-artifact representation
 */
public final class MutableProject<A> {
    private final AtomicReference<Project> project = new AtomicReference<>();
    private final CopyOnWriteArraySet<SourceRoot> sourceRoots = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArrayList<A> attachedArtifacts = new CopyOnWriteArrayList<>();
    private volatile Properties properties = new Properties();

    public void initialize(@Nonnull Project project) {
        Project value = requireNonNull(project, "project cannot be null");
        if (!this.project.compareAndSet(null, value) && this.project.get() != value) {
            throw new IllegalStateException("project has already been initialized");
        }
    }

    @Nonnull
    public Project project() {
        return requireNonNull(project.get(), "project has not been initialized");
    }

    @Nullable
    public Project projectOrNull() {
        return project.get();
    }

    public void replace(@Nonnull Project current, @Nonnull Project replacement) {
        requireNonNull(current, "current project cannot be null");
        requireNonNull(replacement, "replacement project cannot be null");
        if (!project.compareAndSet(current, replacement)) {
            throw new IllegalStateException("project changed while it was being replaced");
        }
    }

    public synchronized boolean addSourceRoot(@Nonnull SourceRoot sourceRoot) {
        return sourceRoots.add(requireNonNull(sourceRoot, "sourceRoot cannot be null"));
    }

    public synchronized boolean removeSourceRoot(@Nonnull SourceRoot sourceRoot) {
        return sourceRoots.remove(requireNonNull(sourceRoot, "sourceRoot cannot be null"));
    }

    public synchronized boolean removeSourceRoots(@Nonnull Predicate<SourceRoot> filter) {
        return sourceRoots.removeIf(requireNonNull(filter, "filter cannot be null"));
    }

    public synchronized boolean replaceSourceRoot(
            @Nonnull Predicate<SourceRoot> filter, @Nonnull SourceRoot replacement) {
        requireNonNull(filter, "filter cannot be null");
        requireNonNull(replacement, "replacement cannot be null");
        List<SourceRoot> roots = List.copyOf(sourceRoots);
        for (int i = 0; i < roots.size(); i++) {
            if (filter.test(roots.get(i))) {
                sourceRoots.clear();
                sourceRoots.addAll(roots.subList(0, i));
                sourceRoots.add(replacement);
                sourceRoots.addAll(roots.subList(i + 1, roots.size()));
                return true;
            }
        }
        return false;
    }

    public synchronized void setSourceRoots(@Nonnull Collection<SourceRoot> sourceRoots) {
        this.sourceRoots.clear();
        this.sourceRoots.addAll(requireNonNull(sourceRoots, "sourceRoots cannot be null"));
    }

    @Nonnull
    public Collection<SourceRoot> sourceRoots() {
        return Collections.unmodifiableSet(sourceRoots);
    }

    public synchronized boolean addAttachedArtifact(@Nonnull A artifact) {
        requireNonNull(artifact, "artifact cannot be null");
        int index = attachedArtifacts.indexOf(artifact);
        if (index >= 0) {
            attachedArtifacts.set(index, artifact);
            return true;
        }
        attachedArtifacts.add(artifact);
        return false;
    }

    public synchronized void setAttachedArtifacts(@Nonnull Collection<? extends A> artifacts) {
        attachedArtifacts.clear();
        attachedArtifacts.addAll(requireNonNull(artifacts, "artifacts cannot be null"));
    }

    @Nonnull
    public List<A> attachedArtifacts() {
        return Collections.unmodifiableList(attachedArtifacts);
    }

    public void setProperties(@Nonnull Properties properties) {
        this.properties = requireNonNull(properties, "properties cannot be null");
    }

    @Nonnull
    public Properties properties() {
        return properties;
    }
}
