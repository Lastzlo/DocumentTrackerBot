CREATE TABLE app_user
(
    appuser_id BIGINT NOT NULL,
    chat_id    BIGINT,
    state      INT,
    CONSTRAINT pk_appuser PRIMARY KEY (appuser_id)
);

ALTER TABLE app_user
    ADD CONSTRAINT uc_appuser_chatid UNIQUE (chat_id);

CREATE TABLE document
(
    document_id                        BIGINT NOT NULL,
    case_number                        VARCHAR(255),
    count_of_documents_found_last_time INT,
    count_of_syncs_by_case_number      INT,
    CONSTRAINT pk_document PRIMARY KEY (document_id)
);

ALTER TABLE document
    ADD CONSTRAINT uc_document_casenumber UNIQUE (case_number);

CREATE TABLE user_document
(
    id                   BIGINT NOT NULL,
    app_user_appuser_id  BIGINT,
    document_document_id BIGINT,
    CONSTRAINT pk_userdocument PRIMARY KEY (id)
);

ALTER TABLE user_document
    ADD CONSTRAINT FK_USERDOCUMENT_ON_APPUSER_APPUSER FOREIGN KEY (app_user_appuser_id) REFERENCES app_user (appuser_id);

ALTER TABLE user_document
    ADD CONSTRAINT FK_USERDOCUMENT_ON_DOCUMENT_DOCUMENT FOREIGN KEY (document_document_id) REFERENCES document (document_id);
