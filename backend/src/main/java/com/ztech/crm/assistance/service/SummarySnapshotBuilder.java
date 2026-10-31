package com.ztech.crm.assistance.service;

import com.ztech.crm.activities.service.ActivitySummaryContext;
import com.ztech.crm.opportunities.service.OpportunitySummaryContext;
import com.ztech.crm.assistance.dto.CommercialSummaryResponse.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SummarySnapshotBuilder {
    public static final int MAX_CHARACTERS=24_000;
    private final JsonMapper json;
    public SummarySnapshotBuilder(JsonMapper json) { this.json=json; }
    public SummarySnapshot build(OpportunitySummaryContext o, ActivitySummaryContext a) {
        var current=new TreeMap<String,String>();
        put(current,"title",o.title(),1000); put(current,"notes",o.notes(),4000);
        put(current,"status",o.status(),100); put(current,"stage",o.stage(),1000);
        put(current,"stageKind",o.stageKind(),100); put(current,"pipeline",o.pipeline(),1000);
        put(current,"venue",o.venue(),1000); put(current,"venueCapacity",o.capacity(),100);
        put(current,"venueCatalogRateARS",o.venueRate(),100); put(current,"services",o.services(),2000);
        put(current,"eventType",o.eventType(),1000); put(current,"eventStart",o.eventStart(),100);
        put(current,"eventEnd",o.eventEnd(),100); put(current,"attendees",o.attendees(),100);
        put(current,"estimatedValueARS",o.estimatedValue(),100); put(current,"finalValueARS",o.finalValue(),100);
        put(current,"optionalProbability",o.probability(),100); put(current,"estimatedCloseDate",o.estimatedCloseDate(),100);
        put(current,"closedAt",o.closedAt(),100); put(current,"lossReason",o.lossReason(),1000);
        boolean currentCut=length(o.notes())>4000 || length(o.title())>1000 || length(o.services())>2000
                || length(o.stage())>1000 || length(o.pipeline())>1000 || length(o.venue())>1000
                || length(o.eventType())>1000 || length(o.lossReason())>1000;
        var selected=new ArrayList<Source>();
        selected.add(new Source("O","OPPORTUNITY",o.id(),"Datos actuales de la oportunidad",null,null,
                Collections.unmodifiableMap(current),currentCut));
        var candidates=new ArrayList<Source>();
        for(int i=0;i<Math.max(a.facts().size(),o.transitions().size());i++) {
            if(i<a.facts().size()) {
                var f=a.facts().get(i); var fields=new TreeMap<String,String>();
                put(fields,"type",f.type(),1000);put(fields,"description",f.description(),2000);put(fields,"result",f.result(),2000);
                candidates.add(new Source("A"+(i+1),"ACTIVITY",f.id(),"Actividad",f.occurredAt(),f.createdAt(),
                        Collections.unmodifiableMap(fields),length(f.description())>2000||length(f.result())>2000||length(f.type())>1000));
            }
            if(i<o.transitions().size()) {
                var h=o.transitions().get(i); var fields=new TreeMap<String,String>();
                put(fields,"eventType",h.eventType(),100);put(fields,"fromStageCurrentName",h.fromStage(),1000);
                put(fields,"toStageCurrentName",h.toStage(),1000);put(fields,"destinationStatus",h.destinationStatus(),100);
                put(fields,"snapshotComplete",h.snapshotComplete(),100);put(fields,"finalValueARS",h.finalValue(),100);
                put(fields,"closedAt",h.closedAt(),100);put(fields,"historicalLossReason",h.lossReason(),1000);put(fields,"note",h.note(),1000);
                candidates.add(new Source("H"+(i+1),"TRANSITION",h.id(),"Cambio comercial",h.changedAt(),h.changedAt(),
                        Collections.unmodifiableMap(fields),length(h.note())>1000||length(h.fromStage())>1000
                                ||length(h.toStage())>1000||length(h.lossReason())>1000));
            }
        }
        // Dejar margen para metadatos; medir siempre el JSON completo, por puntos de código.
        for(var source:candidates) {
            var trial=new ArrayList<>(selected);trial.add(source);
            if(length(serialize(trial,a.total(),o.transitionCount()))<=MAX_CHARACTERS) selected.add(source);
        }
        selected.sort(Comparator.comparing((Source s)->s.occurredAt()==null?Instant.MIN:s.occurredAt())
                .thenComparing(Source::type).thenComparing(Source::originalId));
        String context=serialize(selected,a.total(),o.transitionCount());
        if(length(context)>MAX_CHARACTERS) throw SummaryFailure.invalid();
        int includedA=(int)selected.stream().filter(s->s.type().equals("ACTIVITY")).count();
        int includedH=(int)selected.stream().filter(s->s.type().equals("TRANSITION")).count();
        boolean truncated=selected.stream().anyMatch(Source::textTruncated);
        var coverage=new Coverage(a.total(),includedA,o.transitionCount(),includedH,range(selected,"ACTIVITY"),
                range(selected,"TRANSITION"),includedA<a.total()||includedH<o.transitionCount()||truncated,
                truncated,length(context),"50 recientes por fecha DESC e ID DESC; O primero; históricos intercalados hasta 24.000 caracteres; análisis cronológico");
        // Incluye los candidatos completos: una modificación en un campo recortado también invalida.
        String canonical=json.writeValueAsString(List.of(o,a));
        return new SummarySnapshot(o.id(),o.version(),Instant.now(),hash(canonical),context,coverage,List.copyOf(selected));
    }
    private String serialize(List<Source> sources,long activities,long transitions) {
        return json.writeValueAsString(Map.of("totalActivities",activities,"totalTransitions",transitions,
                "selection","Recent 50 per kind; budget 24000 Unicode code points; fields may be truncated",
                "sources",sources));
    }
    private Range range(List<Source> sources,String type) {
        var times=sources.stream().filter(s->s.type().equals(type)).map(Source::occurredAt).sorted().toList();
        return new Range(times.isEmpty()?null:times.getFirst(),times.isEmpty()?null:times.getLast());
    }
    private static void put(Map<String,String> fields,String key,Object value,int max) {
        if(value!=null) { String s=value.toString(); fields.put(key,s.substring(0,s.offsetByCodePoints(0,Math.min(length(s),max)))); }
    }
    static int length(Object value) { if(value==null)return 0; String s=value.toString();return s.codePointCount(0,s.length()); }
    private static String hash(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException ex) {throw new IllegalStateException(ex);}
    }
}
