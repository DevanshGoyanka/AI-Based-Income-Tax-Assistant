package com.itr.infrastructure.ai;

/**
 * AIPromptLibrary — all LLM prompts as constants.
 * <p>
 * EVERY prompt used by the system must be defined here.
 * Never inline prompt strings in adapter code.
 */
public final class AIPromptLibrary {

    private AIPromptLibrary() {}

    /** Prompt for notice analysis. */
    public static final String NOTICE_ANALYSIS_PROMPT = """
        You are a chartered accountant's assistant analyzing an income tax notice.
        Extract the following in JSON format:
        - notice_type: The type of notice (Scrutiny 143(2), Intimation 143(1), Demand, Penalty, Reassessment 148)
        - sections_cited: Array of IT Act sections mentioned
        - queries: Array of specific queries raised by the officer
        - deadline: Reply due date
        - summary: 2-3 sentence summary of what the notice asks
        - risk_level: HIGH/MEDIUM/LOW based on the type and quantum

        Notice text:
        {notice_text}
        """;

    /** Prompt for drafting a reply to a notice. */
    public static final String REPLY_DRAFT_PROMPT = """
        You are a chartered accountant drafting a reply to an income tax notice.
        Use the following notice analysis and client data to draft a professional reply.

        Notice Analysis:
        {notice_analysis}

        Client ITR Data:
        {itr_data}

        Guidelines:
        - Be respectful and professional
        - Address each query point-by-point
        - Reference supporting documents where applicable
        - Cite relevant sections and case laws
        - End with a declaration that the statements are true to the best of the assessee's knowledge
        - Do NOT include the CA's signature — the CA will review and sign
        """;

    /** Prompt for tax saving suggestions. */
    public static final String TAX_SAVING_PROMPT = """
        You are a tax advisor. Based on the client's income profile, suggest tax-saving 
        investments for the next financial year.
        
        Client Profile:
        {client_profile}
        
        Provide suggestions under: Section 80C, 80D, NPS (80CCD), and any other applicable sections.
        """;

    /** Prompt for compliance risk scoring. */
    public static final String RISK_SCORING_PROMPT = """
        Analyze the following client profile and assess compliance risk.
        
        Client Data:
        {client_data}
        
        Return a JSON with:
        - risk_score: 0-100
        - top_risk_factors: Array of strings explaining the top risks
        - recommended_actions: Array of strings
        """;

    /** General disclaimer appended to all AI-generated outputs. */
    public static final String AI_DISCLAIMER =
        "⚠ AI-generated content — verify before submission. This is not legal advice. "
        + "The CA must review all AI-generated content before sending to the client or ITD.";
}
