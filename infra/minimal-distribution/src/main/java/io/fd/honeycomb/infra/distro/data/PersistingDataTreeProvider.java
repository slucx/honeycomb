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

package io.fd.honeycomb.infra.distro.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.data.impl.PersistingDataTreeAdapter;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule;
import java.nio.file.Paths;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;

public abstract class PersistingDataTreeProvider extends ProviderTrait<DataTree> {

    @Inject
    private SchemaService schemaService;
    @Inject
    protected HoneycombConfiguration config;

    public PersistingDataTreeAdapter create() {
        return new PersistingDataTreeAdapter(getDelegate(), schemaService, Paths.get(getPath()));
    }

    public abstract String getPath();

    public abstract TreeType getType();

    public abstract DataTree getDelegate();

    public static final class ConfigPersistingDataTreeProvider extends PersistingDataTreeProvider {

        @Inject
        @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG_NONPERSIST)
        private DataTree delegate;

        public String getPath() {
            return config.peristConfigPath;
        }

        public TreeType getType() {
            return TreeType.CONFIGURATION;
        }

        public DataTree getDelegate() {
            return delegate;
        }
    }

    public static final class ContextPersistingDataTreeProvider extends PersistingDataTreeProvider {

        @Inject
        @Named(ContextPipelineModule.HONEYCOMB_CONTEXT_NOPERSIST)
        private DataTree delegate;

        public String getPath() {
            return config.peristContextPath;
        }

        public TreeType getType() {
            return TreeType.OPERATIONAL;
        }

        public DataTree getDelegate() {
            return delegate;
        }
    }
}