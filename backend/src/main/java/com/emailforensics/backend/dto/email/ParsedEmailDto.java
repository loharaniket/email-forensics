package com.emailforensics.backend.dto.email;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ParsedEmailDto {

    private String messageId;
    private String subject;
    private String from;
    private List<String> to = new ArrayList<>();
    private List<String> cc = new ArrayList<>();
    private List<String> bcc = new ArrayList<>();
    private List<String> replyTo = new ArrayList<>();
    private String returnPath;
    private Instant sentDate;
    private String rawDate;
    private String plainTextBody;
    private String htmlBody;
    private String extractedText;
    private List<AttachmentMetaDto> attachments = new ArrayList<>();
    private List<InlineImageMetaDto> inlineImages = new ArrayList<>();
    private List<String> parseWarnings = new ArrayList<>();

    public ParsedEmailDto() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    public List<String> getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(List<String> replyTo) {
        this.replyTo = replyTo;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public Instant getSentDate() {
        return sentDate;
    }

    public void setSentDate(Instant sentDate) {
        this.sentDate = sentDate;
    }

    public String getRawDate() {
        return rawDate;
    }

    public void setRawDate(String rawDate) {
        this.rawDate = rawDate;
    }

    public String getPlainTextBody() {
        return plainTextBody;
    }

    public void setPlainTextBody(String plainTextBody) {
        this.plainTextBody = plainTextBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public List<AttachmentMetaDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentMetaDto> attachments) {
        this.attachments = attachments;
    }

    public List<InlineImageMetaDto> getInlineImages() {
        return inlineImages;
    }

    public void setInlineImages(List<InlineImageMetaDto> inlineImages) {
        this.inlineImages = inlineImages;
    }

    public List<String> getParseWarnings() {
        return parseWarnings;
    }

    public void setParseWarnings(List<String> parseWarnings) {
        this.parseWarnings = parseWarnings;
    }
}
