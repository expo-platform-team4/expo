package com.expo.file.repository;

import com.expo.file.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code file_metadata} 조회. */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {}
