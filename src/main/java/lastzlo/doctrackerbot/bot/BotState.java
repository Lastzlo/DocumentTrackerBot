package lastzlo.doctrackerbot.bot;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.service.Documents;
import lastzlo.doctrackerbot.service.UserDocumentService;

import java.util.List;

public enum BotState {
    START {
        @Override
        public BotState nextState(BotContext context) {
            sendMDMessage(context, """
                    Welcome! You can add tracked document by write /add, 
                    or /help for watch all commands""");
            return WAIT_FOR_COMMAND;
        }
    },
    WAIT_FOR_COMMAND {
        @Override
        public BotState nextState(BotContext context) {

            String input = context.getInput();
            BotState nextState;

            switch (input) {
                case ("/start")  -> nextState = HELP.nextState(context);
                case ("/add")  -> nextState = ADD_DOCUMENT.nextState(context);
                case ("/sync") -> nextState = SYNC_DOCUMENT.nextState(context);
                case ("/sync_all") -> nextState = SYNC_ALL.nextState(context);
                case ("/list")  -> nextState = LIST.nextState(context);
                case ("/delete")  -> nextState = DELETE_DOCUMENT.nextState(context);
                case ("/help")  -> nextState = HELP.nextState(context);
                case ("/stop")  -> nextState = STOP.nextState(context);
                default -> nextState = CANT_UNDERSTAND.nextState(context);
            }

            return nextState;
        }
    },
    HELP {
        @Override
        public BotState nextState(BotContext context) {
            String text = """
                Bot can execute the following commands:
                /start - Start working bot
                /add - Add tracked document
                /sync - Synchronize one
                /sync_all - Synchronize all
                /list - Show list traced documents
                /delete - Delete tracked document
                /help - Show all commands
                /stop - Stop working bot""";
            sendMessage(context, text);

            return WAIT_FOR_COMMAND;
        }
    },
    ADD_DOCUMENT {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/add")) {
                String text = """
                    Please write case number,
                    for example: *123/456/789*""";
                sendMDMessage(context, text);
                return ADD_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {
                UserDocumentService service = context.getBot().getUserDocumentService();
                String caseNumber = input;
                boolean isSuccessAdded = service.addDocumentToUser(caseNumber, context.getUser());

                if (isSuccessAdded) {
                    String text = """
                        Document with case number: *%s* was added""".formatted(caseNumber);
                    sendMDMessage(context, text);

                    // sync document, caseNumber value was placed in bot context
                    // to make next command /sync work with case number
                    BotContext botContext = BotContext.of(context.getBot(), context.getUser(), caseNumber);
                    return SYNC_DOCUMENT.nextState(botContext);
                } else {
                    String text = """
                        Document with case number: *%s*
                        has already been added, try to write another case number,
                        or you can see your documents by running /list""".formatted(caseNumber);
                    sendMDMessage(context, text);
                    return ADD_DOCUMENT;
                }

            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else {
                String text = """
                        I can't parse it, please try one more time
                        But if you want to do another command write /back""";
                sendMDMessage(context, text);
                return ADD_DOCUMENT;
            }
        }
    },
    SYNC_DOCUMENT {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/sync")) {
                String text = """
                        Please write case number,
                        for example: *123/456/789*""";
                sendMDMessage(context, text);
                return SYNC_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {
                UserDocumentService service = context.getBot().getUserDocumentService();
                String caseNumber = input;

                Document document = service.getDocumentByUserAndDocumentCaseNumber(context.getUser(), caseNumber);
                if (document == null) {
                    String text = """
                        You don't added document with case number: *%s*
                        try to write another case number,
                        or you can see all documents by /list,
                        or add new document by /add""".formatted(caseNumber);
                    sendMDMessage(context, text);
                    return SYNC_DOCUMENT;
                } else {
                    String text = """
                        Please wait, now I synchronising information
                        about document with case number: *%s*""".formatted(caseNumber);
                    sendMDMessage(context, text);

                    service.synchronizeUserDocument(document, context);
                    return WAIT_FOR_COMMAND;
                }
            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else if (input.equals("/add")) {
                return ADD_DOCUMENT.nextState(context);
            } else {
                String text = """
                    I can't parse it, please try one more time
                    But if you want to do another command write /back""";
                sendMDMessage(context, text);
                return SYNC_DOCUMENT;
            }
        }
    },
    SYNC_ALL {
        @Override
        public BotState nextState(BotContext context) {

            UserDocumentService service = context.getBot()
                    .getUserDocumentService();

            List<Document> documents = service.getDocumentsByUser(context.getUser());

            if (documents.size() == 0) {
                String text = """
                        You haven't documents for synchronising
                        Write /add for add tracked document""";
                sendMDMessage(context, text);
            } else {
                String text = """
                        Please wait, now I synchronising information
                        about all yours documents""";
                sendMDMessage(context, text);

                documents.forEach(doc -> {
                    service.synchronizeUserDocument(doc, context);
                });
            }

            return WAIT_FOR_COMMAND;
        }
    },
    BACK {
        @Override
        public BotState nextState(BotContext context) {
            String text = "OK, now you can run any command from /help";
            sendMDMessage(context, text);
            return BotState.WAIT_FOR_COMMAND;
        }
    },
    LIST {
        @Override
        public BotState nextState(BotContext context) {
            AppUser user = context.getUser();
            UserDocumentService service = context.getBot().getUserDocumentService();
            String text = service.getMessageOfListUserDocuments(user);
            sendMDMessage(context, text);
            return BotState.WAIT_FOR_COMMAND;
        }
    },
    DELETE_DOCUMENT {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/delete")) {
                String text = """
                        Please write case number,
                        for example: *123/456/789*""";
                sendMDMessage(context, text);
                return DELETE_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {

                UserDocumentService service = context.getBot().getUserDocumentService();
                boolean isSuccessDeleted = service.deleteDocumentFromUser(input, context.getUser());

                if (isSuccessDeleted) {
                    String text = """
                            Document with case number: *%s* was deleted""".formatted(input);
                    sendMDMessage(context, text);
                    return WAIT_FOR_COMMAND;
                } else  {
                    String text = """
                            Document with case number: *%s*
                            not found in documents,
                            try to write another case number,
                            or you can see all documents by running /list""".formatted(input);
                    sendMDMessage(context, text);
                    return DELETE_DOCUMENT;
                }
            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else {
                String text = """
                        I can't parse it, please try one more time
                        But if you want to do another command write /back""";
                sendMDMessage(context, text);
                return DELETE_DOCUMENT;
            }
        }
    },
    STOP {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/stop")) {
                String text = """
                        You definitely want to stop the bot?
                        Your documents will no longer be tracked
                        Write /yes to stop the bot,
                        or /no to keep the bot running""";
                sendMDMessage(context, text);
                return STOP;
            } else if (input.equals("/yes")) {
                //do delete
                context.getBot().getUserDocumentService().deleteAllDocumentsFromUser(context.getUser());
                String text = "See you soon!";
                sendMDMessage(context, text);
                return STOPPED;
            } else {
                return BACK.nextState(context);
            }
        }
    },
    STOPPED {
        @Override
        public BotState nextState(BotContext context) {
            return START.nextState(context);
        }
    },
    CANT_UNDERSTAND {
        @Override
        public BotState nextState(BotContext context) {
            String text = """
                I can't understand what you need,
                please write /help for show all commands""";
            sendMDMessage(context, text);

            return WAIT_FOR_COMMAND;
        }
    };


    public static BotState getInitialState() {
        return START;
    }

    protected void sendMDMessage(BotContext context, String text) {
        context.getBot().sendMDMessage(
                String.valueOf(context.getUser().getChatId()),
                text
        );
    }

    protected void sendMessage(BotContext context, String text) {
        context.getBot().sendMessage(
                String.valueOf(context.getUser().getChatId()),
                text
        );
    }

    public abstract BotState nextState(BotContext context);
}
