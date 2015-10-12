package org.aksw.jena_sparql_api.mapper;

import java.util.Set;

import org.aksw.jena_sparql_api.utils.QuadPatternUtils;
import org.aksw.jena_sparql_api.utils.Vars;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.QuadPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.core.VarExprList;
import com.hp.hpl.jena.sparql.syntax.Template;

public class MappedQueryUtils {

    public static MappedQuery<DatasetGraph> fromConstructQuery(Query query, Var partitionVar) {
        PartitionedQuery partQuery = new PartitionedQuery(query, partitionVar);
        MappedQuery<DatasetGraph> result = fromConstructQuery(partQuery);
        return result;
    }

    public static MappedQuery<DatasetGraph> fromConstructQuery(PartitionedQuery partQuery) {
        MappedQuery<DatasetGraph> result;

        Query query = partQuery.getQuery().cloneQuery();

        if(query.isConstructType()) {
            Template template = query.getConstructTemplate();
            QuadPattern qp = QuadPatternUtils.toQuadPattern(Quad.defaultGraphNodeGenerated, template.getBGP());
            Agg<DatasetGraph> agg = AggDatasetGraph.create(qp);


            query.setQuerySelectType();
            query.setQueryResultStar(false);
            VarExprList project = query.getProject();
            project.getVars().clear();
            project.getExprs().clear();

            Set<Var> vars = agg.getDeclaredVars();

            Var partVar = partQuery.getVar();
            if(!vars.contains(partVar)) {
                project.add(partVar);
            }

            for(Var var : vars) {
                project.add(var);
            }

//            QuadPattern qp = new QuadPattern();
//            qp.add(new Quad(Quad.defaultGraphNodeGenerated, Vars.s, Vars.p, Vars.o));
//            Agg<DatasetGraph> agg = AggDatasetGraph.create(qp);

            PartitionedQuery pq = new PartitionedQuery(query, partVar);

            result = new MappedQuery<DatasetGraph>(pq, agg);

        } else {
            throw new RuntimeException("Only construct query supported right now");
        }


        return result;
    }
}
