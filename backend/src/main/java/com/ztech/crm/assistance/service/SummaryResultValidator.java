package com.ztech.crm.assistance.service;

import com.ztech.crm.assistance.dto.CommercialSummaryResponse.*;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SummaryResultValidator {
    static final List<String> SECTION_NAMES=List.of("clientNeeds","currentSituation","agreementsAndObjections","unconfirmedInformation");
    private final JsonMapper json;
    public SummaryResultValidator(JsonMapper json){this.json=json.rebuild()
            .enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();}
    public Sections validate(String text,Set<String> allowed) {
        try {
            var root=json.readTree(text);
            if(!root.isObject()||root.size()!=4||SECTION_NAMES.stream().anyMatch(k->!root.has(k)))throw SummaryFailure.invalid();
            var sections=SECTION_NAMES.stream().map(k->items(root.get(k),allowed)).toList();
            return new Sections(sections.get(0),sections.get(1),sections.get(2),sections.get(3));
        } catch(SummaryFailure ex){throw ex;} catch(RuntimeException ex){throw SummaryFailure.invalid();}
    }
    private List<Item> items(JsonNode node,Set<String> allowed) {
        if(!node.isArray()||node.size()>5)throw SummaryFailure.invalid();
        var result=new ArrayList<Item>();
        for(var item:node) {
            if(!item.isObject()||item.size()!=2||!item.has("text")||!item.has("sourceIds"))throw SummaryFailure.invalid();
            var t=item.get("text");var ids=item.get("sourceIds");
            if(!t.isString()||t.asString().isBlank()||SummarySnapshotBuilder.length(t.asString())>700
                    ||!ids.isArray()||ids.isEmpty()||ids.size()>5)throw SummaryFailure.invalid();
            var references=new ArrayList<String>();
            for(var id:ids) {if(!id.isString()||!allowed.contains(id.asString()))throw SummaryFailure.invalid(); references.add(id.asString());}
            result.add(new Item(t.asString(),references.stream().distinct().toList()));
        }
        return List.copyOf(result);
    }
}
