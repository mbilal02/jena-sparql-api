package org.aksw.jena_sparql_api.lookup;

import java.util.List;

import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.concepts.Relation;
import org.aksw.jena_sparql_api.concepts.RelationUtils;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.mapper.Agg;
import org.aksw.jena_sparql_api.mapper.AggList;
import org.aksw.jena_sparql_api.mapper.AggLiteral;
import org.aksw.jena_sparql_api.mapper.BindingMapperProjectVar;
import org.aksw.jena_sparql_api.mapper.FunctionResultSetAggregate;
import org.aksw.jena_sparql_api.mapper.MappedConcept;
import org.aksw.jena_sparql_api.mapper.MappedQuery;
import org.aksw.jena_sparql_api.mapper.PartitionedQuery1;
import org.aksw.jena_sparql_api.utils.ResultSetPart;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;

public class LookupServiceUtils {
//
//    public static <T> LookupService<Node, T> createLookupService2(QueryExecutionFactory sparqlService, MappedConcept<? extends Map<? extends Node, T>> mappedConcept) {
//        Concept concept = mappedConcept.getConcept();
//        Query query = concept.asQuery();
//        query.setQueryResultStar(true);
//
//        // TODO Set up a projection using the grouping variable and the variables referenced by the aggregator
//
//        LookupService<Node, ResultSetPart> base = new LookupServiceSparqlQuery(sparqlService, query, concept.getVar());
//
//        FunctionResultSetAggregate<? extends Map<? extends Node, T>> t1 = FunctionResultSetAggregate.create(mappedConcept.getAggregator());
//
//
//
//        Function<Map<? extends Node, T>, Map<Node, T>> fn = new Function<Map<? extends Node, T>, Map<Node, T>>() {
//
//            @Override
//            public Map<Node, T> apply(Map<? extends Node, T> input) {
//
//            }
//        };
//
//
//        LookupService<Node, T> result = LookupServiceTransformValue.create(base, transform);
//        return result;
//    }
    public static <T> LookupService<Node, List<Node>> createLookupService(QueryExecutionFactory qef, Relation relation) {
        Var sourceVar = relation.getSourceVar();

        AggList<Node> agg = AggList.create(AggLiteral.create(BindingMapperProjectVar.create(relation.getTargetVar())));
        Query query = RelationUtils.createQuery(relation);
        MappedQuery<List<Node>> mappedQuery = MappedQuery.create(query, sourceVar, agg);
        LookupService<Node, List<Node>> result = LookupServiceUtils.createLookupService(qef, mappedQuery);

        return result;
    }

    public static <T> LookupService<Node, T> createLookupService(QueryExecutionFactory sparqlService, MappedQuery<T> mappedQuery) {
        PartitionedQuery1 partQuery = mappedQuery.getPartQuery();
        Query query = partQuery.getQuery();
        Var partVar = partQuery.getPartitionVar();
        Agg<T> agg = mappedQuery.getAgg();

        LookupService<Node, ResultSetPart> base = new LookupServiceSparqlQuery(sparqlService, query, partVar);
        FunctionResultSetAggregate<T> transform = FunctionResultSetAggregate.create(agg);
        LookupService<Node, T> result = LookupServiceTransformValue.create(base, transform);
        return result;
    }

    public static <T> LookupService<Node, T> createLookupService(QueryExecutionFactory sparqlService, MappedConcept<T> mappedConcept) {

        Concept concept = mappedConcept.getConcept();
        Query query = concept.asQuery();
        query.setQueryResultStar(true);

        // TODO Set up a projection using the grouping variable and the variables referenced by the aggregator

        LookupService<Node, ResultSetPart> base = new LookupServiceSparqlQuery(sparqlService, query, concept.getVar());

        FunctionResultSetAggregate<T> transform = FunctionResultSetAggregate.create(mappedConcept.getAggregator());

        LookupService<Node, T> result = LookupServiceTransformValue.create(base, transform);
        return result;
    }
}
