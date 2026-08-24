package com.expo.file.entity;

/** 올린 내용에 대해 기록해 둘 것들. 파라미터를 늘어놓지 않으려고 묶었다. */
public record UploadedContent(
        String originalFilename,
        String contentType,
        long size,
        String checksum,
        FileAccessLevel accessLevel) {}
