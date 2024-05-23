package com.example.testapp.domain;

import java.io.Serializable;

public class MapFile implements Serializable {
    private String mapName;

    public MapFile(String fileName) {
        this.mapName = fileName;
    }
    public String getMapFile() {
        return mapName;
    }

}
