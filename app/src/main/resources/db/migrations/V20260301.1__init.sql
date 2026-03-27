create table Session
(
    ID identity,
    UID          varchar(255),
    SIMULATOR    varchar(255),
    TRACK        varchar(255),
    CAR          varchar(255),
    SESSION_TYPE varchar(255),
    STARTED_AT   timestamp,
    FINISHED_AT     timestamp
)

