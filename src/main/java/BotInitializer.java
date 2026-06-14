import handler.KaczmarskiBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.logging.Logger;

public class BotInitializer {
    private static final Logger logger = Logger.getLogger(BotInitializer.class.getName());

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new KaczmarskiBot());
            logger.info("KaczmarskiBot successfully started!");
        } catch (TelegramApiException e) {
            logger.severe("Failed to start KaczmarskiBot: " + e);
        }
    }
}
