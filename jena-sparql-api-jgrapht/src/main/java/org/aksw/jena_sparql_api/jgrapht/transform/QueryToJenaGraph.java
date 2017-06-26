package org.aksw.jena_sparql_api.jgrapht.transform;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.aksw.commons.collections.utils.StreamUtils;
import org.aksw.jena_sparql_api.jgrapht.wrapper.PseudoGraphJenaGraph;
import org.aksw.jena_sparql_api.utils.DnfUtils;
import org.aksw.jena_sparql_api.utils.ExprUtils;
import org.aksw.jena_sparql_api.utils.Vars;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Equals;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.NodeUtils;
import org.apache.jena.vocabulary.RDFS;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphMapping;
import org.jgrapht.Graphs;
import org.jgrapht.alg.isomorphism.IsomorphicGraphMapping;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


/**
 * We can directly convert BGPs and expressions to an RDF graph.
 * The sub graph isomorphism check can be done using the jena wrapper for jgrapht.
 *
 *
 * @author raven
 *
 */
public class QueryToJenaGraph {

    public static Node unionMember = NodeFactory.createURI("http://ex.org/unionMember");
    public static Node quadBlockMember = NodeFactory.createURI("http://ex.org/quadBlockMember");
    public static Node filtered = NodeFactory.createURI("http://ex.org/filtered");

    public static Node TP = NodeFactory.createURI("http://ex.org/TP");
    //public static Node TP = NodeFactory.createURI("http://ex.org/TP");

    public static Node g = NodeFactory.createURI("http://ex.org/g");
    public static Node s = NodeFactory.createURI("http://ex.org/s");
    public static Node p = NodeFactory.createURI("http://ex.org/p");
    public static Node o = NodeFactory.createURI("http://ex.org/o");

    public static Node[] gspo = {g, s, p, o};

    public static Node argNode = NodeFactory.createURI("arg://unordered"); // unordered argument

    public static Node cm = NodeFactory.createURI("http://ex.org/cm"); // conjunction member
    public static Node dm = NodeFactory.createURI("http://ex.org/dm"); // disjunction member

    public static Node ev = NodeFactory.createURI("http://ex.org/ea"); // equality var argument
    public static Node ec = NodeFactory.createURI("http://ex.org/ea"); // equality const argument


    //public static unionToGraph(DirectedGraph<Node, LabeledEdge<Node, Node>>)

    public static void addEdge(Graph graph, Node source, Node edgeLabel, Node target) {
        graph.add(new Triple(source, edgeLabel, target));
    }

    public static Node addQuad(Graph graph, Quad quad, Supplier<Node> nodeSupplier) {
        // Allocate a fresh node for the quad
        Node quadNode = nodeSupplier.get();

        g = quad.getGraph();
        if(!Quad.defaultGraphIRI.equals(g) && !Quad.defaultGraphNodeGenerated.equals(g)) {
            addEdge(graph, quadNode, g, quad.getGraph());
        }
        addEdge(graph, quadNode, s, quad.getSubject());
        addEdge(graph, quadNode, p, quad.getPredicate());
        addEdge(graph, quadNode, o, quad.getObject());

        return quadNode;
    }


    /**
     * Connects every quad's node to a newly allocated node representing the quad block
     *
     * @param graph
     * @param quads
     * @return
     */
    public static Node quadsToGraphNode(Graph graph, Collection<Quad> quads, Supplier<Node> nodeSupplier) {
        Node quadBlockNode = nodeSupplier.get();
        //graph = new DirectedPseudograph<>(LabeledEdgeImpl.class);
        for(Quad quad : quads) {
            Node quadNode = addQuad(graph, quad, nodeSupplier);
            addEdge(graph, quadBlockNode, quadBlockMember, quadNode);
        }

        return quadBlockNode;
    }


    public static void quadsToGraph(Graph graph, Collection<Quad> quads, Supplier<Node> nodeSupplier) {
        //graph = new DirectedPseudograph<>(LabeledEdgeImpl.class);
        for(Quad quad : quads) {
            addQuad(graph, quad, nodeSupplier);
        }
    }


    public static Node exprToGraph(Graph graph, Expr expr, Supplier<Node> nodeSupplier) {
        Node result;
        if(expr.isConstant()) {
            result = expr.getConstant().asNode();
        } else if(expr.isFunction()) {
            result = nodeSupplier.get();

            boolean isCommutative = expr instanceof E_Equals;

            ExprFunction ef = expr.getFunction();
            String fnId = ExprUtils.getFunctionId(ef);

            graph.add(new Triple(result, RDFS.label.asNode(), NodeFactory.createLiteral(fnId)));

            List<Expr> args = ef.getArgs();
            int n = args.size();
            for(int i = 0; i < n; ++i) {
                Expr arg = args.get(i);
                Node argNode = exprToGraph(graph, arg, nodeSupplier);

                Node p = isCommutative ? argNode : NodeFactory.createURI("arg://" + i);

                graph.add(new Triple(result, p, argNode));
            }

        } else if(expr.isVariable()){
            result = expr.asVar();
        } else {
            throw new RuntimeException("should not happen");
        }

        return result;
    }


    public static void dnfToGraph(Graph graph, Collection<? extends Collection<? extends Expr>> dnf, Supplier<Node> nodeSupplier) {
        Node orNode = nodeSupplier.get();
        for(Collection<? extends Expr> clause : dnf) {
            // Create a blank node for each clause

            Node andNode = nodeSupplier.get();
            addEdge(graph, orNode, dm, andNode);
            for(Expr e : clause) {
                // Create another blank node for each equality instance
                // TODO This would be another type of construction: Actually the edge labels are already sufficient for discrimination of equals expressions
                //Node equalsNode = nodeSupplier.get();

                Node eNode = exprToGraph(graph, e, nodeSupplier);
                addEdge(graph, andNode, cm, eNode);
            }
        }
    }

    // Filters: Extract all equality filters
    public static void equalExprsToGraphOld(Graph graph, Collection<? extends Collection<? extends Expr>> dnf, Supplier<Node> nodeSupplier, Map<Var, Node> varToNode) {
        Set<Map<Var, NodeValue>> maps = DnfUtils.extractConstantConstraints(dnf);

        for(Map<Var, NodeValue> map : maps) {
            // Create a blank node for each clause
            Node orNode = nodeSupplier.get();

            for(Entry<Var, NodeValue> e : map.entrySet()) {
                // Create another blank node for each equality instance
                // TODO This would be another type of construction: Actually the edge labels are already sufficient for discrimination of equals expressions
                boolean createNodesForEqualities = true; // Does not work with false as search space gets too big
                if(createNodesForEqualities) {
                    Node equalsNode = nodeSupplier.get();

                    Var v = e.getKey();
                    Node c = e.getValue().getNode();

                    addEdge(graph, equalsNode, ev, v);
                    addEdge(graph, c, ec, equalsNode);
                    addEdge(graph, orNode, dm, equalsNode);

                } else {

                    Var v = e.getKey();
                    Node c = e.getValue().getNode();

                    addEdge(graph, orNode, Vars.x, v);
                    addEdge(graph, c, Vars.y, orNode);
                }
            }
        }
    }


    public static Stream<BiMap<Node, Node>> match(BiMap<Node, Node> baseIso, Graph a, Graph b) {
        DirectedGraph<Node, Triple> atmp = new PseudoGraphJenaGraph(a);
        DirectedGraph<Node, Triple> btmp = new PseudoGraphJenaGraph(b);

        // Create a copy of the wrapped graph, as we will have to re-compute a lot of data with the view
        DirectedGraph<Node, Triple> adg = new SimpleDirectedGraph<>(Triple.class);
        DirectedGraph<Node, Triple> bdg = new SimpleDirectedGraph<>(Triple.class);

        Graphs.addGraph(adg, atmp);
        Graphs.addGraph(bdg, btmp);

        Stream<BiMap<Node, Node>> result = match(baseIso, adg, bdg);
        return result;
    }


    public static Stream<Map<Var, Var>> match(Graph a, Map<Node, Var> aNodeToVar, Graph b, Map<Node, Var> bNodeToVar) {
        DirectedGraph<Node, Triple> adg = new PseudoGraphJenaGraph(a);
        DirectedGraph<Node, Triple> bdg = new PseudoGraphJenaGraph(b);

        Stream<Map<Var, Var>> result = matchOld(adg, aNodeToVar, bdg, bNodeToVar);
        return result;
    }


//    public static int getLevel(Node node) {
//        int result
//            = node == null ? 1
//            : node.isLiteral() ? 2
//            : node.isURI() ? 3
//            : node.isBlank() ? 4
//            : node.isVariable() ? 5
//            : 0;
//
//        return result;
//    }
//
//
//    public static int compareByString(Object i, Object j) {
//        String x = Objects.toString(i);
//        String y = Objects.toString(j);
//
//        int result = x.compareTo(y);
//        return result;
//    }

    public static int compareNodes(BiMap<Node, Node> baseIso, Node i, Node j) {
        int result;
        // If nodes were mapped by the isomorphism, they need to be equal
        // otherwise, we treat vars and blanknodes as always equal among each other.

        Node ii;
        Node jj;

        if((ii = baseIso.get(i)) != null) {
            result = Objects.equals(ii, j) ? 0 : NodeUtils.compareRDFTerms(ii, j);
        } else if((jj = baseIso.inverse().get(j)) != null) {
            result = Objects.equals(i, jj) ? 0 : NodeUtils.compareRDFTerms(i, jj);
        } else {
            result =  (i.isVariable() && j.isVariable()) || (i.isBlank() && j.isBlank())
                    ? 0
                    : NodeUtils.compareRDFTerms(i, j);
        }

//        System.err.println("NodeCmp [" + result + "] for " + i + " <-> " + j);
        return result;
    }


    public static Iterator<GraphMapping<Node, Triple>> matchIt(
            BiMap<Node, Node> baseIso, // view to user query
            DirectedGraph<Node, Triple> a,
            DirectedGraph<Node, Triple> b) {
        Comparator<Node> nodeCmp = (x, y) -> QueryToJenaGraph.compareNodes(baseIso, x, y);
        Comparator<Triple> edgeCmp = (x, y) -> QueryToJenaGraph.compareNodes(baseIso, x.getPredicate(), y.getPredicate());

        VF2SubgraphIsomorphismInspector<Node, Triple> inspector =
                new VF2SubgraphIsomorphismInspector<>(b, a, nodeCmp, edgeCmp, true);
        Iterator<GraphMapping<Node, Triple>> result = inspector.getMappings();

        return result;
    }

    public static Comparator<Node> createNodeComparator(BiMap<Node, Node> baseIso) {
        Comparator<Node> result = (x, y) -> QueryToJenaGraph.compareNodes(baseIso, x, y);
        return result;
    }

    public static Comparator<Triple> createEdgeComparator(BiMap<Node, Node> baseIso) {
        Comparator<Triple> result = (x, y) -> QueryToJenaGraph.compareNodes(baseIso, x.getPredicate(), y.getPredicate());
        return result;
    }


    public static Stream<BiMap<Node, Node>> match(
            BiMap<Node, Node> baseIso, // view to user query
            DirectedGraph<Node, Triple> a,
            DirectedGraph<Node, Triple> b) {

//        a.edgeSet().forEach(e -> System.out.println("a: " + e));
//        b.edgeSet().forEach(e -> System.out.println("b: " + e));
//        System.out.println("a: "+ a.vertexSet());
//        System.out.println("b: "+ b.vertexSet());
//        System.err.println("NodeCmp under " + baseIso);

        Comparator<Node> nodeCmp = createNodeComparator(baseIso);
        Comparator<Triple> edgeCmp = createEdgeComparator(baseIso);

        VF2SubgraphIsomorphismInspector<Node, Triple> inspector =
                new VF2SubgraphIsomorphismInspector<>(b, a, nodeCmp, edgeCmp, true);
        Iterator<GraphMapping<Node, Triple>> it = inspector.getMappings();

        Stream<BiMap<Node, Node>> result = StreamUtils.stream(it)
                .map(m -> (IsomorphicGraphMapping<Node, Triple>)m)
                .map(m -> {
                    BiMap<Node, Node> nodeMap = HashBiMap.create();//new HashMap<>();
                    for(Node bNode : b.vertexSet()) {
//                        if(bNode.isVariable()) {
                            if(m.hasVertexCorrespondence(bNode)) {
                                Node aNode = m.getVertexCorrespondence(bNode, true);
//                                if(aNode.isVariable()) {
                                    nodeMap.put(aNode, bNode);
//                                }
                            }
//                        }
                    }
//                    System.out.println("Created map: " + nodeMap);


                    //System.out.println("Mapping: " + m);
//                    Map<Var, Var> varMap = null;
//                    for(Node bNode : b.vertexSet()) {
//                        if(bNode.isVariable()) {
//                            if(m.hasVertexCorrespondence(bNode)) {
//                                Node aNode = m.getVertexCorrespondence(bNode, true);
//                                if(aNode.isVariable()) {
//                                    varMap = varMap == null ? new HashMap<>() : varMap;
//                                    varMap.put((Var)bNode, (Var)aNode);
//                                } else {
//                                    break;
//                                }
//                            }
//                        }
//                    }

                    return nodeMap;
                });
                //filter(x -> x != null)
                // .distinct(); // not sure

        return result;
    }

    public static Stream<Map<Var, Var>> matchOld(
            DirectedGraph<Node, Triple> a,
            Map<Node, Var> aNodeToVar,
            DirectedGraph<Node, Triple> b,
            Map<Node, Var> bNodeToVar) {

//        System.out.println("EDGES:");
//        b.edgeSet().forEach(System.out::println);
//        System.out.println("done with edges");

        Comparator<Node> nodeCmp = (x, y) -> {
            int  r = (x.isVariable() && y.isVariable()) || (x.isBlank() && y.isBlank())
                    ? 0
                    : x.toString().compareTo(y.toString());
            //System.err.println("NodeCmp [" + r + "] for " + x + " <-> " + y);
            return r;
        };

        Comparator<Triple> edgeCmp = (x, y) -> {
            int r = x.getPredicate().toString().compareTo(y.getPredicate().toString());
            //System.err.println("EdgeCmp: [" + r + "] for " + x + " <-> " + y);
            return r;
        };



        VF2SubgraphIsomorphismInspector<Node, Triple> inspector = new VF2SubgraphIsomorphismInspector<>(b, a, nodeCmp, edgeCmp, true);
        Iterator<GraphMapping<Node, Triple>> it = inspector.getMappings();

        Stream<Map<Var, Var>> result = StreamUtils.stream(it)
                .map(m -> (IsomorphicGraphMapping<Node, Triple>)m)
                .map(m -> {
                    //System.out.println("Mapping: " + m);
                    Map<Var, Var> varMap = null;
                    for(Node bNode : b.vertexSet()) {
                        Var bVar = bNodeToVar.get(bNode);
                        if(bVar != null) {
                            if(m.hasVertexCorrespondence(bNode)) {
                                Node aNode = m.getVertexCorrespondence(bNode, true);
                                Var aVar = aNodeToVar.get(aNode);
                                if(aVar != null) {
                                    varMap = varMap == null ? new HashMap<>() : varMap;
                                    varMap.put(bVar, aVar);
                                } else {
                                    break;
                                }
                            }
                        }
                    }

                    return varMap;
                }).
                filter(x -> x != null);

        return result;
    }

}

