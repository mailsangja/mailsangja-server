package com.mailsangja.worker.handler.label;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 경량 Aho-Corasick automaton 구현체.
 * 여러 패턴 키워드를 텍스트에서 한 번에 검색한다.
 *
 * build() 호출 전에 addPattern()으로 패턴을 등록하고,
 * build() 이후 search()로 매칭 결과를 조회한다.
 *
 * 이 구현은 모든 비교를 소문자/trim 정규화된 입력에 대해 수행하는 것을 가정한다.
 */
class AhoCorasickAutomaton {

    private static final int ROOT = 0;

    private final List<Map<Character, Integer>> goto_ = new ArrayList<>();
    private final List<Integer> fail = new ArrayList<>();
    private final List<Set<String>> output = new ArrayList<>();
    private final Set<String> patterns = new LinkedHashSet<>();

    private boolean built = false;

    AhoCorasickAutomaton() {
        addState();
    }

    boolean hasPatterns() {
        return !patterns.isEmpty();
    }

    /**
     * 패턴을 추가한다. build() 호출 전에만 사용 가능.
     */
    void addPattern(String pattern) {
        if (built) {
            throw new IllegalStateException("Cannot add patterns after build()");
        }
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        if (!patterns.add(pattern)) {
            return;
        }
        insertPattern(pattern);
    }

    /**
     * 모든 패턴을 등록한 뒤 자동화 automaton을 빌드한다.
     */
    void build() {
        buildFailureLinks();
        built = true;
    }

    /**
     * 텍스트에서 등록된 패턴을 검색한다.
     * @return 발견된 패턴 문자열 집합
     */
    Set<String> search(String text) {
        if (!built || text == null || text.isEmpty()) {
            return Set.of();
        }

        Set<String> found = new HashSet<>();
        int current = ROOT;

        for (char ch : text.toCharArray()) {
            while (current != ROOT && !goto_.get(current).containsKey(ch)) {
                current = fail.get(current);
            }
            current = goto_.get(current).getOrDefault(ch, ROOT);
            Set<String> hits = output.get(current);
            if (!hits.isEmpty()) {
                found.addAll(hits);
            }
        }

        return found;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private void insertPattern(String pattern) {
        int current = ROOT;
        for (char ch : pattern.toCharArray()) {
            Map<Character, Integer> children = goto_.get(current);
            if (!children.containsKey(ch)) {
                int newState = addState();
                children.put(ch, newState);
            }
            current = children.get(ch);
        }
        output.get(current).add(pattern);
    }

    private void buildFailureLinks() {
        Queue<Integer> queue = new ArrayDeque<>();

        // 깊이 1 노드의 failure link는 루트
        for (int child : goto_.get(ROOT).values()) {
            fail.set(child, ROOT);
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            int state = queue.poll();
            for (Map.Entry<Character, Integer> entry : goto_.get(state).entrySet()) {
                char ch = entry.getKey();
                int next = entry.getValue();
                queue.add(next);

                int failState = fail.get(state);
                while (failState != ROOT && !goto_.get(failState).containsKey(ch)) {
                    failState = fail.get(failState);
                }
                int link = goto_.get(failState).getOrDefault(ch, ROOT);
                if (link == next) {
                    link = ROOT;
                }
                fail.set(next, link);

                // output 전파: failure link의 output을 현재 상태 output에 합산
                output.get(next).addAll(output.get(link));
            }
        }
    }

    private int addState() {
        int id = goto_.size();
        goto_.add(new HashMap<>());
        fail.add(ROOT);
        output.add(new HashSet<>());
        return id;
    }
}
