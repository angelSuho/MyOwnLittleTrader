package com.trader.coin.common.infrastructure.alert.discord;

import com.trader.coin.common.infrastructure.config.WebhookProperties;
import com.trader.coin.common.infrastructure.config.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordService {
    private final WebhookProperties webhookProperties;

    public void sendDiscordErrorAlertLog(BaseException ex, HttpServletRequest request) {
        try {
            DiscordUtil discordBot = getDiscordBot(webhookProperties.getDiscordAlert().getUrl());
            String registeredTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            DiscordUtil.EmbedObject embedObject = new DiscordUtil.EmbedObject()
                    .setTitle("\uD83D\uDEA8")
                    .setColor(Color.RED)
//                    .setFooter("여기는 footer 입니다 ", "https://i.imgur.com/Hv0xNBm.jpeg") //  푸터
//                    .setThumbnail("https://i.imgur.com/oBPXx0D.png") //  썸네일 이미지
//                    .setImage("https://i.imgur.com/8nLFCVP.png") //  메인 이미지
                    .addField("Platform", "upbit", false)
                    .addField("Request IP", request.getRemoteAddr(), true)
                    .addField("Request URL", request.getRequestURL() + "   " + request.getMethod(), true)
                    .addField("Error Code", ex.getErrorCode().getStatus().toString(), false)
                    .addField("Error Message", ex.getMessage(), true)
                    .addField("timestamp", registeredTimeFormat, false);

            discordBot.addEmbed(embedObject);
            discordBot.execute();
        } catch (Exception e) {
            log.error("Discord 통신 과정에 예외 발생");
        }
    }

    public void sendDiscordAlertLog(String marketId, String price, String volume, String side) {
        try {
             DiscordUtil discordBot = getDiscordBot(webhookProperties.getUpbit().getUrl());
            String registeredTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());

            discordBot.setUsername("업비트 알리미");
            discordBot.setAvatarUrl("https://raw.githubusercontent.com/angelSuho/MyOwnLittleTrader/main/images/upbit.png");

            DiscordUtil.EmbedObject embedObject;
            if (volume != null) {
                embedObject = new DiscordUtil.EmbedObject()
                        .setTitle(getSide(side) + " " + marketId)
                        .setColor(getColor(side))
                        .addField("수량", volume, true);
            } else {
                embedObject = new DiscordUtil.EmbedObject()
                        .setTitle(getSide(side) + " " + marketId)
                        .setColor(getColor(side))
                        .addField("가격",String.format("%,d", Long.parseLong(price)), true);
            }

            discordBot.addEmbed(embedObject);
            discordBot.execute();
        } catch (Exception e) {
            log.error("Discord 통신 과정에 예외 발생");
        }
    }

    private DiscordUtil getDiscordBot(String url) {
        return new DiscordUtil(url);
    }

    private String getSide(String side) {
        return side.equals("ask") ? "매도" : "매수";
    }

    private Color getColor(String side) {
        return side.equals("ask") ? Color.BLUE : Color.GREEN;
    }
}
