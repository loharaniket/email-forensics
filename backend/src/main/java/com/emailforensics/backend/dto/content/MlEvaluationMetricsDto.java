package com.emailforensics.backend.dto.content;

public class MlEvaluationMetricsDto {

    private double precision;
    private double recall;
    private double f1;
    private double accuracy;
    private int truePositives;
    private int falsePositives;
    private int trueNegatives;
    private int falseNegatives;
    private int trainingSampleCount;
    private int testSampleCount;

    public MlEvaluationMetricsDto() {
    }

    public MlEvaluationMetricsDto(double precision, double recall, double f1, double accuracy,
                                  int truePositives, int falsePositives, int trueNegatives, int falseNegatives,
                                  int trainingSampleCount, int testSampleCount) {
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.accuracy = accuracy;
        this.truePositives = truePositives;
        this.falsePositives = falsePositives;
        this.trueNegatives = trueNegatives;
        this.falseNegatives = falseNegatives;
        this.trainingSampleCount = trainingSampleCount;
        this.testSampleCount = testSampleCount;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getF1() {
        return f1;
    }

    public void setF1(double f1) {
        this.f1 = f1;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getTruePositives() {
        return truePositives;
    }

    public void setTruePositives(int truePositives) {
        this.truePositives = truePositives;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public void setFalsePositives(int falsePositives) {
        this.falsePositives = falsePositives;
    }

    public int getTrueNegatives() {
        return trueNegatives;
    }

    public void setTrueNegatives(int trueNegatives) {
        this.trueNegatives = trueNegatives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }

    public void setFalseNegatives(int falseNegatives) {
        this.falseNegatives = falseNegatives;
    }

    public int getTrainingSampleCount() {
        return trainingSampleCount;
    }

    public void setTrainingSampleCount(int trainingSampleCount) {
        this.trainingSampleCount = trainingSampleCount;
    }

    public int getTestSampleCount() {
        return testSampleCount;
    }

    public void setTestSampleCount(int testSampleCount) {
        this.testSampleCount = testSampleCount;
    }
}
