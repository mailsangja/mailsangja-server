package com.mailsangja.core.service.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HybridSearchLexicalQueryBuilderTest {

    private final HybridSearchLexicalQueryBuilder builder = new HybridSearchLexicalQueryBuilder();

    @Test
    void 문장검색어를OrTsQuery로변환한다() {
        String result = builder.build("프로젝트 일정 조율 관련 메일 찾아줘");

        assertEquals("프로젝트 | 일정 | 조율 | 관련 | 메일 | 찾아줘", result);
    }

    @Test
    void 검색문법문자는제거하고안전한토큰만사용한다() {
        String result = builder.build("프로젝트 & 일정 | DROP! (회의)");

        assertEquals("프로젝트 | 일정 | drop | 회의", result);
    }
}
