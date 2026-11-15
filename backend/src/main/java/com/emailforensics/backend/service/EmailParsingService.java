package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.ParsedHeaderResult;
import jakarta.mail.Address;
import jakarta.mail.Header;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParsingService {

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\[|\\()([0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[a-fA-F0-9:]+)(?:\\]|\\))");
    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bfrom\\s+([^\\s;]+)");
    private static final Pattern BY_PATTERN = Pattern.compile("(?i)\\bby\\s+([^\\s;]+)");
    private static final Pattern WITH_PATTERN = Pattern.compile("(?i)\\bwith\\s+([^\\s;]+)");

    public ParsedHeaderResult parseHeaders(InputStream inputStream) {
        ParsedHeaderResult result = new ParsedHeaderResult();
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, inputStream);

            extractBasicHeaders(message, result);
            extractSenderAndRecipients(message, result);
            extractReceivedHops(message, result);
            evaluateSenderConsistency(result);
            extractBodyContent(message, result);
        } catch (Exception e) {
            result.setSubject("(Failed to parse email headers)");
        }
        return result;
    }

    private void extractBasicHeaders(MimeMessage message, ParsedHeaderResult result) {
        try {
            String subject = message.getSubject();
            result.setSubject(subject != null ? MimeUtility.decodeText(subject) : "(No Subject)");
        } catch (Exception e) {
            result.setSubject("(Undecodable Subject)");
        }

        try {
            result.setMessageId(message.getMessageID());
        } catch (Exception ignored) {
        }

        try {
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                result.setSentAt(sentDate.toInstant());
            }
        } catch (Exception ignored) {
        }
    }

    private void extractSenderAndRecipients(MimeMessage message, ParsedHeaderResult result) {
        try {
            Address[] from = message.getFrom();
            if (from != null && from.length > 0) {
                String formatted = formatAddress(from[0]);
                result.setSender(formatted);
                result.setFromDomain(extractDomain(formatted));
            }
        } catch (Exception ignored) {
        }

        try {
            Address[] replyTo = message.getReplyTo();
            if (replyTo != null && replyTo.length > 0) {
                String formatted = formatAddress(replyTo[0]);
                result.setReplyTo(formatted);
                result.setReplyToDomain(extractDomain(formatted));
            }
        } catch (Exception ignored) {
        }

        try {
            String[] returnPaths = message.getHeader("Return-Path");
            if (returnPaths != null && returnPaths.length > 0) {
                String raw = returnPaths[0].replaceAll("[<>]", "").trim();
                result.setReturnPath(raw);
                result.setReturnPathDomain(extractDomain(raw));
            }
        } catch (Exception ignored) {
        }
    }

    private void extractReceivedHops(MimeMessage message, ParsedHeaderResult result) {
        List<Map<String, String>> hops = new ArrayList<>();
        try {
            String[] receivedHeaders = message.getHeader("Received");
            if (receivedHeaders != null) {
                int hopNum = 1;
                for (String raw : receivedHeaders) {
                    hops.add(parseSingleHop(raw, hopNum++));
                }
            }
        } catch (Exception ignored) {
        }
        result.setReceivedHops(hops);
    }

    private Map<String, String> parseSingleHop(String rawHop, int hopNumber) {
        Map<String, String> hop = new LinkedHashMap<>();
        hop.put("hop", String.valueOf(hopNumber));

        String cleaned = rawHop.replaceAll("[\\r\\n\\t]+", " ").trim();
        hop.put("raw", cleaned);

        Matcher fromMatcher = FROM_PATTERN.matcher(cleaned);
        if (fromMatcher.find()) {
            hop.put("from", fromMatcher.group(1));
        }

        Matcher byMatcher = BY_PATTERN.matcher(cleaned);
        if (byMatcher.find()) {
            hop.put("by", byMatcher.group(1));
        }

        Matcher withMatcher = WITH_PATTERN.matcher(cleaned);
        if (withMatcher.find()) {
            hop.put("with", withMatcher.group(1));
        }

        Matcher ipMatcher = IP_PATTERN.matcher(cleaned);
        if (ipMatcher.find()) {
            hop.put("ip", ipMatcher.group(1));
        }

        int semicolonIdx = cleaned.lastIndexOf(';');
        if (semicolonIdx >= 0 && semicolonIdx < cleaned.length() - 1) {
            hop.put("timestamp", cleaned.substring(semicolonIdx + 1).trim());
        }

        return hop;
    }

    private void evaluateSenderConsistency(ParsedHeaderResult result) {
        String fromDom = result.getFromDomain();
        String replyToDom = result.getReplyToDomain();
        String returnPathDom = result.getReturnPathDomain();

        if (fromDom != null && replyToDom != null && !fromDom.equalsIgnoreCase(replyToDom)) {
            result.setReplyToMismatch(true);
        }

        if (fromDom != null && returnPathDom != null && !fromDom.equalsIgnoreCase(returnPathDom)) {
            result.setReturnPathMismatch(true);
        }
    }

    private String formatAddress(Address address) {
        if (address instanceof InternetAddress ia) {
            String personal = ia.getPersonal();
            String email = ia.getAddress();
            if (personal != null && !personal.isBlank()) {
                return personal + " <" + email + ">";
            }
            return email;
        }
        return address.toString();
    }

    private String extractDomain(String addressOrText) {
        if (addressOrText == null || addressOrText.isBlank()) {
            return null;
        }
        int atIndex = addressOrText.lastIndexOf('@');
        if (atIndex < 0 || atIndex >= addressOrText.length() - 1) {
            return null;
        }
        String domain = addressOrText.substring(atIndex + 1).trim();
        int end = domain.indexOf('>');
        if (end > 0) {
            domain = domain.substring(0, end).trim();
        }
        return domain.toLowerCase();
    }

    private void extractBodyContent(Part part, ParsedHeaderResult result) {
        try {
            if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                handleTextPart(part, result);
            } else if (part.isMimeType("multipart/*")) {
                handleMultipart(part, result);
            } else if (part.isMimeType("message/rfc822")) {
                Object content = part.getContent();
                if (content instanceof Part nestedPart) {
                    extractBodyContent(nestedPart, result);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void handleTextPart(Part part, ParsedHeaderResult result) throws Exception {
        String disposition = part.getDisposition();
        if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
            return;
        }
        Object content = part.getContent();
        if (!(content instanceof String text)) {
            return;
        }
        if (part.isMimeType("text/plain")) {
            result.appendBodyText(text);
        } else if (part.isMimeType("text/html")) {
            result.appendBodyHtml(text);
        }
    }

    private void handleMultipart(Part part, ParsedHeaderResult result) throws Exception {
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                extractBodyContent(multipart.getBodyPart(i), result);
            }
        }
    }
}
