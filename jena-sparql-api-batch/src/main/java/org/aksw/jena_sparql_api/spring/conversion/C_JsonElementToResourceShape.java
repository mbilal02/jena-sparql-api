package org.aksw.jena_sparql_api.spring.conversion;

import org.aksw.jena_sparql_api.batch.ResourceShapeBuilder;
import org.aksw.jena_sparql_api.shape.ResourceShape;
import org.aksw.jena_sparql_api.shape.ResourceShapeParserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

import com.google.gson.JsonElement;
import com.hp.hpl.jena.sparql.core.Prologue;


@AutoRegistered
public class C_JsonElementToResourceShape
    implements Converter<JsonElement, ResourceShape>
{
//    @Autowired
//    protected ResourceShapeParserJson parser;
    @Autowired
    protected Prologue prologue;

    public ResourceShape convert(JsonElement json) {
        ResourceShapeBuilder builder = new ResourceShapeBuilder(prologue);
        ResourceShape result = ResourceShapeParserJson.parse(json, builder);
        return result;
    }
}