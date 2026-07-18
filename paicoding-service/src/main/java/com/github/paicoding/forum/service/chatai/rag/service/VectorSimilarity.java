package com.github.paicoding.forum.service.chatai.rag.service;

public final class VectorSimilarity {
    private VectorSimilarity() { }

    public static double cosine(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return -1D;
        }
        double dot = 0D, leftNorm = 0D, rightNorm = 0D;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        return leftNorm == 0D || rightNorm == 0D ? -1D : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
