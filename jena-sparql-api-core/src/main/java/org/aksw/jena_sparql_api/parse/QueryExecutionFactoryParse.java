package org.aksw.jena_sparql_api.parse;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDecorator;

import com.google.common.base.Function;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;

public class QueryExecutionFactoryParse
	extends QueryExecutionFactoryDecorator
{
	protected Function<String, Query> parser;

	public QueryExecutionFactoryParse(QueryExecutionFactory decoratee, Function<String, Query> parser) {
		super(decoratee);
		this.parser = parser;
	}

	@Override
	public QueryExecution createQueryExecution(String queryString) {
		Query query = parser.apply(queryString);
		QueryExecution result = createQueryExecution(query);
		return result;
	}

}
