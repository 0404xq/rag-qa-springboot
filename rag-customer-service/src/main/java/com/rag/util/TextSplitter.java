package com.rag.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextSplitter {

    public List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 防止内存溢出：如果文本太长，只取前 2000 个字符
        if (text.length() > 2000) {
            text = text.substring(0, 2000);
        }

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            if (end < length) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastExclamation = text.lastIndexOf('!', end);
                int lastNewline = text.lastIndexOf('\n', end);

                int bestCut = Math.max(Math.max(lastPeriod, lastQuestion),
                        Math.max(lastExclamation, lastNewline));

                if (bestCut > start) {
                    end = bestCut + 1;
                }
            }

            chunks.add(text.substring(start, Math.min(end, length)));
            start = Math.min(end, length) - overlap;

            if (start < 0) {
                start = 0;
            }

            // 防止无限循环
            if (start >= length) {
                break;
            }
        }

        return chunks;
    }
}