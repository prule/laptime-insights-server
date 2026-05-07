create table Realtime_Car_Update
(
    ID                      identity,
    SESSION_ID              bigint       not null,
    SESSION_UID             varchar(255) not null,
    LAP_ID                  bigint,
    LAP_UID                 varchar(255),
    RECORDED_AT             timestamp    not null,
    CAR_INDEX               integer      not null,
    DRIVER_INDEX            integer      not null,
    DRIVER_COUNT            integer      not null,
    GEAR                    integer      not null,
    WORLD_POS_X             real         not null,
    WORLD_POS_Y             real         not null,
    YAW                     real         not null,
    CAR_LOCATION            varchar(32)  not null,
    KMH                     integer      not null,
    RACE_POSITION           integer      not null,
    CUP_POSITION            integer      not null,
    TRACK_POSITION          integer      not null,
    SPLINE_POSITION         double       not null,
    LAPS                    integer      not null,
    DELTA                   integer      not null,
    BEST_LAP_TIME_MS        bigint       not null,
    LAST_LAP_TIME_MS        bigint       not null,
    CURRENT_LAP_TIME_MS     bigint       not null,
    CURRENT_LAP_IS_INVALID  boolean      not null,
    CURRENT_LAP_IS_OUTLAP   boolean      not null,
    CURRENT_LAP_IS_INLAP    boolean      not null
);

create index IDX_REALTIME_CAR_UPDATE_SESSION_ID  on Realtime_Car_Update (SESSION_ID);
create index IDX_REALTIME_CAR_UPDATE_SESSION_UID on Realtime_Car_Update (SESSION_UID);
create index IDX_REALTIME_CAR_UPDATE_LAP_ID      on Realtime_Car_Update (LAP_ID);
create index IDX_REALTIME_CAR_UPDATE_LAP_UID     on Realtime_Car_Update (LAP_UID);
create index IDX_REALTIME_CAR_UPDATE_CAR_INDEX   on Realtime_Car_Update (CAR_INDEX);
