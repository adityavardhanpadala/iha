package org.example;

public class ArgumentAnalysisResult {
    public String unit;
    public String argumentName;
    public int readCount = 0;
    public int writeCount = 0;

    @Override
    public String toString() {
        return unit + " - " + argumentName + " - Reads: " + readCount + ", Writes: " + writeCount;
    }
}
