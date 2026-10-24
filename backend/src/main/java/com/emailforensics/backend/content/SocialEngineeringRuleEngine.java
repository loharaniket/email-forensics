package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.SocialEngineeringMatchDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SocialEngineeringRuleEngine {

    public static class SocialEngineeringFindings {
        private final boolean urgency;
        private final boolean credentialRequest;
        private final boolean paymentInstruction;
        private final boolean impersonation;
        private final boolean secrecy;
        private final List<SocialEngineeringMatchDto> matches;

        public SocialEngineeringFindings(boolean urgency, boolean credentialRequest, boolean paymentInstruction,
                                         boolean impersonation, boolean secrecy, List<SocialEngineeringMatchDto> matches) {
            this.urgency = urgency;
            this.credentialRequest = credentialRequest;
            this.paymentInstruction = paymentInstruction;
            this.impersonation = impersonation;
            this.secrecy = secrecy;
            this.matches = matches != null ? matches : Collections.emptyList();
        }

        public boolean isUrgency() {
            return urgency;
        }

        public boolean isCredentialRequest() {
            return credentialRequest;
        }

        public boolean isPaymentInstruction() {
            return paymentInstruction;
        }

        public boolean isImpersonation() {
            return impersonation;
        }

        public boolean isSecrecy() {
            return secrecy;
        }

        public List<SocialEngineeringMatchDto> getMatches() {
            return matches;
        }
    }

    private static class RuleDef {
        private final String category;
        private final Pattern pattern;
        private final String explanation;
        private final String severity;

        RuleDef(String category, String regex, String explanation, String severity) {
            this.category = category;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.explanation = explanation;
            this.severity = severity;
        }
    }

    private final List<RuleDef> rules = List.of(
            new RuleDef(
                    "URGENCY",
                    "\\b(immediate(ly)? action (is )?required|action required immediately|urgent( notice| message| attention)?|account (will be|has been) (suspended|terminated|disabled|locked)|within (24|48) hours|final (warning|notice)|immediate response required|expires today|limited time|respond immediately|access will be revoked|temporarily locked)\\b",
                    "High pressure or artificial time constraint detected attempting to induce panic or hasty action.",
                    "HIGH"
            ),
            new RuleDef(
                    "CREDENTIAL_REQUEST",
                    "\\b(verify (your )?(password|account|identity|credentials|access)|reset (your )?(password|credentials|passcode)|confirm (your )?(password|account|identity|login)|update (your )?(login|credentials|account details|security info)|enter your (password|passcode|pin|security code)|mfa (reset|verification|device|re-authentication)|re-enter (your )?password|authenticator app verification|security code verification|log in to (confirm|verify|keep)|validate (your )?account)\\b",
                    "Direct or indirect solicitations for authentication credentials, password reset, or MFA tokens.",
                    "HIGH"
            ),
            new RuleDef(
                    "PAYMENT_INSTRUCTION",
                    "\\b(wire transfer|overdue invoice|unpaid balance|remit payment|payment (is )?due|bank account details|routing number|cryptocurrency|bitcoin|btc address|gift card(s)?|payroll direct deposit|update (banking|direct deposit|payment) information|urgent payment|outstanding invoice|transfer the funds|send payment)\\b",
                    "Financial transaction demand, invoice diversion, or alternative payment channel solicitation.",
                    "HIGH"
            ),
            new RuleDef(
                    "IMPERSONATION",
                    "\\b(i am the (ceo|cfo|coo|president|director)|from the desk of the (ceo|director|president)|it (support|helpdesk|department|service desk|admin)|human resources (department)?|hr department|internal revenue service|tax authority|compliance officer|system administrator|security operations center|executive leadership)\\b",
                    "Authority impersonation targeting corporate roles, executive leadership, or IT support desks.",
                    "MEDIUM"
            ),
            new RuleDef(
                    "SECRECY",
                    "\\b(keep this (confidential|private|between us|secret)|strictly confidential|do not (discuss|share|tell|disclose) (this )?(with|to) anyone|do not (call|contact) (me|anyone)|bypass normal (channels|procedures|protocols)|handle this (privately|quietly|discreetly)|discrete inquiry|do not forward this email)\\b",
                    "Isolation or secrecy tactic instructing recipient to bypass normal verification and organizational controls.",
                    "MEDIUM"
            )
    );

    public SocialEngineeringFindings analyze(Map<String, String> channelTexts) {
        if (channelTexts == null || channelTexts.isEmpty()) {
            return new SocialEngineeringFindings(false, false, false, false, false, Collections.emptyList());
        }

        boolean urgency = false;
        boolean credentialRequest = false;
        boolean paymentInstruction = false;
        boolean impersonation = false;
        boolean secrecy = false;
        List<SocialEngineeringMatchDto> matches = new ArrayList<>();

        for (Map.Entry<String, String> entry : channelTexts.entrySet()) {
            String channel = entry.getKey();
            String text = entry.getValue();
            if (text == null || text.isBlank()) {
                continue;
            }

            for (RuleDef rule : rules) {
                Matcher matcher = rule.pattern.matcher(text);
                while (matcher.find()) {
                    String matchedText = matcher.group();
                    matches.add(new SocialEngineeringMatchDto(
                            rule.category,
                            matchedText,
                            channel,
                            rule.explanation,
                            rule.severity
                    ));

                    switch (rule.category) {
                        case "URGENCY" -> urgency = true;
                        case "CREDENTIAL_REQUEST" -> credentialRequest = true;
                        case "PAYMENT_INSTRUCTION" -> paymentInstruction = true;
                        case "IMPERSONATION" -> impersonation = true;
                        case "SECRECY" -> secrecy = true;
                    }
                }
            }
        }

        return new SocialEngineeringFindings(urgency, credentialRequest, paymentInstruction, impersonation, secrecy, matches);
    }
}
