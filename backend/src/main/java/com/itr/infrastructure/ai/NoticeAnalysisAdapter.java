package com.itr.infrastructure.ai;

import java.util.Map;

/**
 * NoticeAnalysisAdapter — sends OCR text to LLM and returns structured analysis.
 */
public class NoticeAnalysisAdapter {

    /**
     * Analyze a notice text and extract structured information.
     *
     * @param ocrText Text extracted from notice PDF
     * @return Map with: summary, sectionsCited[], queries[], riskLevel, deadline
     */
    public Map<String, Object> analyze(String ocrText) {
        // Spring AI + OpenAI integration — placeholder
        // Calls LLM with prompt from AIPromptLibrary
        return Map.of(
            "summary", "",
            "sectionsCited", new String[]{},
            "queries", new String[]{},
            "riskLevel", "MEDIUM",
            "deadline", ""
        );
    }
}
