package com.expo.file.service;

import com.expo.file.entity.FileMetadata;
import java.io.InputStream;

/**
 * 내려보낼 파일 한 건. 메타데이터(이름·타입·크기)와 내용 스트림을 함께 넘긴다.
 *
 * <p>{@code content} 는 <b>받은 쪽이 닫는다.</b> 컨트롤러가 {@code InputStreamResource} 로 감싸 넘기면 Spring 이 응답을 다
 * 쓴 뒤 닫는다.
 */
public record FileDownload(FileMetadata metadata, InputStream content) {}
