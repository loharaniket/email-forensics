package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.PromptInjectionMatchDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptInjectionDetector {

    public static class PromptInjectionFindings {
        private final boolean promptInjectionDetected;
        private final List<PromptInjectionMatchDto> matches;
        private final String warningMessage;

        public PromptInjectionFindings(boolean promptInjectionDetected, List<PromptInjectionMatchDto> matches, String warningMessage) {
            this.promptInjectionDetected = promptInjectionDetected;
            this.matches = matches != null ? matches : Collections.emptyList();
            this.warningMessage = warningMessage;
        }

        public boolean isPromptInjectionDetected() {
            return promptInjectionDetected;
        }

        public List<PromptInjectionMatchDto> getMatches() {
            return matches;
        }

        public String getWarningMessage() {
            return warningMessage;
        }
    }

    private static class InjectionPatternDef {
        private final String category;
        private final Pattern pattern;
        private final String explanation;
        private final String severity;

        InjectionPatternDef(String category, String regex, String explanation, String severity) {
            this.category = category;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.explanation = explanation;
            this.severity = severity;
        }
    }

    private final List<InjectionPatternDef> patterns = List.of(
            new InjectionPatternDef(
                    "INSTRUCTION_OVERRIDE",
                    "\\b(ignore (all )?(previous|prior|above) instructions|disregard (all )?(previous|prior|above) instructions|forget (all )?(previous|prior|above) instructions|system prompt override|new system instruction(s)?|override system prompt|from now on (you are|act as))\\b",
                    "Direct attempt to command downstream LLM to erase or disregard foundational instructions.",
                    "CRITICAL"
            ),
            new InjectionPatternDef(
                    "DATA_EXFILTRATION",
                    "\\b(print (the )?(system prompt|developer instructions|initial prompt)|output (the )?(system prompt|developer instructions)|reveal (your )?(instructions|system prompt)|show (me )?(the )?developer message|repeat (all )?text above|repeat your (entire )?instructions|exfiltrate (the )?following)\\b",
                    "Pattern soliciting disclosure of confidential AI system prompts or environment context.",
                    "HIGH"
            ),
            new InjectionPatternDef(
                    "ROLE_HIJACKING",
                    "\\b(you are now an unrestricted (ai|assistant|model)|jailbreak mode|dan mode|do anything now mode|operate without ethical (guidelines|boundaries)|disable (your )?safety filters|act as an unfiltered (ai|llm))\\b",
                    "Persona hijacking attempting to bypass AI safety guardrails and policy boundaries.",
                    "CRITICAL"
            ),
            new InjectionPatternDef(
                    "DELIMITER_INJECTION",
                    "(```system|<\\|im_start\\|>|<\\|im_end\\|>|<system>|\\[system\\]|\\[INST\\]|\\[\\/INST\\]|<\\|endoftext\\|>|human:.*assistant:)",
                    "Adversarial formatting tokens or control characters commonly used to spoof LLM turn delimiters.",
                    "HIGH"
            ),
            new InjectionPatternDef(
                    "SAFETY_BYPASS",
                    "\\b(do not analyze this (email|message)|mark this (email|message) as (clean|safe|benign)|bypass (the )?security scan|skip safety inspection|pretend this email is harmless)\\b",
                    "Adversarial instruction telling forensic analyzers to bypass scanning or classify malicious content as benign.",
                    "HIGH"
            )
    );

    public PromptInjectionFindings detect(Map<String, String> channelTexts) {
        if (channelTexts == null || channelTexts.isEmpty()) {
            return new PromptInjectionFindings(false, Collections.emptyList(), null);
        }

        List<PromptInjectionMatchDto> matches = new ArrayList<>();

        for (Map.Entry<String, String> entry : channelTexts.entrySet()) {
            String channel = entry.getKey();
            String text = entry.getValue();
            if (text == null || text.isBlank()) {
                continue;
            }

            for (InjectionPatternDef def : patterns) {
                Matcher matcher = def.pattern.matcher(text);
                while (matcher.find()) {
                    String matchedText = matcher.group();
                    matches.add(new PromptInjectionMatchDto(
                            def.category,
                            matchedText,
                            channel,
                            def.explanation,
                            def.severity
                    ));
                }
            }
        }

        boolean detected = !matches.isEmpty();
        String warning = detected
                ? "Warning: Potential prompt injection or adversarial steering patterns detected in untrusted email content. Detection raises an investigative alert; it is not a guarantee of total prevention. Downstream LLMs must isolate raw message text."
                : null;

        return new PromptInjectionFindings(detected, matches, warning);
    }
}
