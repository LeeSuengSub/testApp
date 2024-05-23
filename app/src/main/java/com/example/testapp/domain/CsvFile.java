package com.example.testapp.domain;

import java.io.Serializable;

public class CsvFile implements Serializable {
    private String fileName;

    public CsvFile(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

}
