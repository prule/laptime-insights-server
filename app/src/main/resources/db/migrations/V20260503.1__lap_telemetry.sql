create table Lap_Telemetry
(
    ID identity,
    LAP_ID            bigint  not null,
    LAP_UID           varchar(255) not null,
    SPLINE_POSITION   double  not null,
    SPEED_KPH         double  not null,
    GEAR              integer not null,
    THROTTLE          double  not null,
    BRAKE             double  not null
);

create index IDX_LAP_TELEMETRY_LAP_ID on Lap_Telemetry (LAP_ID);
create index IDX_LAP_TELEMETRY_LAP_UID on Lap_Telemetry (LAP_UID);
