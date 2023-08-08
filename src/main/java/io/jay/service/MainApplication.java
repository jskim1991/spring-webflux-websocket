package io.jay.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

@Configuration
class FluxConfiguration {
    @Bean
    public Sinks.Many<Long> sink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}

@Configuration
class WebSocketConfiguration {

    private final WebSocketHandler handler;

    public WebSocketConfiguration(WebSocketHandler handler) {
        this.handler = handler;
    }

    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    HandlerMapping handlerMapping() {
        return new SimpleUrlHandlerMapping(Map.of("/ws/numbers", handler), -1);
    }
}

@Component
class MyWebSocketHandler implements WebSocketHandler {
    private final Sinks.Many<Long> history;

    public MyWebSocketHandler(Sinks.Many<Long> history) {
        this.history = history;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        session.getAttributes().entrySet().forEach(System.out::println);

        var data = session.receive()
                .map(msg -> {
                    var payload = msg.getPayloadAsText();
                    var n = Long.parseLong(payload);
                    return session.textMessage(String.valueOf(n * n));
                });
//        var data = history.asFlux()
//                .map(n -> {
//                    var text = session.textMessage(String.valueOf(n * n));
//                    return text;
//                });
        return session.send(data);
    }
}

@Controller
@ResponseBody
class MyController {

    private final Sinks.Many<Long> history;

    public MyController(Sinks.Many<Long> history) {
        this.history = history;
    }

    @GetMapping("/hello/{number}")
    public Long hello(@PathVariable Long number) {
        history.tryEmitNext(number);
        return number;
    }

    @PostMapping("/webhook")
    public void receive(String json) {
        history.tryEmitNext(11234L);
    }
}
