package com.forexbot.scheduler;

import com.forexbot.service.BotEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TradingScheduler.class);

    private final BotEngineService botEngineService;

    public TradingScheduler(BotEngineService botEngineService) {
        this.botEngineService = botEngineService;
    }

    @Scheduled(fixedDelayString = "${bot.scheduler.interval:60000}")
    public void scheduledTradingCycle() {
        log.debug("Scheduler: triggering trading cycle");
        botEngineService.executeTradingCycle();
    }
}
