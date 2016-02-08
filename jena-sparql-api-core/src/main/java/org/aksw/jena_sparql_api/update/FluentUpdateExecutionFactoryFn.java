package org.aksw.jena_sparql_api.update;

import org.aksw.jena_sparql_api.core.FluentFnBase;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactoryFn;
import org.aksw.jena_sparql_api.core.UpdateExecutionFactory;
import org.aksw.jena_sparql_api.core.UpdateExecutionFactoryDatasetDescription;

import com.google.common.base.Function;
import org.apache.jena.sparql.core.DatasetDescription;

public class FluentUpdateExecutionFactoryFn<P>
    extends FluentFnBase<UpdateExecutionFactory, P>
{

    public FluentUpdateExecutionFactoryFn<P> withDatasetDescription(final String withIri, final DatasetDescription datasetDescription) {
        compose(new Function<UpdateExecutionFactory, UpdateExecutionFactory>() {
            @Override
            public UpdateExecutionFactory apply(UpdateExecutionFactory uef) {
            	UpdateExecutionFactory r = new UpdateExecutionFactoryDatasetDescription(uef, withIri, datasetDescription);
                return r;
            }
        });
        return this;
    }

}