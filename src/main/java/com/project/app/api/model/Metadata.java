package com.project.app.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {

    public Metadata(String fullFilename, String path){
        this.fullFilename = fullFilename;
        this.path = path;
        this.lastModification = new Date();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)

    private int id;

    private String fullFilename;

    private String path;

    private Date lastModification;
}