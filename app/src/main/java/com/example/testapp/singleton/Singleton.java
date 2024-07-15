package com.example.testapp.singleton;

public class Singleton {
    private static Singleton instance = null;
    private String selectedCsvFile;
    private String selectedMapFile;
    private String selectedMapFilePath;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }

    public String getSelectedCsvFile() {
        return selectedCsvFile;
    }

    public void setSelectedCsvFile(String selectedCsvFile) {
        this.selectedCsvFile = selectedCsvFile;
    }

    public String getSelectedMapFile() {
        return selectedMapFile;
    }

    public void setSelectedMapFile(String selectedMapFile) {
        this.selectedMapFile = selectedMapFile;
    }

    public String getSelectedMapFilePath() {
        return selectedMapFilePath;
    }

    public void setSelectedMapFilePath(String selectedMapFilePath) {
        this.selectedMapFilePath = selectedMapFilePath;
    }
}