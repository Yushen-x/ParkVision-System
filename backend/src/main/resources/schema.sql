CREATE TABLE IF NOT EXISTS parking_slot (
    slot_id VARCHAR(16) PRIMARY KEY,
    layer_name VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS parking_order (
    order_no VARCHAR(32) PRIMARY KEY,
    plate_no VARCHAR(16) NOT NULL,
    slot_id VARCHAR(16) NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS alert_event (
    alert_no VARCHAR(32) PRIMARY KEY,
    alert_type VARCHAR(32) NOT NULL,
    content VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL,
    level_name VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS pricing_rule (
    rule_name VARCHAR(64) PRIMARY KEY,
    time_range VARCHAR(64) NOT NULL,
    method VARCHAR(128) NOT NULL,
    extra_policy VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS access_list_item (
    plate_no VARCHAR(16) PRIMARY KEY,
    list_type VARCHAR(32) NOT NULL,
    user_type VARCHAR(64) NOT NULL,
    valid_until VARCHAR(64) NOT NULL,
    remark VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS system_node_status (
    node_name VARCHAR(64) PRIMARY KEY,
    latency VARCHAR(32) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    level_name VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS agv_unit (
    agv_id VARCHAR(16) PRIMARY KEY,
    x_pos INT NOT NULL,
    y_pos INT NOT NULL,
    loaded BOOLEAN NOT NULL,
    task VARCHAR(255) NOT NULL,
    battery_pct INT NOT NULL,
    mode_name VARCHAR(32) NOT NULL,
    velocity_mps DECIMAL(5, 2) NOT NULL,
    last_command VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS dispatch_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_no VARCHAR(16) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    tag_name VARCHAR(32) NOT NULL,
    wait_time VARCHAR(16) NOT NULL,
    vip BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS vision_camera (
    camera_id VARCHAR(32) PRIMARY KEY,
    profile_name VARCHAR(32) NOT NULL,
    codec VARCHAR(32) NOT NULL,
    stream_url VARCHAR(255) NOT NULL,
    fps INT NOT NULL,
    bitrate_kbps INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_plate VARCHAR(16),
    last_seen TIMESTAMP NOT NULL,
    tamper_alarm BOOLEAN NOT NULL,
    intrusion_state BOOLEAN NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS gate_device (
    gate_id VARCHAR(32) PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL,
    endpoint VARCHAR(64) NOT NULL,
    coil_address VARCHAR(32) NOT NULL,
    queue_depth INT NOT NULL,
    gate_state VARCHAR(32) NOT NULL,
    loop_occupied BOOLEAN NOT NULL,
    estop_armed BOOLEAN NOT NULL,
    last_decision VARCHAR(64) NOT NULL,
    last_seen TIMESTAMP NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS charging_station (
    charger_id VARCHAR(32) PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL,
    endpoint VARCHAR(64) NOT NULL,
    connector_status VARCHAR(32) NOT NULL,
    power_kw DECIMAL(6, 2) NOT NULL,
    session_kwh DECIMAL(8, 2) NOT NULL,
    vehicle_plate VARCHAR(16),
    auth_status VARCHAR(32) NOT NULL,
    last_seen TIMESTAMP NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS device_event (
    event_id VARCHAR(32) PRIMARY KEY,
    device_type VARCHAR(32) NOT NULL,
    device_id VARCHAR(32) NOT NULL,
    event_code VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    message VARCHAR(255) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    acknowledged BOOLEAN NOT NULL
);
