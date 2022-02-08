package lastzlo.doctrackerbot.bot;


import lastzlo.doctrackerbot.model.AppUser;

public class BotContext {
    private final DocumentTrackerBot bot;
    private final AppUser user;
    private final String input;

    public static BotContext of(DocumentTrackerBot bot, AppUser user, String text) {
        return new BotContext(bot, user, text);
    }

    private BotContext(DocumentTrackerBot bot, AppUser user, String input) {
        this.bot = bot;
        this.user = user;
        this.input = input;
    }

    public DocumentTrackerBot getBot() {
        return bot;
    }

    public AppUser getUser() {
        return user;
    }

    public String getInput() {
        return input;
    }
}
