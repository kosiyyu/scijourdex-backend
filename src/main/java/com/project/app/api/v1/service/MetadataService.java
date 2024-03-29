package com.project.app.api.v1.service;

import com.project.app.api.v1.entity.Metadata;
import com.project.app.api.v1.repository.MetadataRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MetadataService {
    private final MetadataRepository metadataRepository;

    public MetadataService(MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    public Metadata saveMetadata(Metadata metadata) {
        return this.metadataRepository.save(metadata);
    }

    public Optional<Metadata> getMetadata(int metadataId) {
        return metadataRepository.findById(metadataId);
    }
}
