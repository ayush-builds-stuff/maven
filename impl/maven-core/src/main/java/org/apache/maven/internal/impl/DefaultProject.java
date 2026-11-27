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
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Type;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.DependencyManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.impl.MappedCollection;
import org.apache.maven.impl.MappedList;
import org.apache.maven.model.root.RootLocator;

import static java.util.Objects.requireNonNull;

public class DefaultProject implements Project {

    private final InternalMavenSession session;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Packaging packaging;
    private final List<org.eclipse.aether.artifact.Artifact> artifacts;
    private final boolean producesMainArtifact;
    private final Model model;
    private final Path pomPath;
    private final Path basedir;
    private final Path rootDirectory;
    private final Project parent;
    private final List<Profile> effectiveProfiles;
    private final List<Profile> declaredActiveProfiles;
    private final List<Profile> effectiveActiveProfiles;

    DefaultProject(InternalMavenSession session, State state) {
        this.session = requireNonNull(session, "session cannot be null");
        State value = requireNonNull(state, "state cannot be null");
        this.groupId = value.groupId();
        this.artifactId = value.artifactId();
        this.version = value.version();
        this.packaging = value.packaging();
        this.artifacts = List.copyOf(value.artifacts());
        this.producesMainArtifact = value.producesMainArtifact();
        this.model = value.model();
        this.pomPath = value.pomPath();
        this.basedir = value.basedir();
        this.rootDirectory = value.rootDirectory();
        this.parent = value.parent();
        this.effectiveProfiles = List.copyOf(value.effectiveProfiles());
        this.declaredActiveProfiles = List.copyOf(value.declaredActiveProfiles());
        this.effectiveActiveProfiles = List.copyOf(value.effectiveActiveProfiles());
    }

    public InternalMavenSession getSession() {
        return session;
    }

    DefaultProject withModel(Model model) {
        return new DefaultProject(
                session,
                new State(
                        groupId,
                        artifactId,
                        version,
                        packaging,
                        artifacts,
                        producesMainArtifact,
                        requireNonNull(model, "model cannot be null"),
                        pomPath,
                        basedir,
                        rootDirectory,
                        parent,
                        effectiveProfiles,
                        declaredActiveProfiles,
                        effectiveActiveProfiles));
    }

    @Nonnull
    @Override
    public String groupId() {
        return groupId;
    }

    @Nonnull
    @Override
    public String artifactId() {
        return artifactId;
    }

    @Nonnull
    @Override
    public String version() {
        return version;
    }

    @Nonnull
    @Override
    public List<ProducedArtifact> artifacts() {
        return new MappedList<>(artifacts, artifact -> session.getArtifact(ProducedArtifact.class, artifact));
    }

    @Override
    public boolean producesMainArtifact() {
        return producesMainArtifact;
    }

    @Nonnull
    @Override
    public Packaging packaging() {
        return packaging;
    }

    @Nonnull
    @Override
    public Model model() {
        return model;
    }

    @Nonnull
    @Override
    public Path pomPath() {
        return Objects.requireNonNull(pomPath, "pomPath cannot be null");
    }

    @Nonnull
    @Override
    public Path basedir() {
        return Objects.requireNonNull(basedir, "basedir cannot be null");
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> dependencies() {
        return new MappedList<>(model().getDependencies(), this::toDependency);
    }

    @Nonnull
    @Override
    public List<DependencyCoordinates> managedDependencies() {
        DependencyManagement dependencyManagement = model().getDependencyManagement();
        if (dependencyManagement != null) {
            return new MappedList<>(dependencyManagement.getDependencies(), this::toDependency);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isTopProject() {
        return basedir().equals(getSession().getTopDirectory());
    }

    @Override
    public boolean isRootProject() {
        return basedir().equals(rootDirectory());
    }

    @Override
    public Path rootDirectory() {
        if (rootDirectory == null) {
            throw new IllegalStateException(RootLocator.UNABLE_TO_FIND_ROOT_PROJECT_MESSAGE);
        }
        return rootDirectory;
    }

    @Override
    public Optional<Project> parent() {
        return Optional.ofNullable(parent);
    }

    @Override
    @Nonnull
    public List<Profile> declaredProfiles() {
        return model().getProfiles();
    }

    @Override
    @Nonnull
    public List<Profile> effectiveProfiles() {
        return effectiveProfiles;
    }

    @Override
    @Nonnull
    public List<Profile> declaredActiveProfiles() {
        return declaredActiveProfiles;
    }

    @Override
    @Nonnull
    public List<Profile> effectiveActiveProfiles() {
        return effectiveActiveProfiles;
    }

    @Nonnull
    private DependencyCoordinates toDependency(org.apache.maven.api.model.Dependency dependency) {
        return new DependencyCoordinates() {
            @Override
            public String groupId() {
                return dependency.getGroupId();
            }

            @Override
            public String artifactId() {
                return dependency.getArtifactId();
            }

            @Override
            public String classifier() {
                String classifier = dependency.getClassifier();
                if (classifier == null || classifier.isEmpty()) {
                    classifier = type().getClassifier();
                    if (classifier == null) {
                        classifier = "";
                    }
                }
                return classifier;
            }

            @Override
            public VersionConstraint versionConstraint() {
                return session.parseVersionConstraint(dependency.getVersion());
            }

            @Override
            public String extension() {
                return type().getExtension();
            }

            @Override
            public Type type() {
                String type = dependency.getType();
                return session.requireType(type);
            }

            @Nonnull
            @Override
            public DependencyScope scope() {
                String scope = dependency.getScope();
                if (scope == null) {
                    scope = "";
                }
                return session.requireDependencyScope(scope);
            }

            @Override
            public Boolean optional() {
                return dependency.isOptional();
            }

            @Nonnull
            @Override
            public Collection<Exclusion> exclusions() {
                return new MappedCollection<>(dependency.getExclusions(), this::toExclusion);
            }

            private Exclusion toExclusion(org.apache.maven.api.model.Exclusion exclusion) {
                return new Exclusion() {
                    @Nullable
                    @Override
                    public String groupId() {
                        return exclusion.getGroupId();
                    }

                    @Nullable
                    @Override
                    public String artifactId() {
                        return exclusion.getArtifactId();
                    }
                };
            }
        };
    }

    record State(
            String groupId,
            String artifactId,
            String version,
            Packaging packaging,
            List<org.eclipse.aether.artifact.Artifact> artifacts,
            boolean producesMainArtifact,
            Model model,
            Path pomPath,
            Path basedir,
            Path rootDirectory,
            Project parent,
            List<Profile> effectiveProfiles,
            List<Profile> declaredActiveProfiles,
            List<Profile> effectiveActiveProfiles) {}
}
