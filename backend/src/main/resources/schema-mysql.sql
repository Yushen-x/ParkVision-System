CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    owner_id VARCHAR(32) NULL,
    last_login DATETIME NULL,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parking_slot (
    slot_id VARCHAR(16) PRIMARY KEY,
    layer_name VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reservation (
    id VARCHAR(32) PRIMARY KEY,
    plate_no VARCHAR(16) NOT NULL,
    phone VARCHAR(32) NULL,
    energy_type VARCHAR(32) NOT NULL,
    slot_id VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner_id VARCHAR(32) NULL,
    order_no VARCHAR(32) NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    KEY idx_reservation_owner (owner_id),
    KEY idx_reservation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parking_order (
    order_no VARCHAR(32) PRIMARY KEY,
    plate_no VARCHAR(16) NOT NULL,
    slot_id VARCHAR(16) NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
    payment_method VARCHAR(32) NULL,
    paid_at DATETIME NULL,
    duration_minutes INT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    KEY idx_parking_order_plate_status (plate_no, status),
    KEY idx_parking_order_entry_time (entry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_account (
    owner_id VARCHAR(32) PRIMARY KEY,
    owner_name VARCHAR(64) NOT NULL,
    phone_masked VARCHAR(32) NOT NULL,
    member_level VARCHAR(32) NOT NULL,
    account_status VARCHAR(32) NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vehicle_profile (
    plate_no VARCHAR(16) PRIMARY KEY,
    owner_id VARCHAR(32) NOT NULL,
    vehicle_type VARCHAR(32) NOT NULL,
    energy_type VARCHAR(32) NOT NULL,
    membership_type VARCHAR(32) NOT NULL,
    default_auth_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_vehicle_profile_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_transaction (
    payment_no VARCHAR(40) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    plate_no VARCHAR(16) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    paid_at DATETIME NOT NULL,
    KEY idx_payment_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_billing_component (
    component_no VARCHAR(48) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    component_type VARCHAR(32) NOT NULL,
    description VARCHAR(128) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL,
    KEY idx_billing_component_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alert_event (
    alert_no VARCHAR(32) PRIMARY KEY,
    alert_type VARCHAR(32) NOT NULL,
    content VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL,
    level_name VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pricing_rule (
    id VARCHAR(32) PRIMARY KEY,
    rule_name VARCHAR(64) NOT NULL,
    vehicle_type VARCHAR(16) NOT NULL,
    free_minutes INT NOT NULL,
    first_hour_fee DECIMAL(10, 2) NOT NULL,
    hourly_fee DECIMAL(10, 2) NOT NULL,
    daily_cap DECIMAL(10, 2) NOT NULL,
    peak_start_hour INT NOT NULL,
    peak_end_hour INT NOT NULL,
    peak_multiplier DECIMAL(6, 2) NOT NULL,
    status VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS access_list_item (
    plate_no VARCHAR(16) PRIMARY KEY,
    list_type VARCHAR(32) NOT NULL,
    user_type VARCHAR(64) NOT NULL,
    valid_until VARCHAR(64) NOT NULL,
    remark VARCHAR(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recognition_event (
    id VARCHAR(48) PRIMARY KEY,
    camera_id VARCHAR(32) NOT NULL,
    plate_no VARCHAR(16) NOT NULL,
    confidence DOUBLE NOT NULL,
    energy_type VARCHAR(16) NOT NULL,
    list_type VARCHAR(32) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    order_no VARCHAR(32),
    intrusion BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_node_status (
    node_name VARCHAR(64) PRIMARY KEY,
    latency VARCHAR(32) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    level_name VARCHAR(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dispatch_task (
    task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plate_no VARCHAR(16) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    tag_name VARCHAR(32) NOT NULL,
    wait_time VARCHAR(16) NOT NULL,
    vip BOOLEAN NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
    progress INT NOT NULL DEFAULT 0,
    slot_id VARCHAR(16),
    agv_id VARCHAR(16),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    role VARCHAR(32),
    method VARCHAR(8) NOT NULL,
    path VARCHAR(255) NOT NULL,
    status INT NOT NULL,
    ip VARCHAR(64),
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vision_camera (
    camera_id VARCHAR(32) PRIMARY KEY,
    profile_name VARCHAR(32) NOT NULL,
    codec VARCHAR(32) NOT NULL,
    stream_url VARCHAR(255) NOT NULL,
    fps INT NOT NULL,
    bitrate_kbps INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_plate VARCHAR(16) NULL,
    last_seen DATETIME NOT NULL,
    tamper_alarm BOOLEAN NOT NULL,
    intrusion_state BOOLEAN NOT NULL,
    detail VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    last_seen DATETIME NOT NULL,
    detail VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS charging_station (
    charger_id VARCHAR(32) PRIMARY KEY,
    protocol VARCHAR(32) NOT NULL,
    endpoint VARCHAR(64) NOT NULL,
    connector_status VARCHAR(32) NOT NULL,
    power_kw DECIMAL(6, 2) NOT NULL,
    session_kwh DECIMAL(8, 2) NOT NULL,
    vehicle_plate VARCHAR(16) NULL,
    auth_status VARCHAR(32) NOT NULL,
    last_seen DATETIME NOT NULL,
    detail VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS device_event (
    event_id VARCHAR(32) PRIMARY KEY,
    device_type VARCHAR(32) NOT NULL,
    device_id VARCHAR(32) NOT NULL,
    event_code VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    message VARCHAR(255) NOT NULL,
    event_time DATETIME NOT NULL,
    acknowledged BOOLEAN NOT NULL,
    KEY idx_device_event_device_time (device_type, device_id, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
