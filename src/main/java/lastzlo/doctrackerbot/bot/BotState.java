package lastzlo.doctrackerbot.bot;

import lastzlo.doctrackerbot.model.AppUser;
import lastzlo.doctrackerbot.model.Document;
import lastzlo.doctrackerbot.service.Documents;
import lastzlo.doctrackerbot.service.UserDocumentService;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public enum BotState {
//	ADD_DOC, +
//	SYNC_DOC,
//	WAIT_FOR_COMMAND,
//	SYNC_ALL,
//	DELETE_DOC, +
//	LIST    +
    Start {
        @Override
        public BotState nextState(BotContext context) {
            sendMessage(context, "Welcome! now you can write /add for add you document, or /help for watch all commands");
            return WAIT_FOR_COMMAND;
        }

    },
    WAIT_FOR_COMMAND {
        @Override
        public BotState nextState(BotContext context) {

            String input = context.getInput();
            
            BotState nextState;
            
            switch (input) {
                case ("/help")  -> nextState = HELP.nextState(context);
                case ("/add")  -> nextState = ADD_DOCUMENT.nextState(context);
                case ("/sync") -> nextState = SYNC_DOCUMENT.nextState(context);
                case ("/delete")  -> nextState = DELETE_DOCUMENT.nextState(context);
                case ("/list")  -> nextState = LIST.nextState(context);
                default -> {
                    nextState = CANT_UNDERSTAND.nextState(context);
                }
            }
            
            return nextState;
        }
    },
    HELP {
        @Override
        public BotState nextState(BotContext context) {
            String text = """
                    Document Tracker bot can execute the following commands:\s
                    /add - Add document\s
                    /sync - Sync one\s
                    /syncAll - Sync all documents now\s
                    /list - Show list traced documents\s
                    /delete - Delete document\s
                    /help - Show all commands\s
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
                String text = "Please write case number,\n" +
                        "for example: 123/456/789";
                sendMessage(context, text);
                return ADD_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {
                UserDocumentService service = context.getBot().getUserDocumentService();

                boolean isSuccessAdded = service.addDocumentToUser(input, context.getUser());

//                DocumentService docService = context.getBot().getDocumentService();
//                boolean isSuccessAdded = docService.addDocumentToUser(
//                        input,
//                        context.getUser());

                if (isSuccessAdded) {
                    String text = "Document with case number: " + input + " was added";
                    sendMessage(context, text);

                    //sync document
                    BotContext botContext = BotContext.of(context.getBot(), context.getUser(), context.getInput());
                    return SYNC_DOCUMENT.nextState(botContext);
                } else {
                    String text = "Document with case number: " + input +
                            " has already been added, try to write another case number, \n" +
                            "or you can see all documents by running /list";
                    sendMessage(context, text);
                    return ADD_DOCUMENT;
                }

            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else {
                String text = "I can't parse it, please try one more time \n" +
                        "But if you want to do another command write /back";
                sendMessage(context, text);
                return ADD_DOCUMENT;
            }
        }
    },
    SYNC_DOCUMENT {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/sync")) {
                String text = "Please write case number,\n" +
                        "for example: 123/456/789";
                sendMessage(context, text);
                return SYNC_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {
                UserDocumentService service = context.getBot().getUserDocumentService();

                Document document = service.getDocumentByUserAndDocumnetCaseNumber(context.getUser(), input);
                if (document == null) {
                    String text = "You don't added document with case number: " + input +
                            " try to write another case number,\n" +
                            "or you can see all documents by /list,\n" +
                            "or add new document by /add";
                    sendMessage(context, text);
                    return SYNC_DOCUMENT;
                } else {
                    String text = "Please wait, now I synchronise information about document with case number: " + input;
                    sendMessage(context, text);

                    service.synchronizeDocument(document, context);
                    return WAIT_FOR_COMMAND;
                }
            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else if (input.equals("/add")) {
                return ADD_DOCUMENT.nextState(context);
            } else {
                String text = "I can't parse it, please try one more time \n" +
                        "But if you want to do another command write /back";
                sendMessage(context, text);
                return SYNC_DOCUMENT;
            }
        }
    },
    BACK {
        @Override
        public BotState nextState(BotContext context) {
            String text = "OK, now you can run any command from /help";
            sendMessage(context, text);
            return BotState.WAIT_FOR_COMMAND;
        }
    },
    LIST {
        @Override
        public BotState nextState(BotContext context) {
            AppUser user = context.getUser();
            UserDocumentService service = context.getBot().getUserDocumentService();
            String documents = service.getDocumentsByUserInOneString(user);

            String text = "List of documents:\n" + documents;
            sendMessage(context, text);
            return BotState.WAIT_FOR_COMMAND;
        }
    },
    DELETE_DOCUMENT {
        @Override
        public BotState nextState(BotContext context) {
            String input = context.getInput();

            if (input.equals("/delete")) {
                String text = "Please write case number,\n" +
                        "for example: 123/456/789";
                sendMessage(context, text);
                return DELETE_DOCUMENT;
            } else if (Documents.isCaseNumber(input)) {

                UserDocumentService service = context.getBot().getUserDocumentService();
                boolean isSuccessDeleted = service.deleteDocumentFromUser(input, context.getUser());

                if (isSuccessDeleted) {
                    String text = "Document with case number: " + input + " was deleted";
                    sendMessage(context, text);
                    return WAIT_FOR_COMMAND;
                } else  {
                    String text = "Document with case number: " + input +
                            " not found in documents, " +
                            "try to write another case number, \n" +
                            "or you can see all documents by running /list";
                    sendMessage(context, text);
                    return DELETE_DOCUMENT;
                }
            } else if (input.equals("/back")) {
                return BACK.nextState(context);
            } else if (input.equals("/list")) {
                return LIST.nextState(context);
            } else {
                String text = "I can't parse it, please try one more time \n" +
                        "But if you want to do another command write /back";
                sendMessage(context, text);
                return DELETE_DOCUMENT;
            }
        }
    },
    CANT_UNDERSTAND {
        @Override
        public BotState nextState(BotContext context) {
            String text = "I can't understand what you need,\n" +
                    "please write /help for show all commands";
            sendMessage(context, text);

            return WAIT_FOR_COMMAND;
        }
    };


    public static BotState getInitialState() {
        return Start;
    }

    protected void sendMessage(BotContext context, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(context.getUser().getChatId()))
                .text(text)
                .build();
        try {
            context.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public abstract BotState nextState(BotContext context);
}
