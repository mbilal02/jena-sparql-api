package org.aksw.jena_sparql_api.changeset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.commons.collections.diff.Diff;
import org.aksw.commons.util.strings.StringUtils;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.lookup.LookupService;
import org.aksw.jena_sparql_api.lookup.LookupServiceSparqlQuery;
import org.aksw.jena_sparql_api.lookup.LookupServiceTransformValue;
import org.aksw.jena_sparql_api.lookup.ResultSetPart;
import org.aksw.jena_sparql_api.mapper.BindingMapperProjectVar;
import org.aksw.jena_sparql_api.mapper.FunctionBindingMapper;
import org.aksw.jena_sparql_api.update.SetGraph;
import org.aksw.jena_sparql_api.update.UpdateUtils;
import org.aksw.jena_sparql_api.utils.GraphUtils;
import org.aksw.jena_sparql_api.utils.Vars;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.graph.GraphFactory;
import com.hp.hpl.jena.sparql.util.ModelUtils;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;


public class ChangeSetUtils {

    public static final Query queryMostRecentChangeSet = QueryFactory.parse(new Query(), "Prefix cs: <http://purl.org/vocab/changeset/schema#> Select ?s { ?s cs:subjectOfChange ?o . Optional { ?x cs:precedingChangeSet ?s } . Filter(!Bound(?x)) }", "http://example.org/", Syntax.syntaxSPARQL_10);


    public static LookupService<Node, Node> createLookupServiceMostRecentChangeSet(QueryExecutionFactory qef) {

        LookupService<Node, ResultSetPart> ls = new LookupServiceSparqlQuery(qef, queryMostRecentChangeSet, Vars.o);

        // We filter by o, but project by s
        LookupService<Node, Node> result =
                LookupServiceTransformValue.create(ls, Functions.compose(
                        FunctionBindingMapper.create(BindingMapperProjectVar.create(Vars.s)),
                        FunctionResultSetPartFirstRow.fn));

        return result;
    }

    /**
     * Check if all triples' subject equal the subjectOfChange
     * @param cs
     * @return
     */
    public static boolean isValid(ChangeSet cs) {
        String str = cs.getSubjectOfChange();
        Node s = NodeFactory.createURI(str);

        boolean isValidAdded = isSubjectOfAllTriples(s, cs.getAddition());
        boolean isValidRemoved = isSubjectOfAllTriples(s, cs.getAddition());

        boolean result = isValidAdded && isValidRemoved;

        return result;
    }

    public static boolean isSubjectOfTriple(Node s, Triple triple) {
        boolean result = triple.getSubject().equals(s);
        return result;
    }

    public static boolean isSubjectOfAllTriples(Node s, Graph g) {
        boolean result = true;

        ExtendedIterator<Triple> it = g.find(Node.ANY, Node.ANY, Node.ANY);
        try {
            while(it.hasNext()) {
                Triple triple = it.next();

                // TODO We could implement this as a filter
                boolean isValid = isSubjectOfTriple(s, triple);
                if(!isValid) {
                    result = false;
                    break;
                }
            }
        } finally {
            it.close();
        }

        return result;
    }

    public static void writeLangMap(Model model, Resource s, Property p, Map<String, String> langToText) {
        if(langToText != null) {
            for(Entry<String, String> entry : langToText.entrySet()) {
                String lang = entry.getKey();
                String text = entry.getValue();

                // TODO Is this check needed?

                Literal o = lang == null || lang.trim().isEmpty()
                        ? model.createLiteral(text)
                        : model.createLiteral(text, lang);

                model.add(s, p, o);
            }
        }
    }

    public static void writeReifiedGraph(Model model, Graph graph, Function<Triple, Node> tripleToSubject) {
        ExtendedIterator<Triple> it = graph.find(Node.ANY, Node.ANY, Node.ANY);
        try {
            while(it.hasNext()) {
                Triple triple = it.next();
                Node s = tripleToSubject.apply(triple);
                writeReifiedTriple(model, s, triple);
            }
        } finally {
            it.close();
        }
    }


    public static void writeReifiedTriple(Model model, Node s, Triple triple) {
        RDFNode tmp = ModelUtils.convertGraphNodeToRDFNode(s, model);
        Resource n = tmp.asResource();
        Statement stmt = ModelUtils.tripleToStatement(model, triple);
        writeReifiedStatement(model, n, stmt);
    }

    public static void writeReifiedStatement(Model model, Resource s, Statement stmt) {
        model.add(s, RDF.type, RDF.Statement);
        model.add(s, RDF.subject, stmt.getSubject());
        model.add(s, RDF.predicate, stmt.getPredicate());
        model.add(s, RDF.object, stmt.getObject());
    }

    public static void write(Model model, ChangeSet cs) {
        Resource s = model.createResource(cs.getUri());

        model.add(s, RDF.type, CS.ChangeSet);
        //writeLangMap(model, s, CS.changeReason, cs.getChangeReason());

        ChangeSetMetadata md = cs.getMetadata();

        model.add(s, CS.changeReason, model.createLiteral(md.getChangeReason()));
        model.add(s, CS.createdDate, model.createTypedLiteral(md.getCreatedDate()));
        model.add(s, CS.creatorName, model.createLiteral(md.getCreatorName()));

        if(cs.getPrecedingChangeSet() != null) {
            model.add(s, CS.precedingChangeSet, model.createResource(cs.getPrecedingChangeSet()));
        }

        String prefix = "http://example.org/";

        for(Triple triple : SetGraph.wrap(cs.getAddition())) {
            String uri = prefix + FN_TripleToMd5.fn.apply(triple);
            Resource o = ResourceFactory.createResource(uri);
            Statement stmt = ModelUtils.tripleToStatement(model, triple);
            writeReifiedStatement(model, o, stmt);
            model.add(s, CS.addition, o);
            model.add(s, CS.statement, o);
        }

        for(Triple triple : SetGraph.wrap(cs.getRemoval())) {
            String uri = prefix + FN_TripleToMd5.fn.apply(triple);
            Resource o = ResourceFactory.createResource(uri);
            Statement stmt = ModelUtils.tripleToStatement(model, triple);
            writeReifiedStatement(model, o, stmt);
            model.add(s, CS.removal, o);
            model.add(s, CS.statement, o);
        }
    }

    public static UpdateRequest createUpdateRequest(ChangeSetMetadata metadata,
            QueryExecutionFactory qef,
            Diff<? extends Iterable<Quad>> diff,
            String prefix) {
        Diff<Graph> d = new Diff<Graph>(GraphFactory.createGraphMem(), GraphFactory.createGraphMem(), null);

        for(Quad quad : diff.getAdded()) {
            d.getAdded().add(quad.asTriple());
        }

        for(Quad quad : diff.getRemoved()) {
            d.getRemoved().add(quad.asTriple());
        }

        UpdateRequest result = createUpdateRequestGraph(metadata, qef, d, prefix);
        return result;
    }


    public static UpdateRequest createUpdateRequestGraph(ChangeSetMetadata metadata,
            QueryExecutionFactory qef,
            Diff<Graph> diff,
            String prefix) {

        Map<Node, ChangeSet> subjectToChangeSet = createChangeSets(qef, metadata, diff, prefix);

        Model model = ModelFactory.createDefaultModel();

        for(ChangeSet cs : subjectToChangeSet.values()) {
            write(model, cs);
        }

        UpdateRequest result = UpdateUtils.createUpdateRequest(model, null);
        return result;
    }


//    public Map<Node, ChangeSet> apply(Diff<Graph> diff) {

    public static Map<Node, ChangeSet> createChangeSets(QueryExecutionFactory qef, ChangeSetMetadata metadata, Diff<Graph> diff, String prefix) {
        LookupService<Node, Node> precedingChangeSetLs = ChangeSetUtils.createLookupServiceMostRecentChangeSet(qef);

        Graph added = diff.getAdded();
        Graph removed = diff.getAdded();

        Map<Node, Graph> subjectToAdded = GraphUtils.indexBySubject(added);
        Map<Node, Graph> subjectToRemoved = GraphUtils.indexBySubject(removed);

        Set<Node> subjects = new HashSet<Node>();
        subjects.addAll(subjectToAdded.keySet());
        subjects.addAll(subjectToRemoved.keySet());

        Map<Node, Node> subjectToRecentChangeSet = precedingChangeSetLs.apply(subjects);

        Map<Node, ChangeSet> result = new HashMap<Node, ChangeSet>();
        for(Node s : subjects) {
            String subjectOfChange = s.getURI();

            Node precedingId = subjectToRecentChangeSet.get(s);
            String precedingChangeSet = precedingId == null ? null : precedingId.getURI();

            String localName = StringUtils.md5Hash(subjectOfChange + " " + precedingId);
            String uri = prefix + localName;

            Graph addedGraph = subjectToAdded.get(s);
            Graph removedGraph = subjectToRemoved.get(s);

            addedGraph = addedGraph == null ? GraphFactory.createGraphMem() : addedGraph;
            removedGraph = removedGraph == null ? GraphFactory.createGraphMem() : removedGraph;

            ChangeSet cs = new ChangeSet(metadata, uri, precedingChangeSet, subjectOfChange, addedGraph, removedGraph);

            result.put(s, cs);
        }

        return result;
    }

//  public static Query createQueryPrecedingChangeSet() {
    //String str = "Prefix cs: <http://purl.org/vocab/changeset/schema#> Select ?s { ?s cs:subjectOfChange <" + uri + "> ; cs:createdDate ?d } Order By Desc(?d) Limit 1";

    // Get the changeset for a subject that does not (yet) occurr as a preceding changeset
    /*
    Query query = new Query();
    query.setQuerySelectType();
    query.getProject().add(Vars.s);

    ElementGroup queryPattern = new ElementGroup()

    ElementTriplesBlock b1 = new ElementTRip

    query.setQueryPattern(queryPattern);
*/

    //String queryString = "{ Select ?s { ?s <http://purl.org/vocab/changeset/schema#subjectOfChange> ?o . Optional { ?x <http://purl.org/vocab/changeset/schema#precedingChangeSet> ?s } . Filter(!Bound(?x)) } Order By Desc(?d) Limit 1 }";
    //Concept concept = Concept.create(queryString, "s");
    //LookupService ls = new LookupServiceSparqlQuery(qef, )

//}

}

