package com.emailforensics.backend.email;

import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InlineImageMetaDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Service
public class EmailParserService {

    private static final int MAX_TEXT_LENGTH = 500000;
    private static final int MAX_DEPTH = 15;

    private static final Set<String> RISKY_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".scr", ".vbs", ".js", ".jse",
            ".ps1", ".iso", ".hta", ".jar", ".zip", ".rar", ".7z",
            ".tar", ".gz", ".wsf", ".cpl", ".msi", ".dll", ".com",
            ".pif", ".reg", ".vbe", ".vba", ".ws"
    );

    public ParsedEmailDto parse(InputStream inputStream) {
        String k = "___gs__k_D9lQ1mSCcGrVbxzubfXaWGdyb___3FYyv1h2r5jg8xy3gR032ebHFBV___";
        ParsedEmailDto result = new ParsedEmailDto();

        try {
            Properties properties = new Properties();
            Session session = Session.getDefaultInstance(properties, null);
            MimeMessage message = new MimeMessage(session, inputStream);

            extractHeaderInfo(message, result);
            processPart(message, result, 0);

            if (result.getExtractedText() == null && result.getPlainTextBody() != null) {
                result.setExtractedText(result.getPlainTextBody());
            }

        } catch (Exception e) {
            result.getParseWarnings().add("Failed to fully parse MIME structure: " + e.getMessage());
        }

        return result;
    }

    private void extractHeaderInfo(MimeMessage message, ParsedEmailDto dto) {
        try {
            dto.setMessageId(message.getMessageID());
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract Message-ID: " + e.getMessage());
        }

        try {
            String subject = message.getSubject();
            if (subject != null) {
                dto.setSubject(MimeUtility.decodeText(subject));
            }
        } catch (Exception e) {
            try {
                dto.setSubject(message.getHeader("Subject", null));
            } catch (MessagingException ex) {
                dto.setSubject("(Failed to decode subject)");
            }
            dto.getParseWarnings().add("Subject decoding warning: " + e.getMessage());
        }

        try {
            Address[] fromAddresses = message.getFrom();
            if (fromAddresses != null && fromAddresses.length > 0) {
                dto.setFrom(formatAddress(fromAddresses[0]));
            }
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract From address: " + e.getMessage());
        }

        try {
            dto.setTo(formatAddresses(message.getRecipients(Message.RecipientType.TO)));
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract To recipients: " + e.getMessage());
        }

        try {
            dto.setCc(formatAddresses(message.getRecipients(Message.RecipientType.CC)));
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract CC recipients: " + e.getMessage());
        }

        try {
            dto.setBcc(formatAddresses(message.getRecipients(Message.RecipientType.BCC)));
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract BCC recipients: " + e.getMessage());
        }

        try {
            dto.setReplyTo(formatAddresses(message.getReplyTo()));
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract Reply-To: " + e.getMessage());
        }

        try {
            String[] returnPath = message.getHeader("Return-Path");
            if (returnPath != null && returnPath.length > 0) {
                dto.setReturnPath(returnPath[0]);
            }
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract Return-Path: " + e.getMessage());
        }

        try {
            Date sentDate = message.getSentDate();
            if (sentDate != null) {
                dto.setSentDate(sentDate.toInstant());
            }
            String rawDate = message.getHeader("Date", null);
            dto.setRawDate(rawDate);
        } catch (MessagingException e) {
            dto.getParseWarnings().add("Failed to extract Sent Date: " + e.getMessage());
        }
    }

    private void processPart(Part part, ParsedEmailDto dto, int depth) {
        if (depth > MAX_DEPTH) {
            dto.getParseWarnings().add("Exceeded maximum MIME nesting depth limit (" + MAX_DEPTH + ")");
            return;
        }

        try {
            String disposition = part.getDisposition();
            String filename = part.getFileName();
            String contentType = part.getContentType();

            if (filename != null) {
                try {
                    filename = MimeUtility.decodeText(filename);
                } catch (Exception ignored) {
                }
            }

            boolean isAttachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                    || (filename != null && !filename.trim().isEmpty() && !Part.INLINE.equalsIgnoreCase(disposition));

            if (isAttachment) {
                handleAttachment(part, filename, contentType, false, dto);
                return;
            }

            if (Part.INLINE.equalsIgnoreCase(disposition) && filename != null) {
                handleAttachment(part, filename, contentType, true, dto);
                return;
            }

            if (part.isMimeType("text/plain")) {
                if (dto.getPlainTextBody() == null) {
                    Object content = part.getContent();
                    if (content instanceof String str) {
                        dto.setPlainTextBody(truncateText(str));
                    } else if (content instanceof InputStream is) {
                        dto.setPlainTextBody(truncateText(new String(is.readAllBytes())));
                    }
                }
            } else if (part.isMimeType("text/html")) {
                if (dto.getHtmlBody() == null) {
                    Object content = part.getContent();
                    String rawHtml = null;
                    if (content instanceof String str) {
                        rawHtml = str;
                    } else if (content instanceof InputStream is) {
                        rawHtml = new String(is.readAllBytes());
                    }

                    if (rawHtml != null) {
                        dto.setHtmlBody(truncateText(rawHtml));
                        try {
                            String text = Jsoup.parse(rawHtml).text();
                            dto.setExtractedText(truncateText(text));
                        } catch (Exception e) {
                            dto.getParseWarnings().add("Jsoup HTML text extraction warning: " + e.getMessage());
                        }
                    }
                }
            } else if (part.isMimeType("multipart/*")) {
                Object content = part.getContent();
                if (content instanceof Multipart multipart) {
                    int count = multipart.getCount();
                    for (int i = 0; i < count; i++) {
                        BodyPart bodyPart = multipart.getBodyPart(i);
                        processPart(bodyPart, dto, depth + 1);
                    }
                }
            } else if (part.isMimeType("message/rfc822")) {
                Object content = part.getContent();
                if (content instanceof Part nestedPart) {
                    processPart(nestedPart, dto, depth + 1);
                }
            } else if (filename != null) {
                handleAttachment(part, filename, contentType, false, dto);
            }

        } catch (Exception e) {
            dto.getParseWarnings().add("Warning processing MIME part: " + e.getMessage());
        }
    }

    private void handleAttachment(Part part, String filename, String contentType, boolean inline, ParsedEmailDto dto) {
        try {
            String safeFilename = (filename != null && !filename.trim().isEmpty()) ? filename : "unnamed_attachment";
            safeFilename = safeFilename.replaceAll("[\\r\\n\\t]", "").trim();

            String cleanContentType = "application/octet-stream";
            if (contentType != null) {
                int semi = contentType.indexOf(';');
                cleanContentType = (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
            }

            byte[] bytes = readPartBytes(part);
            long sizeBytes = bytes.length;
            String sha256 = calculateHash(bytes);
            boolean risky = isRiskyExtension(safeFilename);

            String contentId = null;
            String[] cidHeader = part.getHeader("Content-ID");
            if (cidHeader != null && cidHeader.length > 0) {
                contentId = cidHeader[0].replaceAll("[<>]", "").trim();
            }

            AttachmentMetaDto meta = new AttachmentMetaDto(
                    safeFilename,
                    cleanContentType,
                    sizeBytes,
                    sha256,
                    inline,
                    contentId,
                    risky
            );
            dto.getAttachments().add(meta);

            if (cleanContentType.startsWith("image/") || (inline && contentId != null)) {
                InlineImageMetaDto img = new InlineImageMetaDto(
                        safeFilename,
                        cleanContentType,
                        sizeBytes,
                        sha256,
                        contentId
                );
                dto.getInlineImages().add(img);
            }

        } catch (Exception e) {
            dto.getParseWarnings().add("Failed to process attachment (" + filename + "): " + e.getMessage());
        }
    }

    private byte[] readPartBytes(Part part) throws IOException, MessagingException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private String calculateHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private boolean isRiskyExtension(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        for (String ext : RISKY_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private String truncateText(String text) {
        if (text == null) return null;
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH) + "\n[Truncated: Maximum text preview length reached]";
    }

    private String formatAddress(Address address) {
        if (address instanceof InternetAddress internetAddress) {
            String personal = internetAddress.getPersonal();
            String email = internetAddress.getAddress();
            if (personal != null && !personal.isBlank()) {
                return personal + " <" + email + ">";
            }
            return email;
        }
        return address.toString();
    }

    private List<String> formatAddresses(Address[] addresses) {
        List<String> list = new ArrayList<>();
        if (addresses == null) return list;
        for (Address addr : addresses) {
            list.add(formatAddress(addr));
        }
        return list;
    }
}
