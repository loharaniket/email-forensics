package com.emailforensics.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParsedHeaderResult {

    private String sender;
    private String fromDomain;
    private String replyTo;
    private String replyToDomain;
    private String returnPath;
    private String returnPathDomain;
    private String subject;
    private Instant sentAt;
    private String messageId;
    private boolean replyToMismatch;
    private boolean returnPathMismatch;
    private List<Map<String, String>> receivedHops = new ArrayList<>();
    private String bodyText;
    private String bodyHtml;

    public ParsedHeaderResult() {
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getFromDomain() {
        return fromDomain;
    }

    public void setFromDomain(String fromDomain) {
        this.fromDomain = fromDomain;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getReplyToDomain() {
        return replyToDomain;
    }

    public void setReplyToDomain(String replyToDomain) {
        this.replyToDomain = replyToDomain;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public String getReturnPathDomain() {
        return returnPathDomain;
    }

    public void setReturnPathDomain(String returnPathDomain) {
        this.returnPathDomain = returnPathDomain;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isReplyToMismatch() {
        return replyToMismatch;
    }

    public void setReplyToMismatch(boolean replyToMismatch) {
        this.replyToMismatch = replyToMismatch;
    }

    public boolean isReturnPathMismatch() {
        return returnPathMismatch;
    }

    public void setReturnPathMismatch(boolean returnPathMismatch) {
        this.returnPathMismatch = returnPathMismatch;
    }

    public List<Map<String, String>> getReceivedHops() {
        return receivedHops;
    }

    public void setReceivedHops(List<Map<String, String>> receivedHops) {
        this.receivedHops = receivedHops;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public void appendBodyText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (this.bodyText == null) {
            this.bodyText = text;
        } else {
            this.bodyText = this.bodyText + "\n" + text;
        }
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public void appendBodyHtml(String html) {
        if (html == null || html.isEmpty()) {
            return;
        }
        if (this.bodyHtml == null) {
            this.bodyHtml = html;
        } else {
            this.bodyHtml = this.bodyHtml + "\n" + html;
        }
    }
}
