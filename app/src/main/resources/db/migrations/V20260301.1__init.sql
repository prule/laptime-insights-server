create table Session
(
    ID identity,
    UID          varchar(255) not null,
    SIMULATOR    varchar(255) not null,
    SESSION_TYPE varchar(255) not null,
    TRACK        varchar(255),
    CAR          varchar(255),
    STARTED_AT   timestamp,
    FINISHED_AT  timestamp
);

create table Lap
(
    ID identity,
    UID           varchar(255),
    SESSION_ID    bigint,
    SESSION_UID   varchar(255),
    RECORDED_AT   timestamp,
    LAP_TIME      bigint,
    LAP_NUMBER    integer,
    VALID         boolean,
    PERSONAL_BEST boolean
);