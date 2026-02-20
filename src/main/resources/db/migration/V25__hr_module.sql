-- =============================================
-- HR Module: Departments, Positions, Employees, Salary
-- =============================================

-- Departments
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id BIGINT REFERENCES departments(id),
    manager_id BIGINT,  -- references employees(id), added as FK after employees table
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, code)
);

CREATE INDEX idx_departments_tenant ON departments(tenant_id);
CREATE INDEX idx_departments_parent ON departments(parent_id);

-- Positions (Job titles)
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    department_id BIGINT REFERENCES departments(id),
    base_salary DECIMAL(15,2) DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, code)
);

CREATE INDEX idx_positions_tenant ON positions(tenant_id);
CREATE INDEX idx_positions_department ON positions(department_id);

-- Employees
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    employee_code VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(10),
    address TEXT,
    hire_date DATE NOT NULL,
    termination_date DATE,
    department_id BIGINT REFERENCES departments(id),
    position_id BIGINT REFERENCES positions(id),
    user_id BIGINT REFERENCES users(id),
    current_salary DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, ON_LEAVE, TERMINATED
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, employee_code)
);

CREATE INDEX idx_employees_tenant ON employees(tenant_id);
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_position ON employees(position_id);
CREATE INDEX idx_employees_user ON employees(user_id);
CREATE INDEX idx_employees_status ON employees(tenant_id, status);

-- Add manager FK in departments now that employees exists
ALTER TABLE departments ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_id) REFERENCES employees(id);

-- Salary records (history of salary payments)
CREATE TABLE salary_records (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    base_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    bonus_amount DECIMAL(15,2) DEFAULT 0,
    deduction_amount DECIMAL(15,2) DEFAULT 0,
    net_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PAID, CANCELLED
    paid_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    updated_by BIGINT,
    version BIGINT DEFAULT 0,
    UNIQUE(tenant_id, employee_id, period_year, period_month)
);

CREATE INDEX idx_salary_records_tenant ON salary_records(tenant_id);
CREATE INDEX idx_salary_records_employee ON salary_records(employee_id);
CREATE INDEX idx_salary_records_period ON salary_records(tenant_id, period_year, period_month);

-- HR Permissions
INSERT INTO permissions (code, name, description, module, created_at) VALUES
    ('HR_DEPARTMENT_READ', 'View Departments', 'Can view departments', 'HR', NOW()),
    ('HR_DEPARTMENT_WRITE', 'Manage Departments', 'Can create and edit departments', 'HR', NOW()),
    ('HR_POSITION_READ', 'View Positions', 'Can view positions', 'HR', NOW()),
    ('HR_POSITION_WRITE', 'Manage Positions', 'Can create and edit positions', 'HR', NOW()),
    ('HR_EMPLOYEE_READ', 'View Employees', 'Can view employees', 'HR', NOW()),
    ('HR_EMPLOYEE_WRITE', 'Manage Employees', 'Can create and edit employees', 'HR', NOW()),
    ('HR_SALARY_READ', 'View Salary', 'Can view salary records', 'HR', NOW()),
    ('HR_SALARY_WRITE', 'Manage Salary', 'Can create and manage salary payments', 'HR', NOW())
ON CONFLICT (code) DO NOTHING;

-- Give SUPER_ADMIN and ADMIN all HR permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN')
  AND p.module = 'HR'
ON CONFLICT DO NOTHING;
