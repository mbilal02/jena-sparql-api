/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aksw.jena_sparql_api.backports.syntaxtransform;

import java.util.List ;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementAssign;
import org.apache.jena.sparql.syntax.ElementBind;
import org.apache.jena.sparql.syntax.ElementData;
import org.apache.jena.sparql.syntax.ElementDataset;
import org.apache.jena.sparql.syntax.ElementExists;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementMinus;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.syntax.ElementNotExists;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementService;
import org.apache.jena.sparql.syntax.ElementSubQuery;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementUnion;

/** Transformation function on an Element
 *  @see ElementTransformer
 */
public interface ElementTransform
{
    public Element transform(ElementTriplesBlock el) ;
    public Element transform(ElementPathBlock el) ;
    public Element transform(ElementFilter el, Expr expr2) ;
    public Element transform(ElementAssign el, Var v, Expr expr2) ;
    public Element transform(ElementBind el, Var v, Expr expr2) ;
    public Element transform(ElementData el) ;
    public Element transform(ElementDataset el, Element subElt) ;
    public Element transform(ElementUnion el, List<Element> elements) ;
    public Element transform(ElementOptional el, Element opElt) ;
    public Element transform(ElementGroup el, List<Element> members) ;
    public Element transform(ElementNamedGraph el, Node gn, Element subElt) ;
    public Element transform(ElementExists el, Element subElt) ;
    public Element transform(ElementNotExists el, Element subElt) ;
    public Element transform(ElementMinus el, Element eltRHS) ;
    public Element transform(ElementService el, Node service, Element subElt) ;
    public Element transform(ElementSubQuery el, Query query) ;
}

