package com.ztech.crm.assistance.service;

import static org.assertj.core.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SummaryResultValidatorTest {
    private final SummaryResultValidator validator=new SummaryResultValidator(JsonMapper.builder().build());
    private String response(String item){return "{\"clientNeeds\":"+item+",\"currentSituation\":[],\"agreementsAndObjections\":[],\"unconfirmedInformation\":[]}";}
    @Test void acceptsEmptySectionsAndEscapedTextWithoutCreatingSources() {
        assertThat(validator.validate(response("[]"),Set.of("O")).clientNeeds()).isEmpty();
        var result=validator.validate(response("[{\"text\":\"<script>ignored</script>\",\"sourceIds\":[\"O\"]}]"),Set.of("O"));
        assertThat(result.clientNeeds().getFirst().text()).isEqualTo("<script>ignored</script>");
    }
    @Test void rejectsShapeLimitsAndUnknownReferences() {
        for(String input:new String[]{"", "{", "null", "[]",response("{}"),response("[{\"text\":\" \",\"sourceIds\":[\"O\"]}]"),
                response("[{\"text\":\"x\",\"sourceIds\":[\"A99\"]}]"),response("[{\"text\":\"x\",\"sourceIds\":[]}]"),
                response("[{\"text\":\"x\",\"sourceIds\":[\"O\"],\"url\":\"https://example.invalid\"}]"),
                response("["+String.join(",",java.util.Collections.nCopies(6,"{\"text\":\"x\",\"sourceIds\":[\"O\"]}"))+"]"),
                response("[{\"text\":\"x\",\"sourceIds\":[\"O\",\"O\",\"O\",\"O\",\"O\",\"O\"]}]"),
                response("[]").replace("\"clientNeeds\":[]","\"clientNeeds\":[],\"clientNeeds\":[]"),response("[]")+" {}",
                response("[{\"text\":\""+"x".repeat(701)+"\",\"sourceIds\":[\"O\"]}]"),response("[]").replace("\"currentSituation\":[]","\"unexpected\":[]")})
            assertThatThrownBy(()->validator.validate(input,Set.of("O"))).isInstanceOf(SummaryFailure.class);
    }
}
