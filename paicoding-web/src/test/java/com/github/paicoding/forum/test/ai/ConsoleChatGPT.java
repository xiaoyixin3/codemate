package com.github.paicoding.forum.test.ai;

import cn.hutool.core.util.NumberUtil;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.ChatGPTStream;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.listener.ConsoleStreamListener;
import com.plexpt.chatgpt.util.Proxys;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/** Manual OpenAI smoke test. Credentials are supplied only through the environment. */
public class ConsoleChatGPT {
    public static Proxy proxy = Proxy.NO_PROXY;

    public static void main(String[] args) {
        String key = System.getenv("OPENAI_API_KEY");
        check(key);
        proxy = Proxys.http("http://127.0.0.1", 7890);
        BigDecimal balance = getBalance(key);
        if (!NumberUtil.isGreater(balance, BigDecimal.ZERO)) {
            return;
        }
        while (true) {
            String prompt = getInput("\nYou:\n");
            ChatGPTStream chatGPT = ChatGPTStream.builder().apiKey(key).proxy(proxy).build().init();
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ConsoleStreamListener listener = new ConsoleStreamListener() {
                @Override
                public void onError(Throwable throwable, String response) {
                    countDownLatch.countDown();
                }
            };
            listener.setOnComplate(msg -> countDownLatch.countDown());
            chatGPT.streamChatCompletion(Arrays.asList(Message.of(prompt)), listener);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static BigDecimal getBalance(String key) {
        return ChatGPT.builder().apiKey(key).proxy(proxy).build().init().balance();
    }

    private static void check(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY is required");
        }
    }

    @SneakyThrows
    public static String getInput(String prompt) {
        System.out.print(prompt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        List<String> lines = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read console input", e);
        }
        return lines.stream().collect(Collectors.joining("\n"));
    }
}
