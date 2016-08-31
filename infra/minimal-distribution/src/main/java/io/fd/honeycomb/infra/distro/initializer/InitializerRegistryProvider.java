/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.infra.distro.initializer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule;
import java.util.HashSet;
import java.util.Set;

public final class InitializerRegistryProvider extends ProviderTrait<InitializerRegistry> {

    @Inject
    @Named(ContextPipelineModule.HONEYCOMB_CONTEXT)
    private DataTreeInitializer contextInitializer;
    @Inject
    @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)
    private DataTreeInitializer configInitializer;
    @Inject(optional = true)
    private Set<DataTreeInitializer> pluginInitializers = new HashSet<>();

    @Override
    protected InitializerRegistryAdapter create() {
        return new InitializerRegistryAdapter(contextInitializer, configInitializer, pluginInitializers);
    }
}