package org.aksw.jena_sparql_api.concepts;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;

/**
 * A ternary relation - it can e.g. be joined with a triple pattern
 * 
 * @author raven Mar 7, 2018
 *
 */
public class TernaryRelation
	implements Relation
{
	protected Var s;
	protected Var p;
	protected Var o;

	protected Element element;

	public TernaryRelation(Var s, Var p, Var o, Element element) {
		super();
		this.s = s;
		this.p = p;
		this.o = o;
		this.element = element;
	}
	
	public List<Var> getVars() {
		return Arrays.asList(s, p, o);
	}
	
	public Var getS() {
		return s;
	}

	public Var getP() {
		return p;
	}

	public Var getO() {
		return o;
	}

	public Element getElement() {
		return element;
	}
	
	
}
