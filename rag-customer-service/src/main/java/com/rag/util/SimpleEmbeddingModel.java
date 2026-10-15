package com.rag.util;

import java.util.Random;

/**
 * 简单的向量生成器 - 不依赖 LangChain4j
 * 用于测试Mock模式
 */
public class SimpleEmbeddingModel {

    private final int dimension;

    public SimpleEmbeddingModel(int dimension) {
        this.dimension = dimension;
    }

    public float[] embed(String text) {
        float[] vector = new float[dimension];

        // 使用文本哈希作为随机种子，确保相同文本生成相同向量
        long seed = text.hashCode();
        Random textRandom = new Random(seed);

        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) textRandom.nextGaussian();
        }

        // 归一化向量
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] = vector[i] / norm;
            }
        }

        return vector;
    }

    public int dimension() {
        return dimension;
    }
}