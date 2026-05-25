package com.echill.event;

public record DocumentUploadedEvent(String fileUrl, String title) {
}
