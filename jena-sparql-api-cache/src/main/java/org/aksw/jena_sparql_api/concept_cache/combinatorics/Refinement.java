package org.aksw.jena_sparql_api.concept_cache.combinatorics;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.aksw.isomorphism.Problem;
import org.aksw.jena_sparql_api.utils.MapUtils;
import org.aksw.jena_sparql_api.utils.NodeTransformSignaturize;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.NodeTransform;
import org.apache.jena.sparql.graph.NodeTransformLib;

import com.codepoetics.protonpack.functions.TriFunction;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Refinement {

    public static Problem<Map<Var, Var>> groupToProblem(Entry<Set<Quad>, Set<Quad>> group, Map<Var, Var> newBase) {
        Problem<Map<Var, Var>> result = new ProblemVarMappingQuad(group.getKey(), group.getValue(), newBase);
        return result;
    }

    public static <T> Collection<Problem<Map<Var, Var>>> groupsToProblems(
            Map<?, Entry<Set<T>, Set<T>>> groups,
            Map<Var, Var> newBase,
            TriFunction<Set<T>, Set<T>, Map<Var, Var>, Problem<Map<Var, Var>>> groupToProblem) {
        Collection<Problem<Map<Var, Var>>> result = groups.values().stream()
                .map(e -> groupToProblem.apply(e.getKey(), e.getValue(), newBase)) //new ProblemVarMappingQuad(e.getKey(), e.getValue(), newBase))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * Signature construction:
     * - Variables that were already renamed must not be remapped again
     * - Variables of the 'as' that clash with the renaming (ps.values()) must be renamed first
     *
     *
     */
    public static <T> Collection<Problem<Map<Var, Var>>> refineGeneric(
            Collection<? extends T> as,
            Collection<? extends T> bs,
            Map<Var, Var> baseSolution,
            Map<Var, Var> partialSolution,
            BiFunction<NodeTransform, T, T> transformer,
            TriFunction<Set<T>, Set<T>, Map<Var, Var>, Problem<Map<Var, Var>>> groupToProblem
            )
    {
        Map<Var, Var> newBase = MapUtils.mergeIfCompatible(baseSolution, partialSolution);
        Map<T, Entry<Set<T>, Set<T>>> groups = Refinement.refineGeneric(as, bs, newBase, transformer);
        Collection<Problem<Map<Var, Var>>> result = groupsToProblems(groups, newBase, groupToProblem);

        return result;
    }

    public static <T> Map<T, Entry<Set<T>, Set<T>>> refineGeneric(
            Collection<? extends T> as,
            Collection<? extends T> bs,
            Map<Var, Var> newBase,
            BiFunction<NodeTransform, T, T> transformer
            )
    {
        Map<T, Entry<Set<T>, Set<T>>> result;

        if(newBase == null) {
            result = null;
        } else {
            NodeTransform signaturizer = NodeTransformSignaturize.create(newBase);

            Multimap<T, T> sigToAs = HashMultimap.create();
            as.forEach(q -> {
                T sig = transformer.apply(signaturizer, q);
                sigToAs.put(sig, q);
            });

            Map<Var, Var> identity = newBase.values().stream().collect(Collectors.toMap(x -> x, x -> x));
            NodeTransform s2 = NodeTransformSignaturize.create(identity);
            Multimap<T, T> sigToBs = HashMultimap.create();
            bs.forEach(q -> {
                T sig = transformer.apply(s2, q);
                sigToBs.put(sig, q);
            });

            result = MapUtils.groupByKey(sigToAs.asMap(), sigToBs.asMap());

            System.out.println("sigToAs: " + sigToAs);
            System.out.println("sigToBs: " + sigToBs);
            result.values().stream().forEach(e ->
            System.out.println("  Refined to " + e + " from " + as + " - " + bs + " via " + newBase));
        }
        return result;
    }

    public static Map<Quad, Entry<Set<Quad>, Set<Quad>>> refine(
            Collection<? extends Quad> as,
            Collection<? extends Quad> bs,
            Map<Var, Var> newBase)
    {
        Map<Quad, Entry<Set<Quad>, Set<Quad>>> result =
                refineGeneric(as, bs, newBase, (nodeTransform, quad) -> NodeTransformLib.transform(nodeTransform, quad));
        return result;

        /*
        Map<Quad, Entry<Set<Quad>, Set<Quad>>> result;

        if(newBase == null) {
            result = null;
        } else {
            NodeTransform signaturizer = NodeTransformSignaturize.create(newBase);

            Multimap<Quad, Quad> sigToAs = HashMultimap.create();
            as.forEach(q -> {
                Quad sig = NodeTransformLib.transform(signaturizer, q);
                sigToAs.put(sig, q);
            });

            Map<Var, Var> identity = newBase.values().stream().collect(Collectors.toMap(x -> x, x -> x));
            NodeTransform s2 = NodeTransformSignaturize.create(identity);
            Multimap<Quad, Quad> sigToBs = HashMultimap.create();
            bs.forEach(q -> {
                Quad sig = NodeTransformLib.transform(s2, q);
                sigToBs.put(sig, q);
            });

            result = ProblemVarMappingQuad.groupByKey(sigToAs.asMap(), sigToBs.asMap());

            System.out.println("sigToAs: " + sigToAs);
            System.out.println("sigToBs: " + sigToBs);
            result.values().stream().forEach(e ->
            System.out.println("  Refined to " + e + " from " + as + " - " + bs + " via " + newBase));
        }
        return result;
        */
    }

    public static Collection<Problem<Map<Var, Var>>> groupsToProblems(Map<Quad, Entry<Set<Quad>, Set<Quad>>> group, Map<Var, Var> newBase) {
        Collection<Problem<Map<Var, Var>>> result = group.values().stream()
                .map(e -> new ProblemVarMappingQuad(e.getKey(), e.getValue(), newBase))
                .collect(Collectors.toList());

        return result;
    }

    public static Collection<Problem<Map<Var, Var>>> refineQuads(
            Collection<? extends Quad> as,
            Collection<? extends Quad> bs,
            Map<Var, Var> baseSolution,
            Map<Var, Var> partialSolution) {
        Collection<Problem<Map<Var, Var>>> result = refineGeneric(
                as,
                bs,
                baseSolution,
                partialSolution,
                (nodeTransform, quad) -> NodeTransformLib.transform(nodeTransform, quad),
                (newAs, newBs, newBase) -> new ProblemVarMappingQuad(newAs, newBs, newBase)
                );

        return result;
    }

    public static Collection<Problem<Map<Var, Var>>> refineExprs(
            Collection<? extends Expr> as,
            Collection<? extends Expr> bs,
            Map<Var, Var> baseSolution,
            Map<Var, Var> partialSolution) {
        Collection<Problem<Map<Var, Var>>> result = refineGeneric(
                as,
                bs,
                baseSolution,
                partialSolution,
                (nodeTransform, expr) -> NodeTransformLib.transform(nodeTransform, expr),
                (newAs, newBs, newBase) -> new ProblemVarMappingExpr(newAs, newBs, newBase)
                );

        return result;
    }

}
